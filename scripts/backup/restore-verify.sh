#!/usr/bin/env bash
# Restores an encrypted backup only into an explicitly disposable PostgreSQL database.

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

backup_require_command age
backup_require_command pg_restore
backup_require_command psql
backup_require_command python3
backup_require_var TRADEPILOT_BACKUP_AGE_IDENTITY_FILE
backup_require_var TRADEPILOT_RESTORE_CONFIRM_DISPOSABLE
[[ "$TRADEPILOT_RESTORE_CONFIRM_DISPOSABLE" == "yes" ]] || backup_die "set TRADEPILOT_RESTORE_CONFIRM_DISPOSABLE=yes only for an isolated disposable database"
[[ -f "$TRADEPILOT_BACKUP_AGE_IDENTITY_FILE" ]] || backup_die "age identity file is unavailable"

for required in \
  TRADEPILOT_RESTORE_PGDATABASE \
  TRADEPILOT_RESTORE_PGUSER \
  TRADEPILOT_RESTORE_PGPASSWORD; do
  backup_require_var "$required"
done

restore_docker_container="${TRADEPILOT_RESTORE_DOCKER_CONTAINER:-}"
if [[ -n "$restore_docker_container" ]]; then
  backup_require_command docker
else
  for required in TRADEPILOT_RESTORE_PGHOST TRADEPILOT_RESTORE_PGPORT; do
    backup_require_var "$required"
  done
fi

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/tradepilot-restore.XXXXXX")"
cleanup() {
  unset PGPASSWORD TRADEPILOT_RESTORE_PGPASSWORD
  rm -rf "$work_dir"
}
trap cleanup EXIT HUP INT TERM

encrypted_path="$work_dir/backup.dump.age"
metadata_path="$work_dir/backup.dump.age.metadata.json"
if [[ -n "${TRADEPILOT_BACKUP_ENCRYPTED_FILE:-}" ]]; then
  [[ -f "$TRADEPILOT_BACKUP_ENCRYPTED_FILE" ]] || backup_die "local encrypted backup file is unavailable"
  cp "$TRADEPILOT_BACKUP_ENCRYPTED_FILE" "$encrypted_path"
  cp "${TRADEPILOT_BACKUP_ENCRYPTED_FILE}.metadata.json" "$metadata_path"
else
  backup_require_command aws
  backup_require_var TRADEPILOT_BACKUP_S3_BUCKET
  backup_require_var TRADEPILOT_BACKUP_OBJECT_KEY
  backup_s3_args
  export AWS_EC2_METADATA_DISABLED=true
  aws "${BACKUP_S3_ARGS[@]}" s3 cp "s3://${TRADEPILOT_BACKUP_S3_BUCKET}/${TRADEPILOT_BACKUP_OBJECT_KEY}" "$encrypted_path" --only-show-errors
  aws "${BACKUP_S3_ARGS[@]}" s3 cp "s3://${TRADEPILOT_BACKUP_S3_BUCKET}/${TRADEPILOT_BACKUP_OBJECT_KEY}.metadata.json" "$metadata_path" --only-show-errors
fi

expected_checksum="$(python3 - "$metadata_path" <<'PY'
import json
import sys
value = json.load(open(sys.argv[1], encoding="utf-8")).get("encryptedSha256")
if not isinstance(value, str) or len(value) != 64:
    raise SystemExit("backup metadata has no valid encryptedSha256")
print(value)
PY
)"
actual_checksum="$(backup_sha256 "$encrypted_path")"
[[ "$actual_checksum" == "$expected_checksum" ]] || backup_die "encrypted backup checksum mismatch"

archive_path="$work_dir/backup.dump"
age --decrypt --identity "$TRADEPILOT_BACKUP_AGE_IDENTITY_FILE" --output "$archive_path" "$encrypted_path"
[[ -s "$archive_path" ]] || backup_die "decryption produced an empty archive"
pg_restore --list "$archive_path" >/dev/null

export PGDATABASE="$TRADEPILOT_RESTORE_PGDATABASE"
export PGUSER="$TRADEPILOT_RESTORE_PGUSER"
export PGPASSWORD="$TRADEPILOT_RESTORE_PGPASSWORD"
export PGSSLMODE="${TRADEPILOT_RESTORE_PGSSLMODE:-disable}"

if [[ -z "$restore_docker_container" ]]; then
  export PGHOST="$TRADEPILOT_RESTORE_PGHOST"
  export PGPORT="$TRADEPILOT_RESTORE_PGPORT"
fi

restore_psql() {
  if [[ -n "$restore_docker_container" ]]; then
    docker exec --interactive --env PGPASSWORD "$restore_docker_container" \
      psql -X -A -t -v ON_ERROR_STOP=1 --username="$PGUSER" --dbname="$PGDATABASE" "$@"
  else
    psql -X -A -t -v ON_ERROR_STOP=1 "$@"
  fi
}

restore_archive() {
  if [[ -n "$restore_docker_container" ]]; then
    docker exec --interactive --env PGPASSWORD "$restore_docker_container" \
      pg_restore --exit-on-error --no-owner --no-privileges --username="$PGUSER" --dbname="$PGDATABASE" <"$archive_path"
  else
    pg_restore --exit-on-error --no-owner --no-privileges --dbname="$PGDATABASE" "$archive_path"
  fi
}

existing_user_managed_state="$(restore_psql -c "
  select count(*)
  from (
    select namespace.oid
    from pg_catalog.pg_namespace namespace
    where namespace.nspname not in ('pg_catalog', 'information_schema', 'public')
      and namespace.nspname not like 'pg_toast%'
      and namespace.nspname not like 'pg_temp_%'
    union
    select relation.oid
    from pg_catalog.pg_class relation
    join pg_catalog.pg_namespace namespace on namespace.oid = relation.relnamespace
    where namespace.nspname = 'public'
    union
    select routine.oid
    from pg_catalog.pg_proc routine
    join pg_catalog.pg_namespace namespace on namespace.oid = routine.pronamespace
    where namespace.nspname = 'public'
    union
    select type.oid
    from pg_catalog.pg_type type
    join pg_catalog.pg_namespace namespace on namespace.oid = type.typnamespace
    where namespace.nspname = 'public'
    union
    select extension.oid
    from pg_catalog.pg_extension extension
    where extension.extname <> 'plpgsql'
  ) existing_state
")"
[[ "$existing_user_managed_state" == "0" ]] || backup_die "restore target has user-managed schemas or objects; refusing to overwrite a non-disposable database"
restore_archive

for table in users trading_accounts trades import_batches journal_entries flyway_schema_history; do
  table_present="$(restore_psql -c "select to_regclass('public.${table}') is not null")"
  [[ "$table_present" == "t" ]] || backup_die "missing required restored table: $table"
  restore_psql -c "select count(*) from public.${table}" >/dev/null
done
invalid_constraints="$(restore_psql -c "select count(*) from pg_constraint where contype = 'f' and not convalidated")"
[[ "$invalid_constraints" == "0" ]] || backup_die "restored database has unvalidated foreign-key constraints"

printf 'Restore verification passed for an isolated disposable PostgreSQL database.\n'
