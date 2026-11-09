#!/usr/bin/env bash
# Secretless scan of the exact commit which the protected approval job approved.
set -Eeuo pipefail

target_file=${1:?approved target manifest is required}
bundle_root=${2:?NVD bundle directory is required}
root=$(cd "$(dirname "$0")/../.." && pwd)
candidate="$root/docs/security/candidates/dependency-check-expanded-sixteen-dec15-candidate.xml"

test -z "${NVD_API_KEY:-}"
test -z "${TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN:-}"
python3 "$root/scripts/circleci/verify_trusted_nvd_manifest.py" \
  --manifest "$bundle_root/manifest.json" --archive "$bundle_root/dependency-check-data.tar.gz"
test -r "$target_file"
target_sha=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["targetSha"])' "$target_file")
candidate_sha=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["candidateSha256"])' "$target_file")
test "$(shasum -a 256 "$candidate" | awk '{print $1}')" = "$candidate_sha"

work=$(mktemp -d)
cleanup() { rm -rf "$work"; }
trap cleanup EXIT
cp "$candidate" "$work/trusted-suppression.xml"
tar -C "$work" -xzf "$bundle_root/dependency-check-data.tar.gz"
test -s "$work/data/odc.mv.db"
git fetch --no-tags origin "$target_sha"
git checkout --detach "$target_sha"
mvn -q -f backend/pom.xml org.owasp:dependency-check-maven:12.2.2:check \
  -DautoUpdate=false -DdataDirectory="$work/data" -DsuppressionFile="$work/trusted-suppression.xml" \
  -DfailOnError=true -DfailBuildOnCVSS=8 -DfailBuildOnUnusedSuppressionRule=true
cp "$target_file" /tmp/analysis-result.json
