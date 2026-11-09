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

restore_scope="${TRADEPILOT_RESTORE_SCOPE:-complete}"
case "$restore_scope" in
  complete|application-public) ;;
  *) backup_die "TRADEPILOT_RESTORE_SCOPE must be complete or application-public" ;;
esac

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
completeness_manifest_encrypted_path="$work_dir/backup.dump.age.completeness.json.age"
if [[ -n "${TRADEPILOT_BACKUP_ENCRYPTED_FILE:-}" ]]; then
  [[ -f "$TRADEPILOT_BACKUP_ENCRYPTED_FILE" ]] || backup_die "local encrypted backup file is unavailable"
  source_encrypted_name="$(basename "$TRADEPILOT_BACKUP_ENCRYPTED_FILE")"
  cp "$TRADEPILOT_BACKUP_ENCRYPTED_FILE" "$encrypted_path"
  cp "${TRADEPILOT_BACKUP_ENCRYPTED_FILE}.metadata.json" "$metadata_path"
  cp "${TRADEPILOT_BACKUP_ENCRYPTED_FILE}.completeness.json.age" "$completeness_manifest_encrypted_path"
else
  backup_require_command aws
  backup_require_var TRADEPILOT_BACKUP_S3_BUCKET
  backup_require_var TRADEPILOT_BACKUP_OBJECT_KEY
  source_encrypted_name="$(basename "$TRADEPILOT_BACKUP_OBJECT_KEY")"
  backup_s3_args
  export AWS_EC2_METADATA_DISABLED=true
  aws "${BACKUP_S3_ARGS[@]}" s3 cp "s3://${TRADEPILOT_BACKUP_S3_BUCKET}/${TRADEPILOT_BACKUP_OBJECT_KEY}" "$encrypted_path" --only-show-errors
  aws "${BACKUP_S3_ARGS[@]}" s3 cp "s3://${TRADEPILOT_BACKUP_S3_BUCKET}/${TRADEPILOT_BACKUP_OBJECT_KEY}.metadata.json" "$metadata_path" --only-show-errors
  aws "${BACKUP_S3_ARGS[@]}" s3 cp "s3://${TRADEPILOT_BACKUP_S3_BUCKET}/${TRADEPILOT_BACKUP_OBJECT_KEY}.completeness.json.age" "$completeness_manifest_encrypted_path" --only-show-errors
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
expected_manifest_checksum="$(python3 - "$metadata_path" "$source_encrypted_name" <<'PY'
import json
import sys

metadata = json.load(open(sys.argv[1], encoding="utf-8"))
manifest = metadata.get("completenessManifest")
expected_name = f"{sys.argv[2]}.completeness.json.age"
if not isinstance(manifest, dict) or manifest.get("artifact") != expected_name:
    raise SystemExit("backup metadata has no completeness manifest bound to this archive")
value = manifest.get("encryptedSha256")
if not isinstance(value, str) or len(value) != 64:
    raise SystemExit("backup metadata has no valid encrypted completeness manifest checksum")
print(value)
PY
)"
actual_manifest_checksum="$(backup_sha256 "$completeness_manifest_encrypted_path")"
[[ "$actual_manifest_checksum" == "$expected_manifest_checksum" ]] || backup_die "encrypted completeness manifest checksum mismatch"

archive_path="$work_dir/backup.dump"
age --decrypt --identity "$TRADEPILOT_BACKUP_AGE_IDENTITY_FILE" --output "$archive_path" "$encrypted_path"
[[ -s "$archive_path" ]] || backup_die "decryption produced an empty archive"
pg_restore --list "$archive_path" >/dev/null

scope_manifest=""
if [[ "$restore_scope" == "application-public" ]]; then
  scope_ddl="$work_dir/public-schema.sql"
  scope_manifest="$work_dir/application-scope-manifest.json"
  scope_sequence_list="$work_dir/public-sequence-set.list"
  scope_sequence_states="$work_dir/public-sequence-state.sql"
  pg_restore --schema=public --strict-names --schema-only --file="$scope_ddl" "$archive_path"
  python3 "$SCRIPT_DIR/validate-application-scope.py" --ddl "$scope_ddl" --manifest "$scope_manifest"
  pg_restore --list "$archive_path" | awk '/ SEQUENCE SET public / { print }' >"$scope_sequence_list"
  [[ -s "$scope_sequence_list" ]] || backup_die "application-scope archive has sequences without sequence-state entries"
  pg_restore --use-list="$scope_sequence_list" --file="$scope_sequence_states" "$archive_path"
  [[ -s "$scope_sequence_states" ]] || backup_die "application-scope archive sequence-state extraction failed"
  completeness_manifest="$work_dir/completeness-manifest.json"
  age --decrypt --identity "$TRADEPILOT_BACKUP_AGE_IDENTITY_FILE" --output "$completeness_manifest" "$completeness_manifest_encrypted_path"
  python3 "$SCRIPT_DIR/validate-completeness-manifest.py" emit-count-queries \
    --manifest "$completeness_manifest" \
    --artifact "$source_encrypted_name" \
    --encrypted-artifact-sha256 "$actual_checksum" >"$work_dir/completeness-count-queries.sql"
fi

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
    # A non-streaming SQL command must not inherit a caller's stdin: manifest
    # validation iterates query lines and Docker's interactive stdin would
    # consume the remaining inventory after the first count.
    docker exec --env PGPASSWORD "$restore_docker_container" \
      psql -X -A -t -v ON_ERROR_STOP=1 --username="$PGUSER" --dbname="$PGDATABASE" "$@"
  else
    psql -X -A -t -v ON_ERROR_STOP=1 "$@"
  fi
}

restore_psql_file() {
  local path="$1"
  if [[ -n "$restore_docker_container" ]]; then
    docker exec --interactive --env PGPASSWORD "$restore_docker_container" \
      psql -X -v ON_ERROR_STOP=1 --username="$PGUSER" --dbname="$PGDATABASE" -f - <"$path"
  else
    psql -X -v ON_ERROR_STOP=1 -f "$path"
  fi
}

restore_archive() {
  local scope_args=()
  if [[ "$restore_scope" == "application-public" ]]; then
    scope_args+=(--schema=public --strict-names)
  fi
  if [[ -n "$restore_docker_container" ]]; then
    docker exec --interactive --env PGPASSWORD "$restore_docker_container" \
      pg_restore --exit-on-error --no-owner --no-privileges "${scope_args[@]}" --username="$PGUSER" --dbname="$PGDATABASE" <"$archive_path"
  else
    pg_restore --exit-on-error --no-owner --no-privileges "${scope_args[@]}" --dbname="$PGDATABASE" "$archive_path"
  fi
}

scope_manifest_value() {
  python3 - "$scope_manifest" "$1" <<'PY'
import json
import sys
print(json.load(open(sys.argv[1], encoding="utf-8"))[sys.argv[2]])
PY
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

if [[ "$restore_scope" == "application-public" ]]; then
  restored_table_counts="$work_dir/restored-public-table-counts.tsv"
  while IFS= read -r query; do
    [[ -n "$query" ]] || continue
    restore_psql -c "$query" >>"$restored_table_counts"
  done <"$work_dir/completeness-count-queries.sql"
  python3 "$SCRIPT_DIR/validate-completeness-manifest.py" validate \
    --manifest "$completeness_manifest" --counts "$restored_table_counts"

  for check in \
    "tables|select count(*) from pg_catalog.pg_class relation join pg_catalog.pg_namespace namespace on namespace.oid = relation.relnamespace where namespace.nspname = 'public' and relation.relkind in ('r', 'p')" \
    "functions|select count(*) from pg_catalog.pg_proc routine join pg_catalog.pg_namespace namespace on namespace.oid = routine.pronamespace where namespace.nspname = 'public'" \
    "rls_tables|select count(*) from pg_catalog.pg_class relation join pg_catalog.pg_namespace namespace on namespace.oid = relation.relnamespace where namespace.nspname = 'public' and relation.relrowsecurity" \
    "policies|select count(*) from pg_catalog.pg_policy policy join pg_catalog.pg_class relation on relation.oid = policy.polrelid join pg_catalog.pg_namespace namespace on namespace.oid = relation.relnamespace where namespace.nspname = 'public'" \
    "triggers|select count(*) from pg_catalog.pg_trigger trigger join pg_catalog.pg_class relation on relation.oid = trigger.tgrelid join pg_catalog.pg_namespace namespace on namespace.oid = relation.relnamespace where namespace.nspname = 'public' and not trigger.tgisinternal"; do
    IFS='|' read -r manifest_key sql <<<"$check"
    expected_count="$(scope_manifest_value "$manifest_key")"
    actual_count="$(restore_psql -c "$sql")"
    [[ "$actual_count" == "$expected_count" ]] || backup_die "application-scope restore did not reproduce expected public ${manifest_key}"
  done

  expected_indexes="$(scope_manifest_value explicit_indexes)"
  actual_indexes="$(restore_psql -c "select count(*) from pg_catalog.pg_indexes where schemaname = 'public'")"
  (( actual_indexes >= expected_indexes )) || backup_die "application-scope restore did not reproduce expected public indexes"

  restored_sequence_inventory="$work_dir/restored-sequences.tsv"
  restore_psql -F $'\t' -c "
    select sequence.relname,
           coalesce(owner_relation.relname, ''),
           coalesce(owner_attribute.attname, ''),
           coalesce(dependency.deptype::text, ''),
           coalesce(nullif(owner_attribute.attidentity, ''), '')
    from pg_catalog.pg_class sequence
    join pg_catalog.pg_namespace sequence_namespace on sequence_namespace.oid = sequence.relnamespace
    left join pg_catalog.pg_depend dependency
      on dependency.classid = 'pg_class'::regclass
      and dependency.objid = sequence.oid
      and dependency.refclassid = 'pg_class'::regclass
      and dependency.refobjsubid > 0
      and dependency.deptype in ('a', 'i')
    left join pg_catalog.pg_class owner_relation on owner_relation.oid = dependency.refobjid
    left join pg_catalog.pg_attribute owner_attribute
      on owner_attribute.attrelid = dependency.refobjid
      and owner_attribute.attnum = dependency.refobjsubid
    where sequence_namespace.nspname = 'public'
      and sequence.relkind = 'S'
    order by sequence.relname
  " >"$restored_sequence_inventory"
  python3 "$SCRIPT_DIR/validate-application-scope.py" \
    --manifest "$scope_manifest" --actual-sequences "$restored_sequence_inventory"
  sequence_state_check="$work_dir/check-restored-sequence-state.sql"
  python3 "$SCRIPT_DIR/validate-application-scope.py" \
    --manifest "$scope_manifest" --sequence-states "$scope_sequence_states" \
    --emit-sequence-state-check >"$sequence_state_check"
  restore_psql_file "$sequence_state_check" >/dev/null
fi

for table in users trading_accounts trades import_batches journal_entries flyway_schema_history; do
  table_present="$(restore_psql -c "select to_regclass('public.${table}') is not null")"
  [[ "$table_present" == "t" ]] || backup_die "missing required restored table: $table"
  restore_psql -c "select count(*) from public.${table}" >/dev/null
done
invalid_constraints="$(restore_psql -c "select count(*) from pg_constraint where contype = 'f' and not convalidated")"
[[ "$invalid_constraints" == "0" ]] || backup_die "restored database has unvalidated foreign-key constraints"

printf 'Restore verification passed for an isolated disposable PostgreSQL database.\n'
