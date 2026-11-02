#!/usr/bin/env bash
# Uploads one already encrypted artifact to private S3-compatible storage.

set -Eeuo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

artifact_path="${1:-}"
[[ -n "$artifact_path" && -f "$artifact_path" ]] || backup_die "usage: upload-s3-backup.sh <encrypted-artifact>"
metadata_path="${artifact_path}.metadata.json"
[[ -f "$metadata_path" ]] || backup_die "metadata file is missing for encrypted artifact"
manifest_path="${artifact_path}.completeness.json.age"
[[ -f "$manifest_path" ]] || backup_die "encrypted completeness manifest is missing for encrypted artifact"

backup_require_command aws
backup_require_command python3
for required in \
  TRADEPILOT_BACKUP_S3_BUCKET \
  AWS_ACCESS_KEY_ID \
  AWS_SECRET_ACCESS_KEY; do
  backup_require_var "$required"
done

export AWS_EC2_METADATA_DISABLED=true
backup_s3_args
prefix="$(backup_object_prefix)"
artifact_name="$(basename "$artifact_path")"
object_key="${prefix}/${artifact_name}"
metadata_key="${object_key}.metadata.json"
manifest_key="${object_key}.completeness.json.age"
checksum="$(backup_sha256 "$artifact_path")"
size="$(wc -c <"$artifact_path" | tr -d '[:space:]')"
manifest_checksum="$(backup_sha256 "$manifest_path")"
manifest_size="$(wc -c <"$manifest_path" | tr -d '[:space:]')"

python3 - "$metadata_path" "$(basename "$manifest_path")" "$manifest_checksum" "$manifest_size" <<'PY'
import json
import sys

metadata_path, name, checksum, size = sys.argv[1:]
manifest = json.load(open(metadata_path, encoding="utf-8")).get("completenessManifest")
if not isinstance(manifest, dict) or manifest.get("artifact") != name or manifest.get("encryptedSha256") != checksum or manifest.get("encryptedBytes") != int(size):
    raise SystemExit("metadata does not bind the encrypted completeness manifest")
PY

# A controlled first backup must not overwrite an object if timestamp generation collides.
existing_keys="$(aws "${BACKUP_S3_ARGS[@]}" s3api list-objects-v2 --bucket "$TRADEPILOT_BACKUP_S3_BUCKET" --prefix "$object_key" --output json | python3 -c '
import json
import sys

key = sys.argv[1]
for item in json.load(sys.stdin).get("Contents", []):
    if item.get("Key") in {key, f"{key}.metadata.json", f"{key}.completeness.json.age"}:
        print(item["Key"])
' "$object_key")"
[[ -z "$existing_keys" ]] || backup_die "refusing to overwrite an existing backup object"

aws "${BACKUP_S3_ARGS[@]}" s3 cp "$artifact_path" "s3://${TRADEPILOT_BACKUP_S3_BUCKET}/${object_key}" \
  --only-show-errors --metadata "sha256=${checksum}"
aws "${BACKUP_S3_ARGS[@]}" s3 cp "$manifest_path" "s3://${TRADEPILOT_BACKUP_S3_BUCKET}/${manifest_key}" \
  --only-show-errors --metadata "sha256=${manifest_checksum}"
aws "${BACKUP_S3_ARGS[@]}" s3 cp "$metadata_path" "s3://${TRADEPILOT_BACKUP_S3_BUCKET}/${metadata_key}" --only-show-errors

remote_checksum="$(aws "${BACKUP_S3_ARGS[@]}" s3api head-object --bucket "$TRADEPILOT_BACKUP_S3_BUCKET" --key "$object_key" --query 'Metadata.sha256' --output text)"
remote_size="$(aws "${BACKUP_S3_ARGS[@]}" s3api head-object --bucket "$TRADEPILOT_BACKUP_S3_BUCKET" --key "$object_key" --query 'ContentLength' --output text)"
[[ "$remote_checksum" == "$checksum" ]] || backup_die "uploaded artifact checksum metadata mismatch"
[[ "$remote_size" == "$size" ]] || backup_die "uploaded artifact size mismatch"
remote_manifest_checksum="$(aws "${BACKUP_S3_ARGS[@]}" s3api head-object --bucket "$TRADEPILOT_BACKUP_S3_BUCKET" --key "$manifest_key" --query 'Metadata.sha256' --output text)"
remote_manifest_size="$(aws "${BACKUP_S3_ARGS[@]}" s3api head-object --bucket "$TRADEPILOT_BACKUP_S3_BUCKET" --key "$manifest_key" --query 'ContentLength' --output text)"
[[ "$remote_manifest_checksum" == "$manifest_checksum" ]] || backup_die "uploaded completeness manifest checksum metadata mismatch"
[[ "$remote_manifest_size" == "$manifest_size" ]] || backup_die "uploaded completeness manifest size mismatch"

# Retention runs only after this new artifact is encrypted, uploaded, and verified.
retention_mode="${TRADEPILOT_BACKUP_RETENTION_MODE:-enforce}"
case "$retention_mode" in
  enforce) ;;
  disabled)
    printf 'Encrypted backup upload and checksum verification completed with retention deletion disabled: %s\n' "$artifact_name"
    exit 0
    ;;
  *) backup_die "TRADEPILOT_BACKUP_RETENTION_MODE must be enforce or disabled" ;;
esac
retention_days="${TRADEPILOT_BACKUP_RETENTION_DAYS:-21}"
if ! [[ "$retention_days" =~ ^[0-9]+$ ]] || (( retention_days < 14 )); then
  backup_die "retention must be an integer of at least 14 days"
fi
inventory="$(mktemp "${TMPDIR:-/tmp}/tradepilot-backup-objects.XXXXXX")"
selection="$(mktemp "${TMPDIR:-/tmp}/tradepilot-backup-selection.XXXXXX")"
trap 'rm -f "$inventory" "$selection"' EXIT HUP INT TERM
aws "${BACKUP_S3_ARGS[@]}" s3api list-objects-v2 --bucket "$TRADEPILOT_BACKUP_S3_BUCKET" --prefix "${prefix}/" --output json >"$inventory"
python3 "$SCRIPT_DIR/retention.py" --retention-days "$retention_days" --now "$(date -u +%Y-%m-%dT%H:%M:%SZ)" <"$inventory" >"$selection"
while IFS= read -r key; do
  [[ -n "$key" ]] || continue
  aws "${BACKUP_S3_ARGS[@]}" s3api delete-object --bucket "$TRADEPILOT_BACKUP_S3_BUCKET" --key "$key"
  aws "${BACKUP_S3_ARGS[@]}" s3api delete-object --bucket "$TRADEPILOT_BACKUP_S3_BUCKET" --key "${key}.metadata.json"
  aws "${BACKUP_S3_ARGS[@]}" s3api delete-object --bucket "$TRADEPILOT_BACKUP_S3_BUCKET" --key "${key}.completeness.json.age"
done < <(python3 - "$selection" <<'PY'
import json
import sys
for key in json.load(open(sys.argv[1], encoding="utf-8"))["delete"]:
    print(key)
PY
)

printf 'Encrypted backup upload and checksum verification completed: %s\n' "$artifact_name"
