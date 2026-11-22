#!/usr/bin/env bash
# Fails when the most recent verified encrypted object is older than 36 hours.

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

backup_require_command aws
backup_require_command python3
backup_require_var TRADEPILOT_BACKUP_S3_BUCKET
backup_s3_args
prefix="$(backup_object_prefix)"

aws "${BACKUP_S3_ARGS[@]}" s3api list-objects-v2 --bucket "$TRADEPILOT_BACKUP_S3_BUCKET" --prefix "${prefix}/" --output json |
python3 -c '
import datetime as dt
import json
import sys

objects = [item for item in json.load(sys.stdin).get("Contents", []) if item.get("Key", "").endswith(".dump.age")]
if not objects:
    raise SystemExit("no encrypted backup object is available")
latest = max(objects, key=lambda item: item["LastModified"])
timestamp = dt.datetime.fromisoformat(latest["LastModified"].replace("Z", "+00:00"))
age = dt.datetime.now(dt.timezone.utc) - timestamp
if age > dt.timedelta(hours=36):
    raise SystemExit(f"newest encrypted backup is stale: {age}")
print(f"backup freshness check passed: newest encrypted object is {age} old")
'
