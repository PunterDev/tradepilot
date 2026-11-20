#!/usr/bin/env bash
# Ensures the production-facing connectivity probe cannot run without explicit approval.

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PREFLIGHT_SCRIPT="$ROOT_DIR/scripts/backup/production-connectivity-preflight.sh"

set +e
output="$(bash "$PREFLIGHT_SCRIPT" 2>&1)"
status=$?
set -e

(( status != 0 )) || {
  printf 'Connectivity preflight unexpectedly ran without approval.\n' >&2
  exit 1
}
[[ "$output" == *'TRADEPILOT_PRODUCTION_CONNECTIVITY_PREFLIGHT_APPROVED=yes'* ]] || {
  printf 'Connectivity preflight did not explain the explicit approval gate.\n' >&2
  exit 1
}
printf 'Production connectivity preflight approval guard passed.\n'
