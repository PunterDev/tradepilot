#!/usr/bin/env bash
# Confirms a failed backup connection leaves neither plaintext dumps nor artifacts behind.

set -Eeuo pipefail
umask 077

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
BACKUP_SCRIPT="$ROOT_DIR/scripts/backup/backup-production-db.sh"

for command in age age-keygen pg_dump pg_restore psql python3; do
  command -v "$command" >/dev/null || {
    printf 'Missing required command: %s\n' "$command" >&2
    exit 1
  }
done

temp_dir="$(mktemp -d "${TMPDIR:-/tmp}/tradepilot-backup-failure.XXXXXX")"
cleanup() { rm -rf "$temp_dir"; }
trap cleanup EXIT HUP INT TERM

identity_file="$temp_dir/age-identity.txt"
age-keygen -o "$identity_file" >/dev/null 2>&1
recipient="$(awk '/^# public key: / {print $4}' "$identity_file")"
work_tmp="$temp_dir/work"
output_dir="$temp_dir/output"
mkdir -p "$work_tmp" "$output_dir"

set +e
TMPDIR="$work_tmp" \
TRADEPILOT_BACKUP_PGHOST=127.0.0.1 \
TRADEPILOT_BACKUP_PGPORT=1 \
TRADEPILOT_BACKUP_PGDATABASE=tradepilot \
TRADEPILOT_BACKUP_PGUSER=postgres \
TRADEPILOT_BACKUP_PGPASSWORD=synthetic-test-password \
TRADEPILOT_BACKUP_PGSSLROOTCERT_BASE64="$(printf 'synthetic test certificate' | base64)" \
TRADEPILOT_BACKUP_AGE_RECIPIENT="$recipient" \
TRADEPILOT_BACKUP_OUTPUT_DIR="$output_dir" \
  bash "$BACKUP_SCRIPT" >/dev/null 2>&1
status=$?
set -e

(( status != 0 )) || {
  printf 'Backup unexpectedly succeeded against an unreachable local port.\n' >&2
  exit 1
}
[[ -z "$(find "$work_tmp" -type f -print -quit)" ]] || {
  printf 'Failed backup left temporary plaintext data behind.\n' >&2
  exit 1
}
[[ -z "$(find "$output_dir" -type f -print -quit)" ]] || {
  printf 'Failed backup left an output artifact behind.\n' >&2
  exit 1
}
printf 'Failed-backup cleanup verification passed.\n'
