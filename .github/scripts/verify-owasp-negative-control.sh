#!/usr/bin/env bash
# Isolated CI-only negative control: never add this intentionally vulnerable dependency to TradePilot's POM.
set -euo pipefail

if [[ -z "$NVD_API_KEY" ]]; then
  echo "::error::NVD_API_KEY is required for the OWASP negative control."
  exit 1
fi

fixture_dir="$RUNNER_TEMP/tradepilot-owasp-negative-control"
mkdir -p "$fixture_dir"
cat > "$fixture_dir/pom.xml" <<'POM'
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.tradepilot.security-tests</groupId>
  <artifactId>owasp-negative-control-only</artifactId>
  <version>1.0.0</version>
  <dependencies>
    <!-- Test fixture only: deliberately vulnerable, never included in the application. -->
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-core</artifactId>
      <version>2.14.1</version>
    </dependency>
  </dependencies>
</project>
POM

# The real application scan preserves failBuildOnUnusedSuppressionRule=true.
# This fixture cannot use any of the eight approved rules; turning off the unused-rule
# check here ensures the negative-control failure must come from a real vulnerability.
set +e
mvn -B -f "$fixture_dir/pom.xml" org.owasp:dependency-check-maven:check \
  -DfailBuildOnCVSS=8 \
  -DfailBuildOnUnusedSuppressionRule=false \
  -DsuppressionFile="$GITHUB_WORKSPACE/backend/config/dependency-check-suppressions.xml" \
  -DnvdApiKeyEnvironmentVariable=NVD_API_KEY \
  -Dformat=JSON
scan_exit=$?
set -e

if [[ "$scan_exit" -eq 0 ]]; then
  echo "::error::Dependency-Check unexpectedly passed with an unrelated CVSS 10 vulnerability."
  exit 1
fi

python3 - "$fixture_dir/target/dependency-check-report.json" <<'PY'
import json
import pathlib
import sys

report_path = pathlib.Path(sys.argv[1])
if not report_path.is_file():
    sys.exit("FAIL: JSON report was not produced; scanner failure cannot be attributed to a vulnerability.")

data = json.loads(report_path.read_text(encoding="utf-8"))
for dependency in data.get("dependencies", []):
    if "log4j-core-2.14.1" not in dependency.get("fileName", ""):
        continue
    for vulnerability in dependency.get("vulnerabilities", []):
        if vulnerability.get("name") != "CVE-2021-44228":
            continue
        scores = [
            block.get("baseScore")
            for field in ("cvssv4", "cvssv3", "cvssv2")
            if isinstance(block := vulnerability.get(field), dict)
        ]
        if any(isinstance(score, (int, float)) and score >= 8 for score in scores):
            print("PASS: Real Dependency-Check failed and reported unsuppressed CVE-2021-44228 (CVSS >= 8) on isolated log4j-core 2.14.1.")
            sys.exit(0)

sys.exit("FAIL: Expected unrelated CVSS >= 8 CVE was not found on the isolated fixture.")
PY
