#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
E2E_DIR="$ROOT_DIR/e2e-tests"
BACKEND_URL="${TRADEPILOT_BACKEND_URL:-http://localhost:8080}"
FRONTEND_URL="${TRADEPILOT_FRONTEND_URL:-http://localhost:5173}"
BASE_URL="${TRADEPILOT_BASE_URL:-$FRONTEND_URL}"
HEADLESS="${HEADLESS:-true}"

BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
  if [[ -n "$FRONTEND_PID" ]]; then
    kill "$FRONTEND_PID" 2>/dev/null || true
  fi
  if [[ -n "$BACKEND_PID" ]]; then
    kill "$BACKEND_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required tool: $1" >&2
    exit 2
  }
}

wait_for_url() {
  local url="$1"
  local label="$2"
  local attempts="${3:-60}"
  for _ in $(seq 1 "$attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for $label at $url" >&2
  exit 3
}

require_cmd java
require_cmd mvn
require_cmd npm
require_cmd curl

mkdir -p "$E2E_DIR/test-output"

if [[ -z "${TRADEPILOT_TEST_EMAIL:-}" || -z "${TRADEPILOT_TEST_PASSWORD:-}" ]]; then
  echo "Missing TRADEPILOT_TEST_EMAIL or TRADEPILOT_TEST_PASSWORD." >&2
  exit 2
fi

if ! curl -fsS "$BACKEND_URL/api/v1/features" >/dev/null 2>&1; then
  echo "Starting backend on $BACKEND_URL"
  (
    cd "$ROOT_DIR/backend"
    mvn spring-boot:run
  ) > "$E2E_DIR/test-output/backend.log" 2>&1 &
  BACKEND_PID="$!"
fi
wait_for_url "$BACKEND_URL/api/v1/features" "backend"

if ! curl -fsS "$FRONTEND_URL" >/dev/null 2>&1; then
  echo "Starting frontend on $FRONTEND_URL"
  (
    cd "$ROOT_DIR/frontend"
    VITE_API_BASE_URL="$BACKEND_URL/api" npm run dev -- --host 127.0.0.1 --port "${FRONTEND_URL##*:}"
  ) > "$E2E_DIR/test-output/frontend.log" 2>&1 &
  FRONTEND_PID="$!"
fi
wait_for_url "$FRONTEND_URL" "frontend"

cd "$E2E_DIR"
mvn test -Dtradepilot.baseUrl="$BASE_URL" -Dheadless="$HEADLESS" "$@"
