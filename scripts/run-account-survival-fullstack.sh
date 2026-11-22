#!/usr/bin/env bash
set -euo pipefail

# Runs the real-browser Account Survival acceptance journey against an
# ephemeral PostgreSQL 16 database. No production configuration is used.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTAINER="tradepilot-account-survival-e2e-$$"
BACKEND_PID=""

cleanup() {
  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID" 2>/dev/null || true
    wait "$BACKEND_PID" 2>/dev/null || true
  fi
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run --detach --rm --name "$CONTAINER" \
  --publish 127.0.0.1::5432 \
  --env POSTGRES_DB=tradepilot_acceptance \
  --env POSTGRES_USER=tradepilot \
  --env POSTGRES_PASSWORD=tradepilot \
  postgres:16-alpine >/dev/null

for _ in $(seq 1 30); do
  if docker exec "$CONTAINER" pg_isready --username=tradepilot --dbname=tradepilot_acceptance >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
docker exec "$CONTAINER" pg_isready --username=tradepilot --dbname=tradepilot_acceptance >/dev/null

POSTGRES_PORT="$(docker port "$CONTAINER" 5432/tcp | awk -F: '{print $2}')"
LOG_FILE="${TMPDIR:-/tmp}/tradepilot-account-survival-e2e-$$.log"

(
  cd "$ROOT/backend"
  DB_URL="jdbc:postgresql://127.0.0.1:${POSTGRES_PORT}/tradepilot_acceptance" \
  DB_USERNAME=tradepilot \
  DB_PASSWORD=tradepilot \
  DB_DRIVER=org.postgresql.Driver \
  JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY= \
  DEV_AUTH_ENABLED=true \
  DEV_TOOLS_ENABLED=true \
  SPRING_PROFILES_ACTIVE=e2e \
  MARKET_DATA_PROVIDER=disabled \
  TRADING_MEMORY_ENABLED=false \
  GSC_ENABLED=false \
  FLYWAY_ENABLED=true \
  JPA_DDL_AUTO=validate \
  SERVER_PORT=18080 \
  APP_ALLOWED_ORIGINS=http://127.0.0.1:5175,http://localhost:5175 \
  mvn -Dmaven.test.skip=true spring-boot:run
) >"$LOG_FILE" 2>&1 &
BACKEND_PID=$!

for _ in $(seq 1 90); do
  if curl --silent --fail http://127.0.0.1:18080/actuator/health >/dev/null; then
    break
  fi
  sleep 1
done
curl --silent --fail http://127.0.0.1:18080/actuator/health >/dev/null || {
  cat "$LOG_FILE" >&2
  exit 1
}

cd "$ROOT/frontend"
E2E_API_URL=http://127.0.0.1:18080/api \
E2E_FRONTEND_PORT=5175 \
npx playwright test e2e/account-survival-real-backend.spec.ts --reporter=line --workers=1
