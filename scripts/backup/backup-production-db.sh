#!/usr/bin/env bash
# Creates an encrypted, custom-format PostgreSQL backup without uploading it.

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

backup_require_command age
backup_require_command pg_dump
backup_require_command pg_restore
backup_require_command psql
backup_require_command python3

for required in \
  TRADEPILOT_BACKUP_PGHOST \
  TRADEPILOT_BACKUP_PGPORT \
  TRADEPILOT_BACKUP_PGDATABASE \
  TRADEPILOT_BACKUP_PGUSER \
  TRADEPILOT_BACKUP_PGPASSWORD \
  TRADEPILOT_BACKUP_AGE_RECIPIENT; do
  backup_require_var "$required"
done

sslmode="${TRADEPILOT_BACKUP_PGSSLMODE:-verify-full}"
synthetic_mode="${TRADEPILOT_BACKUP_SYNTHETIC_TEST_MODE:-false}"
connect_timeout="${TRADEPILOT_BACKUP_PGCONNECT_TIMEOUT:-15}"
[[ "$connect_timeout" =~ ^[1-9][0-9]*$ ]] || backup_die "TRADEPILOT_BACKUP_PGCONNECT_TIMEOUT must be a positive integer"
if [[ "$sslmode" != "verify-full" ]]; then
  [[ "$synthetic_mode" == "true" && "$sslmode" == "disable" ]] || backup_die "TRADEPILOT_BACKUP_PGSSLMODE must be verify-full"
  case "$TRADEPILOT_BACKUP_PGHOST" in
    localhost|127.0.0.1|::1) ;;
    *) backup_die "synthetic TLS bypass is limited to a loopback PostgreSQL host" ;;
  esac
fi

output_dir="${TRADEPILOT_BACKUP_OUTPUT_DIR:-$PWD/.backup-output}"
mkdir -p "$output_dir"
chmod 700 "$output_dir"
[[ -d "$output_dir" ]] || backup_die "backup output path is not a directory"

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/tradepilot-backup.XXXXXX")"
backup_completed=false
snapshot_holder_pid=""
snapshot_holder_input_open=false
cleanup() {
  unset PGPASSWORD TRADEPILOT_BACKUP_PGPASSWORD
  if [[ "$snapshot_holder_input_open" == "true" ]]; then
    exec 3>&-
  fi
  if [[ -n "$snapshot_holder_pid" ]] && kill -0 "$snapshot_holder_pid" 2>/dev/null; then
    kill "$snapshot_holder_pid" 2>/dev/null || true
    wait "$snapshot_holder_pid" 2>/dev/null || true
  fi
  # The output names are unique to this invocation. Do not leave a partial
  # encrypted artifact or metadata behind if dump, encryption, or metadata
  # generation is interrupted before the backup becomes usable.
  if [[ "$backup_completed" != "true" ]]; then
    rm -f "${encrypted_path:-}" "${metadata_path:-}" "${completeness_manifest_encrypted_path:-}"
  fi
  rm -rf "$work_dir"
}
trap cleanup EXIT HUP INT TERM

export PGHOST="$TRADEPILOT_BACKUP_PGHOST"
export PGPORT="$TRADEPILOT_BACKUP_PGPORT"
export PGDATABASE="$TRADEPILOT_BACKUP_PGDATABASE"
export PGUSER="$TRADEPILOT_BACKUP_PGUSER"
export PGPASSWORD="$TRADEPILOT_BACKUP_PGPASSWORD"
export PGSSLMODE="$sslmode"
export PGCONNECT_TIMEOUT="$connect_timeout"

if [[ "$sslmode" == "verify-full" && -n "${TRADEPILOT_BACKUP_PGSSLROOTCERT_BASE64:-}" ]]; then
  export PGSSLROOTCERT="$work_dir/root.crt"
  backup_decode_base64 "$TRADEPILOT_BACKUP_PGSSLROOTCERT_BASE64" "$PGSSLROOTCERT"
  chmod 600 "$PGSSLROOTCERT"
elif [[ "$sslmode" == "verify-full" && -n "${TRADEPILOT_BACKUP_PGSSLROOTCERT_PATH:-}" ]]; then
  export PGSSLROOTCERT="$TRADEPILOT_BACKUP_PGSSLROOTCERT_PATH"
elif [[ "$sslmode" == "verify-full" ]]; then
  backup_die "a CA root certificate is required for verify-full TLS"
fi

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
archive_name="tradepilot-postgres-${timestamp}.dump"
encrypted_name="${archive_name}.age"
archive_path="$work_dir/$archive_name"
encrypted_path="$output_dir/$encrypted_name"
metadata_path="$output_dir/${encrypted_name}.metadata.json"
completeness_manifest_name="${encrypted_name}.completeness.json.age"
completeness_manifest_encrypted_path="$output_dir/$completeness_manifest_name"

printf 'Starting encrypted PostgreSQL backup at %s UTC.\n' "$timestamp"
server_version="$(psql -X -A -t -v ON_ERROR_STOP=1 -c 'show server_version_num' | tr -d '[:space:]')"
[[ "$server_version" =~ ^[0-9]+$ ]] || backup_die "could not read PostgreSQL server version"

# The holder transaction stays open while pg_dump and each public-table count
# import its exported snapshot. This makes the encrypted archive and manifest
# a single, concurrency-consistent recovery record.
# Bash 3.2 on the macOS recovery workstation has no coprocess support. Named
# pipes keep one interactive psql transaction open without relying on coproc.
snapshot_holder_input="$work_dir/snapshot-holder.in"
snapshot_holder_output="$work_dir/snapshot-holder.out"
mkfifo "$snapshot_holder_input" "$snapshot_holder_output"
psql -X -A -t -q -v ON_ERROR_STOP=1 <"$snapshot_holder_input" >"$snapshot_holder_output" &
snapshot_holder_pid="$!"
exec 3>"$snapshot_holder_input"
snapshot_holder_input_open=true
printf 'begin transaction isolation level repeatable read, read only;\nselect pg_export_snapshot();\n' >&3
IFS= read -r snapshot_id <"$snapshot_holder_output" || backup_die "could not create a PostgreSQL exported snapshot"
[[ "$snapshot_id" =~ ^[0-9A-Za-z_-]+$ ]] || backup_die "PostgreSQL returned an invalid exported snapshot identifier"

snapshot_query() {
  local query="$1"
  psql -X -A -t -q -v ON_ERROR_STOP=1 -c "begin transaction isolation level repeatable read, read only; set transaction snapshot '${snapshot_id}'; ${query}; commit;"
}

table_count_queries="$work_dir/public-table-count-queries.sql"
table_counts="$work_dir/public-table-counts.tsv"
snapshot_query "
  select format(
    'select %L || chr(9) || count(*) from only %I.%I',
    relation.relname,
    namespace.nspname,
    relation.relname
  )
  from pg_catalog.pg_class relation
  join pg_catalog.pg_namespace namespace on namespace.oid = relation.relnamespace
  where namespace.nspname = 'public'
    and relation.relkind in ('r', 'p')
  order by relation.relname
" >"$table_count_queries"
[[ -s "$table_count_queries" ]] || backup_die "exported snapshot has no public tables to inventory"
while IFS= read -r query; do
  [[ -n "$query" ]] || continue
  snapshot_query "$query" >>"$table_counts"
done <"$table_count_queries"
[[ -s "$table_counts" ]] || backup_die "exported snapshot did not produce public table counts"

pg_dump --snapshot="$snapshot_id" --format=custom --file="$archive_path" --no-owner --no-privileges
[[ -s "$archive_path" ]] || backup_die "pg_dump produced an empty archive"

printf 'commit;\n' >&3
exec 3>&-
snapshot_holder_input_open=false
wait "$snapshot_holder_pid" || backup_die "PostgreSQL exported snapshot holder did not close cleanly"
snapshot_holder_pid=""

toc_path="$work_dir/archive.toc"
pg_restore --list "$archive_path" >"$toc_path"
toc_entries="$(grep -cve '^[[:space:]]*;' "$toc_path" || true)"
(( toc_entries > 0 )) || backup_die "pg_restore could not read a non-empty archive table of contents"

age --encrypt --recipient "$TRADEPILOT_BACKUP_AGE_RECIPIENT" --output "$encrypted_path" "$archive_path"
[[ -s "$encrypted_path" ]] || backup_die "age produced an empty encrypted artifact"
chmod 600 "$encrypted_path"

encrypted_sha256="$(backup_sha256 "$encrypted_path")"
encrypted_bytes="$(wc -c <"$encrypted_path" | tr -d '[:space:]')"

completeness_manifest_path="$work_dir/completeness-manifest.json"
python3 "$SCRIPT_DIR/validate-completeness-manifest.py" create \
  --counts "$table_counts" \
  --output "$completeness_manifest_path" \
  --artifact "$encrypted_name" \
  --encrypted-artifact-sha256 "$encrypted_sha256"
age --encrypt --recipient "$TRADEPILOT_BACKUP_AGE_RECIPIENT" --output "$completeness_manifest_encrypted_path" "$completeness_manifest_path"
[[ -s "$completeness_manifest_encrypted_path" ]] || backup_die "age produced an empty encrypted completeness manifest"
chmod 600 "$completeness_manifest_encrypted_path"
completeness_manifest_sha256="$(backup_sha256 "$completeness_manifest_encrypted_path")"
completeness_manifest_bytes="$(wc -c <"$completeness_manifest_encrypted_path" | tr -d '[:space:]')"

python3 - "$metadata_path" "$encrypted_name" "$timestamp" "$server_version" "$toc_entries" "$encrypted_sha256" "$encrypted_bytes" "$completeness_manifest_name" "$completeness_manifest_sha256" "$completeness_manifest_bytes" <<'PY'
import json
import sys
from pathlib import Path

path, name, timestamp, version, toc_entries, checksum, size, manifest_name, manifest_checksum, manifest_size = sys.argv[1:]
Path(path).write_text(json.dumps({
    "artifact": name,
    "createdAtUtc": timestamp,
    "postgresServerVersionNum": int(version),
    "archiveFormat": "pg_dump custom",
    "archiveTocEntries": int(toc_entries),
    "encryption": "age asymmetric recipient encryption",
    "encryptedSha256": checksum,
    "encryptedBytes": int(size),
    "completenessManifest": {
        "artifact": manifest_name,
        "encryption": "age asymmetric recipient encryption",
        "encryptedSha256": manifest_checksum,
        "encryptedBytes": int(manifest_size),
    },
}, indent=2) + "\n", encoding="utf-8")
PY
chmod 600 "$metadata_path"

backup_completed=true
printf 'Snapshot-consistent archive and encrypted completeness manifest are ready for approved upload: %s\n' "$encrypted_name"
