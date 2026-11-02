#!/usr/bin/env bash
# Downloads an encrypted backup and independently verifies its metadata checksum.

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

backup_require_command aws
backup_require_command python3
for required in \
  TRADEPILOT_BACKUP_S3_BUCKET \
  TRADEPILOT_BACKUP_OBJECT_KEY \
  AWS_ACCESS_KEY_ID \
  AWS_SECRET_ACCESS_KEY; do
  backup_require_var "$required"
done

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/tradepilot-upload-verify.XXXXXX")"
cleanup() { rm -rf "$work_dir"; }
trap cleanup EXIT HUP INT TERM

export AWS_EC2_METADATA_DISABLED=true
backup_s3_args
artifact_path="$work_dir/backup.dump.age"
metadata_path="$work_dir/backup.dump.age.metadata.json"
aws "${BACKUP_S3_ARGS[@]}" s3 cp "s3://${TRADEPILOT_BACKUP_S3_BUCKET}/${TRADEPILOT_BACKUP_OBJECT_KEY}" "$artifact_path" --only-show-errors
aws "${BACKUP_S3_ARGS[@]}" s3 cp "s3://${TRADEPILOT_BACKUP_S3_BUCKET}/${TRADEPILOT_BACKUP_OBJECT_KEY}.metadata.json" "$metadata_path" --only-show-errors

expected_checksum="$(python3 - "$metadata_path" <<'PY'
import json
import sys

value = json.load(open(sys.argv[1], encoding="utf-8")).get("encryptedSha256")
if not isinstance(value, str) or len(value) != 64:
    raise SystemExit("backup metadata has no valid encryptedSha256")
print(value)
PY
)"
actual_checksum="$(backup_sha256 "$artifact_path")"
[[ "$actual_checksum" == "$expected_checksum" ]] || backup_die "downloaded encrypted backup checksum mismatch"
printf 'Downloaded encrypted backup checksum verification passed.\n'
