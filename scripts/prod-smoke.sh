#!/usr/bin/env bash
set -euo pipefail

web_url="${TRADEPILOT_WEB_URL:-}"
api_url="${TRADEPILOT_API_URL:-}"
if [[ -z "$web_url" || -z "$api_url" ]]; then
  echo 'Set TRADEPILOT_WEB_URL and TRADEPILOT_API_URL before running this read-only smoke test.' >&2
  exit 2
fi
web_url="${web_url%/}"
api_url="${api_url%/}"
if [[ "$api_url" != */api ]]; then
  echo 'TRADEPILOT_API_URL must be the deployed backend API base ending in /api.' >&2
  exit 2
fi
health_url="${api_url%/api}/actuator/health"
failures=0

request_status() {
  curl --silent --show-error --location --output /dev/null --write-out '%{http_code}' --max-time 20 "$@"
}

expect_status() {
  local label="$1" expected="$2"
  shift 2
  local actual
  actual="$(request_status "$@")"
  if [[ "$actual" == "$expected" ]]; then
    printf 'PASS  %s (%s)\n' "$label" "$actual"
  else
    printf 'FAIL  %s (expected %s, got %s)\n' "$label" "$expected" "$actual" >&2
    failures=1
  fi
}

expect_not_server_error() {
  local label="$1"
  shift
  local actual
  actual="$(request_status "$@")"
  if [[ "$actual" =~ ^5 ]]; then
    printf 'FAIL  %s (%s)\n' "$label" "$actual" >&2
    failures=1
  else
    printf 'PASS  %s (%s)\n' "$label" "$actual"
  fi
}

expect_status 'homepage' 200 "$web_url/"
expect_status 'API health' 200 "$health_url"
expect_not_server_error 'login endpoint' -X POST -H 'Content-Type: application/json' --data '{}' "$api_url/auth/login"
expect_not_server_error 'signup endpoint' -X POST -H 'Content-Type: application/json' --data '{}' "$api_url/auth/register"
cors_headers="$(curl --silent --show-error --max-time 20 --output /dev/null --dump-header - -X OPTIONS -H "Origin: $web_url" -H 'Access-Control-Request-Method: POST' "$api_url/auth/login" | tr -d '\r')"
cors_status="$(printf '%s\n' "$cors_headers" | awk 'NR == 1 { print $2 }')"
cors_origin="$(printf '%s\n' "$cors_headers" | awk 'tolower($1) == "access-control-allow-origin:" { print $2; exit }')"
if [[ "$cors_status" == "200" && "$cors_origin" == "$web_url" ]]; then
  printf 'PASS  CORS preflight (%s, origin allowed)\n' "$cors_status"
else
  printf 'FAIL  CORS preflight (status=%s, allowed-origin=%s)\n' "$cors_status" "${cors_origin:-missing}" >&2
  failures=1
fi
expect_not_server_error 'public feature availability' "$api_url/v1/features"
expect_not_server_error 'dev E2E endpoint disabled' -X POST "$api_url/dev/e2e/prepare"

features="$(curl --silent --show-error --max-time 20 "$api_url/v1/features")"
if [[ "$features" == *'"v2FeaturesEnabled":false'* && "$features" == *'"v3FeaturesEnabled":false'* ]]; then
  printf '%s\n' 'PASS  V2/V3 feature flags disabled'
else
  printf '%s\n' 'FAIL  V2/V3 feature flags are not both disabled' >&2
  failures=1
fi

if [[ "$failures" -ne 0 ]]; then
  echo 'FAIL: production smoke checks failed.' >&2
  exit 1
fi
echo 'PASS: production smoke checks passed.'
