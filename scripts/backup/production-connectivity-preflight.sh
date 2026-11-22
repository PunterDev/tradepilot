#!/usr/bin/env bash
# Performs an explicitly approved, read-only production PostgreSQL connectivity preflight.

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

[[ "${TRADEPILOT_PRODUCTION_CONNECTIVITY_PREFLIGHT_APPROVED:-}" == "yes" ]] || \
  backup_die "set TRADEPILOT_PRODUCTION_CONNECTIVITY_PREFLIGHT_APPROVED=yes to run this read-only production preflight"

backup_require_command pg_dump
backup_require_command pg_restore
backup_require_command psql
backup_require_command python3

pg_dump --version | grep -Eq 'PostgreSQL\) 17\.' || backup_die "PostgreSQL 17 client tooling is required"

for required in \
  TRADEPILOT_BACKUP_PGHOST \
  TRADEPILOT_BACKUP_PGPORT \
  TRADEPILOT_BACKUP_PGDATABASE \
  TRADEPILOT_BACKUP_PGUSER \
  TRADEPILOT_BACKUP_PGPASSWORD \
  TRADEPILOT_BACKUP_PGSSLROOTCERT_BASE64; do
  backup_require_var "$required"
done

connect_timeout="${TRADEPILOT_BACKUP_PGCONNECT_TIMEOUT:-15}"
[[ "$connect_timeout" =~ ^[1-9][0-9]*$ ]] || backup_die "TRADEPILOT_BACKUP_PGCONNECT_TIMEOUT must be a positive integer"

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/tradepilot-connectivity-preflight.XXXXXX")"
cleanup() {
  unset PGPASSWORD TRADEPILOT_BACKUP_PGPASSWORD PGSSLROOTCERT PGOPTIONS
  rm -rf "$work_dir"
}
trap cleanup EXIT HUP INT TERM

export PGHOST="$TRADEPILOT_BACKUP_PGHOST"
export PGPORT="$TRADEPILOT_BACKUP_PGPORT"
export PGDATABASE="$TRADEPILOT_BACKUP_PGDATABASE"
export PGUSER="$TRADEPILOT_BACKUP_PGUSER"
export PGPASSWORD="$TRADEPILOT_BACKUP_PGPASSWORD"
export PGSSLMODE=verify-full
export PGCONNECT_TIMEOUT="$connect_timeout"
export PGSSLROOTCERT="$work_dir/root.crt"
export PGOPTIONS='-c default_transaction_read_only=on -c statement_timeout=15000'
backup_decode_base64 "$TRADEPILOT_BACKUP_PGSSLROOTCERT_BASE64" "$PGSSLROOTCERT"
chmod 600 "$PGSSLROOTCERT"

# The query and schema-only archive are read-only and retained only in the private temp directory.
psql -X -q -v ON_ERROR_STOP=1 -c 'select 1' >/dev/null
schema_archive="$work_dir/schema-only.dump"
pg_dump --format=custom --schema-only --no-owner --no-privileges --file="$schema_archive"
[[ -s "$schema_archive" ]] || backup_die "schema-only pg_dump produced an empty archive"
pg_restore --list "$schema_archive" >/dev/null

printf 'Production PostgreSQL connectivity preflight passed: TLS, authentication, read-only query, and schema-only pg_dump.\n'
