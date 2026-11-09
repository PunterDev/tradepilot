#!/usr/bin/env bash
# Runs only from protected master code; it never checks out a PR revision.
set -Eeuo pipefail

bundle_root=${1:?bundle output directory is required}
scanner_version=12.2.2

test "${CIRCLE_BRANCH:-}" = master
test -z "${CIRCLE_SSH_CONNECTION:-}"
test -n "${NVD_API_KEY:-}"
test "${CIRCLE_PROJECT_USERNAME:-}" = PunterDev
test "${CIRCLE_PROJECT_REPONAME:-}" = tradepilot
test "${CIRCLE_CONFIG_SOURCE:-}" = repository
[[ "${CIRCLE_SHA1:-}" =~ ^[0-9a-f]{40}$ ]] || { echo "invalid protected control SHA" >&2; exit 1; }
test "$(git rev-parse HEAD)" = "$CIRCLE_SHA1"
for required in awk git grep mktemp mvn python3 sha256sum tar; do
  command -v "$required" >/dev/null || { echo "required command unavailable: $required" >&2; exit 1; }
done

work=$(mktemp -d)
cleanup() { rm -rf "$work"; }
trap cleanup EXIT
mkdir -p "$bundle_root"
cat >"$work/pom.xml" <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>local</groupId><artifactId>trusted-nvd-refresh</artifactId><version>1</version><build><plugins><plugin><groupId>org.owasp</groupId><artifactId>dependency-check-maven</artifactId><version>12.2.2</version></plugin></plugins></build></project>
EOF
mvn -q -f "$work/pom.xml" org.owasp:dependency-check-maven:"$scanner_version":update-only \
  -DdataDirectory="$work/data" -DnvdApiKeyEnvironmentVariable=NVD_API_KEY \
  -DnvdApiDelay=6000 -DnvdMaxRetryCount=10 -DconnectionTimeout=30000 -DreadTimeout=30000 \
  >"$work/update.log" 2>&1
if grep -Eqi 'unable to import|database update failed|fatal.*nvd' "$work/update.log"; then
  echo "NVD ingestion did not complete" >&2
  exit 1
fi
test -s "$work/data/odc.mv.db"
tar -C "$work" -czf "$bundle_root/dependency-check-data.tar.gz" data
archive_sha=$(sha256sum "$bundle_root/dependency-check-data.tar.gz" | awk '{print $1}')
created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
printf '{"archiveSha256":"%s","controlCommit":"%s","createdAt":"%s","repository":"PunterDev/tradepilot","scannerVersion":"%s"}\n' \
  "$archive_sha" "$CIRCLE_SHA1" "$created_at" "$scanner_version" >"$bundle_root/manifest.json"
