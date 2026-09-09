# TradePilot Selenium E2E Tests

Java 17, Maven, JUnit 5, Selenium 4, Chrome, and Selenium Manager based browser tests for the real TradePilot React/Spring Boot application.

## Project Location

This project belongs at:

```bash
e2e-tests
```

It should sit beside `backend` and `frontend` in the TradePilot repo.

## Deterministic Test Data

Regular Selenium tests prepare synthetic local-only data automatically through the guarded dev endpoint:

```text
POST /api/dev/e2e/prepare
```

The endpoint creates or resets only these clearly synthetic users:

```text
selenium-e2e@tradepilot.test
selenium-free-e2e@tradepilot.test
```

The Pro user receives a deterministic `Primary E2E Account`, `Secondary E2E Account`, and known futures trades. The Free user is also reset to a predictable Free-plan state. The endpoint returns generated database IDs for the synthetic users, primary account, and seeded trade, so Selenium never hard-codes numeric IDs and no longer needs developer-owned users or a manually supplied trade ID.

This requires a local or QA backend with both dev auth and dev tools enabled:

```bash
DEV_AUTH_ENABLED=true DEV_TOOLS_ENABLED=true
```

Never enable those flags in production. Never use real customer credentials.

## Start TradePilot Locally

Backend:

```bash
cd backend
DEV_AUTH_ENABLED=true DEV_TOOLS_ENABLED=true mvn spring-boot:run
```

The backend defaults to `http://localhost:8080` and requires its normal local database/JWT environment variables. See `docs/ENVIRONMENT_VARIABLES.md`.

Frontend:

```bash
cd frontend
VITE_API_BASE_URL=http://localhost:8080/api npm run dev -- --host 127.0.0.1 --port 5173
```

The frontend runs at `http://localhost:5173`.

## Seed Or Prepare Test Data

Use a disposable local or QA database. The Selenium suite calls the E2E prepare endpoint before browser tests begin. Re-running the suite is idempotent: existing synthetic E2E data is cleared and recreated, while unrelated developer data is left alone.

## Run Tests

Compile only:

```bash
cd e2e-tests
mvn test-compile
```

Run the regular deterministic suite:

```bash
cd e2e-tests
mvn test -Dtradepilot.baseUrl=http://localhost:5173 -Dtradepilot.apiBaseUrl=http://localhost:8080/api -Dheadless=true
```

Run one test class:

```bash
mvn test -Dtest=LoginTest -Dtradepilot.baseUrl=http://localhost:5173 -Dheadless=true
```

Run against QA:

```bash
mvn test -Dtradepilot.baseUrl=https://qa.example.test -Dtradepilot.apiBaseUrl=https://qa.example.test/api -Dheadless=true
```

Run staging AI/Qdrant Trading Memory smoke tests:

```bash
mvn test -Pai-integration -Dtradepilot.baseUrl=https://qa.example.test -Dtradepilot.apiBaseUrl=https://qa.example.test/api -Dheadless=true
```

## Convenient Runner

From the TradePilot repo root:

```bash
./e2e-tests/scripts/run-e2e.sh
```

The runner checks Java, Maven, npm, and curl; starts backend/frontend only if their configured URLs are not already responding; prepares deterministic E2E data; waits for readiness; runs Maven; and stops only processes it started.

Optional runner variables:

```bash
TRADEPILOT_BACKEND_URL=http://localhost:8080
TRADEPILOT_FRONTEND_URL=http://localhost:5173
TRADEPILOT_BASE_URL=http://localhost:5173
E2E_API_URL=http://localhost:8080/api
HEADLESS=true
```

## Screenshots And Diagnostics

Failure screenshots and short sanitized failure notes are written to:

```bash
e2e-tests/test-output
```

The runner also writes backend/frontend logs there for processes it starts.

## Production Safety

The deterministic setup endpoint is available only when dev tooling is enabled, and production config rejects dev auth/dev tools. Mutating tests also reject production TradePilot URLs. Production smoke mode must be read-only and tagged separately:

```bash
mvn test -Pproduction-smoke -Dtradepilot.baseUrl=https://app.tradepilot.app -DproductionSmoke=true
```

No production smoke tests are currently implemented.

## Resetting Test Data

The E2E prepare endpoint resets only synthetic Selenium users and their owned accounts/trades/generated data. Do not point mutating Selenium tests at production PostgreSQL, Qdrant, Stripe, or OpenAI-backed customer data.
