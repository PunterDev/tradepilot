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
cleanup() {
  unset PGPASSWORD TRADEPILOT_BACKUP_PGPASSWORD
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

printf 'Starting encrypted PostgreSQL backup at %s UTC.\n' "$timestamp"
server_version="$(psql -X -A -t -v ON_ERROR_STOP=1 -c 'show server_version_num' | tr -d '[:space:]')"
[[ "$server_version" =~ ^[0-9]+$ ]] || backup_die "could not read PostgreSQL server version"

pg_dump --format=custom --file="$archive_path" --no-owner --no-privileges
[[ -s "$archive_path" ]] || backup_die "pg_dump produced an empty archive"

toc_path="$work_dir/archive.toc"
pg_restore --list "$archive_path" >"$toc_path"
toc_entries="$(grep -cve '^[[:space:]]*;' "$toc_path" || true)"
(( toc_entries > 0 )) || backup_die "pg_restore could not read a non-empty archive table of contents"

age --encrypt --recipient "$TRADEPILOT_BACKUP_AGE_RECIPIENT" --output "$encrypted_path" "$archive_path"
[[ -s "$encrypted_path" ]] || backup_die "age produced an empty encrypted artifact"
chmod 600 "$encrypted_path"

encrypted_sha256="$(backup_sha256 "$encrypted_path")"
encrypted_bytes="$(wc -c <"$encrypted_path" | tr -d '[:space:]')"
python3 - "$metadata_path" "$encrypted_name" "$timestamp" "$server_version" "$toc_entries" "$encrypted_sha256" "$encrypted_bytes" <<'PY'
import json
import sys
from pathlib import Path

path, name, timestamp, version, toc_entries, checksum, size = sys.argv[1:]
Path(path).write_text(json.dumps({
    "artifact": name,
    "createdAtUtc": timestamp,
    "postgresServerVersionNum": int(version),
    "archiveFormat": "pg_dump custom",
    "archiveTocEntries": int(toc_entries),
    "encryption": "age asymmetric recipient encryption",
    "encryptedSha256": checksum,
    "encryptedBytes": int(size),
}, indent=2) + "\n", encoding="utf-8")
PY
chmod 600 "$metadata_path"

printf 'Backup archive verified, encrypted, and ready for approved upload: %s\n' "$encrypted_name"
