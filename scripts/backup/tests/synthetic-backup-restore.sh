#!/usr/bin/env bash
# Exercises the production backup and restore scripts against disposable PostgreSQL 17 containers.

set -Eeuo pipefail
umask 077

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
BACKUP_SCRIPT="$ROOT_DIR/scripts/backup/backup-production-db.sh"
RESTORE_SCRIPT="$ROOT_DIR/scripts/backup/restore-verify.sh"
UPLOAD_SCRIPT="$ROOT_DIR/scripts/backup/upload-s3-backup.sh"
FRESHNESS_SCRIPT="$ROOT_DIR/scripts/backup/check-backup-freshness.sh"

for command in docker pg_dump pg_restore psql pg_isready age age-keygen python3; do
  command -v "$command" >/dev/null || {
    printf 'Missing required command: %s\n' "$command" >&2
    exit 1
  }
done

temp_dir="$(mktemp -d "${TMPDIR:-/tmp}/tradepilot-backup-synthetic.XXXXXX")"
suffix="${RANDOM}${RANDOM}"
source_name="tradepilot-backup-source-$suffix"
restore_name="tradepilot-backup-restore-$suffix"
blocked_name="tradepilot-backup-blocked-$suffix"
function_blocked_name="tradepilot-backup-function-blocked-$suffix"

cleanup() {
  docker rm -f "$source_name" "$restore_name" "$blocked_name" "$function_blocked_name" >/dev/null 2>&1 || true
  rm -rf "$temp_dir"
}
trap cleanup EXIT HUP INT TERM

make_fake_aws() {
  local fake_bin="$temp_dir/fake-bin"
  mkdir -p "$fake_bin"
  cat >"$fake_bin/aws" <<'FAKE_AWS'
#!/usr/bin/env bash
set -Eeuo pipefail
while [[ "${1:-}" == --region || "${1:-}" == --endpoint-url ]]; do shift 2; done
service="$1"
operation="$2"
shift 2
object_path() {
  local uri="$1"
  printf '%s/%s\n' "$FAKE_S3_ROOT" "${uri#s3://}"
}
case "$service:$operation" in
  s3:cp)
    source="$1"
    destination="$2"
    shift 2
    metadata=""
    while (($#)); do
      if [[ "$1" == --metadata ]]; then metadata="$2"; shift 2; else shift; fi
    done
    if [[ "$source" == s3://* ]]; then
      cp "$(object_path "$source")" "$destination"
    else
      target="$(object_path "$destination")"
      mkdir -p "$(dirname "$target")"
      cp "$source" "$target"
      if [[ -n "$metadata" ]]; then printf '%s\n' "${metadata#sha256=}" >"$target.sha256"; fi
    fi
    ;;
  s3api:head-object)
    bucket=""; key=""; query=""
    while (($#)); do
      case "$1" in
        --bucket) bucket="$2"; shift 2 ;;
        --key) key="$2"; shift 2 ;;
        --query) query="$2"; shift 2 ;;
        *) shift ;;
      esac
    done
    target="$FAKE_S3_ROOT/$bucket/$key"
    [[ -f "$target" ]]
    if [[ "$query" == "Metadata.sha256" ]]; then cat "$target.sha256"; else wc -c <"$target" | tr -d '[:space:]'; fi
    ;;
  s3api:list-objects-v2)
    bucket=""; prefix=""
    while (($#)); do
      case "$1" in
        --bucket) bucket="$2"; shift 2 ;;
        --prefix) prefix="$2"; shift 2 ;;
        *) shift ;;
      esac
    done
    python3 - "$FAKE_S3_ROOT/$bucket" "$prefix" <<'PY'
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

root, prefix = Path(sys.argv[1]), sys.argv[2]
items = []
for path in root.rglob('*') if root.exists() else []:
    if path.is_file() and not path.name.endswith('.sha256'):
        key = path.relative_to(root).as_posix()
        if key.startswith(prefix):
            items.append({"Key": key, "LastModified": datetime.fromtimestamp(path.stat().st_mtime, timezone.utc).isoformat().replace('+00:00', 'Z')})
print(json.dumps({"Contents": items}))
PY
    ;;
  s3api:delete-object)
    bucket=""; key=""
    while (($#)); do
      case "$1" in
        --bucket) bucket="$2"; shift 2 ;;
        --key) key="$2"; shift 2 ;;
        *) shift ;;
      esac
    done
    rm -f "$FAKE_S3_ROOT/$bucket/$key" "$FAKE_S3_ROOT/$bucket/$key.sha256"
    ;;
  *) printf 'Unsupported fake AWS command: %s %s\n' "$service" "$operation" >&2; exit 2 ;;
esac
FAKE_AWS
  chmod 700 "$fake_bin/aws"
  printf '%s\n' "$fake_bin"
}

start_postgres() {
  local name="$1"
  docker run --detach --rm --name "$name" \
    -e POSTGRES_PASSWORD=synthetic-test-password \
    -e POSTGRES_DB=tradepilot \
    -p 127.0.0.1::5432 postgres:17-alpine >/dev/null
  local port
  port="$(docker port "$name" 5432/tcp | awk -F: '{print $NF}')"
  until PGPASSWORD=synthetic-test-password pg_isready -h 127.0.0.1 -p "$port" -U postgres -d tradepilot >/dev/null 2>&1; do
    sleep 1
  done
  printf '%s\n' "$port"
}

source_port="$(start_postgres "$source_name")"
PGPASSWORD=synthetic-test-password psql -X -v ON_ERROR_STOP=1 \
  -h 127.0.0.1 -p "$source_port" -U postgres -d tradepilot <<'SQL'
create table users (id bigint primary key, email text not null);
create table trading_accounts (id bigint primary key, user_id bigint not null references users(id));
create table import_batches (id bigint primary key, user_id bigint not null references users(id));
create table trades (id bigint primary key, account_id bigint not null references trading_accounts(id));
create table journal_entries (id bigint primary key, user_id bigint not null references users(id));
create table flyway_schema_history (installed_rank integer primary key, version varchar(50), description varchar(200));
insert into users values (1, 'synthetic-user@example.test');
insert into trading_accounts values (1, 1);
insert into import_batches values (1, 1);
insert into trades values (1, 1);
insert into journal_entries values (1, 1);
insert into flyway_schema_history values (1, '001', 'synthetic baseline');
SQL

identity_file="$temp_dir/age-identity.txt"
age-keygen -o "$identity_file" >/dev/null 2>&1
recipient="$(awk '/^# public key: / {print $4}' "$identity_file")"
backup_output="$temp_dir/backup-output"

TRADEPILOT_BACKUP_PGHOST=127.0.0.1 \
TRADEPILOT_BACKUP_PGPORT="$source_port" \
TRADEPILOT_BACKUP_PGDATABASE=tradepilot \
TRADEPILOT_BACKUP_PGUSER=postgres \
TRADEPILOT_BACKUP_PGPASSWORD=synthetic-test-password \
TRADEPILOT_BACKUP_AGE_RECIPIENT="$recipient" \
TRADEPILOT_BACKUP_PGSSLMODE=disable \
TRADEPILOT_BACKUP_SYNTHETIC_TEST_MODE=true \
TRADEPILOT_BACKUP_OUTPUT_DIR="$backup_output" \
  bash "$BACKUP_SCRIPT"

encrypted_file="$(find "$backup_output" -type f -name '*.dump.age' -print -quit)"
[[ -n "$encrypted_file" && -s "$encrypted_file" ]]
[[ -s "$encrypted_file.metadata.json" ]]
if grep -aq 'synthetic-user@example.test' "$encrypted_file"; then
  printf 'Encrypted artifact unexpectedly contains synthetic plaintext.\n' >&2
  exit 1
fi

fake_bin="$(make_fake_aws)"
export FAKE_S3_ROOT="$temp_dir/fake-s3"
export PATH="$fake_bin:$PATH"
export AWS_ACCESS_KEY_ID=synthetic-access-key
export AWS_SECRET_ACCESS_KEY=synthetic-secret-key
export TRADEPILOT_BACKUP_S3_BUCKET=synthetic-backups
export TRADEPILOT_BACKUP_S3_PREFIX=tradepilot/production/postgres
export TRADEPILOT_BACKUP_S3_REGION=us-east-1
export TRADEPILOT_BACKUP_S3_ENDPOINT_URL=https://s3.synthetic.invalid
export TRADEPILOT_BACKUP_RETENTION_DAYS=21
bash "$UPLOAD_SCRIPT" "$encrypted_file"
bash "$FRESHNESS_SCRIPT"

remote_key="tradepilot/production/postgres/$(basename "$encrypted_file")"
[[ -s "$FAKE_S3_ROOT/synthetic-backups/$remote_key" ]]
[[ -s "$FAKE_S3_ROOT/synthetic-backups/$remote_key.metadata.json" ]]

restore_port="$(start_postgres "$restore_name")"
TRADEPILOT_BACKUP_AGE_IDENTITY_FILE="$identity_file" \
TRADEPILOT_RESTORE_CONFIRM_DISPOSABLE=yes \
TRADEPILOT_RESTORE_PGHOST=127.0.0.1 \
TRADEPILOT_RESTORE_PGPORT="$restore_port" \
TRADEPILOT_RESTORE_PGDATABASE=tradepilot \
TRADEPILOT_RESTORE_PGUSER=postgres \
TRADEPILOT_RESTORE_PGPASSWORD=synthetic-test-password \
TRADEPILOT_RESTORE_PGSSLMODE=disable \
TRADEPILOT_BACKUP_OBJECT_KEY="$remote_key" \
  bash "$RESTORE_SCRIPT"

restored_users="$(PGPASSWORD=synthetic-test-password psql -X -A -t -v ON_ERROR_STOP=1 -h 127.0.0.1 -p "$restore_port" -U postgres -d tradepilot -c 'select count(*) from users')"
[[ "$restored_users" == "1" ]]

# A target with an object outside public must also be refused before restore.
blocked_port="$(start_postgres "$blocked_name")"
PGPASSWORD=synthetic-test-password psql -X -v ON_ERROR_STOP=1 -h 127.0.0.1 -p "$blocked_port" -U postgres -d tradepilot <<'SQL'
create schema unmanaged;
create table unmanaged.marker (id integer primary key);
SQL
set +e
TRADEPILOT_BACKUP_AGE_IDENTITY_FILE="$identity_file" \
TRADEPILOT_RESTORE_CONFIRM_DISPOSABLE=yes \
TRADEPILOT_RESTORE_PGHOST=127.0.0.1 \
TRADEPILOT_RESTORE_PGPORT="$blocked_port" \
TRADEPILOT_RESTORE_PGDATABASE=tradepilot \
TRADEPILOT_RESTORE_PGUSER=postgres \
TRADEPILOT_RESTORE_PGPASSWORD=synthetic-test-password \
TRADEPILOT_RESTORE_PGSSLMODE=disable \
TRADEPILOT_BACKUP_OBJECT_KEY="$remote_key" \
  bash "$RESTORE_SCRIPT" >/dev/null 2>&1
blocked_status=$?
set -e
(( blocked_status != 0 )) || {
  printf 'Restore unexpectedly accepted a target with user-managed schema objects.\n' >&2
  exit 1
}
marker_exists="$(PGPASSWORD=synthetic-test-password psql -X -A -t -v ON_ERROR_STOP=1 -h 127.0.0.1 -p "$blocked_port" -U postgres -d tradepilot -c "select to_regclass('unmanaged.marker') is not null")"
[[ "$marker_exists" == "t" ]]

# A public function and standalone type must also make the target ineligible.
function_blocked_port="$(start_postgres "$function_blocked_name")"
PGPASSWORD=synthetic-test-password psql -X -v ON_ERROR_STOP=1 -h 127.0.0.1 -p "$function_blocked_port" -U postgres -d tradepilot <<'SQL'
create type public.marker_status as enum ('READY');
create function public.marker_function() returns integer language sql as 'select 1';
SQL
set +e
TRADEPILOT_BACKUP_AGE_IDENTITY_FILE="$identity_file" \
TRADEPILOT_RESTORE_CONFIRM_DISPOSABLE=yes \
TRADEPILOT_RESTORE_PGHOST=127.0.0.1 \
TRADEPILOT_RESTORE_PGPORT="$function_blocked_port" \
TRADEPILOT_RESTORE_PGDATABASE=tradepilot \
TRADEPILOT_RESTORE_PGUSER=postgres \
TRADEPILOT_RESTORE_PGPASSWORD=synthetic-test-password \
TRADEPILOT_RESTORE_PGSSLMODE=disable \
TRADEPILOT_BACKUP_OBJECT_KEY="$remote_key" \
  bash "$RESTORE_SCRIPT" >/dev/null 2>&1
function_blocked_status=$?
set -e
(( function_blocked_status != 0 )) || {
  printf 'Restore unexpectedly accepted a target with public function/type objects.\n' >&2
  exit 1
}
function_exists="$(PGPASSWORD=synthetic-test-password psql -X -A -t -v ON_ERROR_STOP=1 -h 127.0.0.1 -p "$function_blocked_port" -U postgres -d tradepilot -c "select to_regprocedure('public.marker_function()') is not null")"
[[ "$function_exists" == "t" ]]
printf 'Synthetic PostgreSQL 17 backup/encrypt/decrypt/restore verification passed.\n'
