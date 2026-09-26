#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"
failures=0

run() {
  local label="$1"
  shift
  if "$@"; then
    printf 'PASS  %s\n' "$label"
  else
    printf 'FAIL  %s\n' "$label" >&2
    failures=1
  fi
}

run 'master branch' test "$(git branch --show-current)" = master
run 'clean working tree' git diff --quiet
run 'clean staged tree' git diff --cached --quiet
run 'clean diff whitespace' git diff --check
run 'V1 environment templates' scripts/check-prod-env.sh --template
run 'backend test suite' bash -lc 'cd backend && mvn clean test'
run 'backend package' bash -lc 'cd backend && mvn package -DskipTests'
run 'frontend build' bash -lc 'cd frontend && npm run build'
run 'public frontend regression' bash -lc 'cd frontend && npx playwright test e2e/homepage-marketing.spec.ts e2e/public-branding.spec.ts e2e/seo-landing-pages.spec.ts --reporter=line'

if [[ -n "${TRADEPILOT_WEB_URL:-}" || -n "${TRADEPILOT_API_URL:-}" ]]; then
  run 'production smoke' scripts/prod-smoke.sh
else
  printf '%s\n' 'SKIPPED  production smoke (set TRADEPILOT_WEB_URL and TRADEPILOT_API_URL to enable)'
fi

if [[ "$failures" -ne 0 ]]; then
  echo 'FAIL: V1 release checks failed.' >&2
  exit 1
fi
echo 'PASS: V1 release checks passed.'
