#!/usr/bin/env bash
# Secretless scan of the exact commit which the protected approval job approved.
set -Eeuo pipefail

target_file=${1:?approved target manifest is required}
bundle_root=${2:?NVD bundle directory is required}
root=$(cd "$(dirname "$0")/../.." && pwd)
candidate="$root/docs/security/candidates/dependency-check-expanded-sixteen-dec15-candidate.xml"

test -z "${NVD_API_KEY:-}"
test -z "${TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN:-}"
for required in git mvn python3 sha256sum tar; do
  command -v "$required" >/dev/null || { echo "required command unavailable: $required" >&2; exit 1; }
done
python3 "$root/scripts/circleci/verify_trusted_nvd_manifest.py" \
  --manifest "$bundle_root/manifest.json" --archive "$bundle_root/dependency-check-data.tar.gz"
test -r "$target_file"
target_sha=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["targetSha"])' "$target_file")
candidate_sha=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["candidateSha256"])' "$target_file")
control_sha=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["trustedControlSha"])' "$target_file")
test "$(sha256sum "$candidate" | awk '{print $1}')" = "$candidate_sha"

work=$(mktemp -d)
cleanup() { rm -rf "$work"; }
trap cleanup EXIT
cp "$candidate" "$work/trusted-suppression.xml"
tar -C "$work" -xzf "$bundle_root/dependency-check-data.tar.gz"
test -s "$work/data/odc.mv.db"
git fetch --no-tags origin "$target_sha"
git checkout --detach "$target_sha"
mvn -q -f backend/pom.xml org.owasp:dependency-check-maven:12.2.2:check \
  -DautoUpdate=false -DdataDirectory="$work/data" -DsuppressionFiles="$work/trusted-suppression.xml" \
  -Dformats=JSON -DoutputDirectory="$work/report" \
  -DfailOnError=true -DfailBuildOnCVSS=8 -DfailBuildOnUnusedSuppressionRule=true
report="$work/report/dependency-check-report.json"
test -s "$report"
cp "$report" /tmp/analysis-report.json
report_sha=$(sha256sum "$report" | awk '{print $1}')
bundle_manifest_sha=$(sha256sum "$bundle_root/manifest.json" | awk '{print $1}')
python3 - "$report" "$target_sha" "$candidate_sha" "$control_sha" "$bundle_manifest_sha" "$report_sha" >/tmp/analysis-result.json <<'PY'
import json
import sys
from pathlib import Path

report = json.loads(Path(sys.argv[1]).read_text())
dependencies = report.get("dependencies")
if not isinstance(dependencies, list) or not dependencies:
    raise SystemExit("Dependency-Check report has no analyzed dependencies")
print(json.dumps({
    "schemaVersion": 1,
    "status": "PASS",
    "targetSha": sys.argv[2],
    "candidateSha256": sys.argv[3],
    "trustedControlSha": sys.argv[4],
    "nvdManifestSha256": sys.argv[5],
    "reportSha256": sys.argv[6],
    "scannerVersion": "12.2.2",
}, sort_keys=True))
PY
