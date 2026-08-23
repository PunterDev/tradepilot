# TradePilot Selenium E2E Tests

Java 17, Maven, JUnit 5, Selenium 4, Chrome, and Selenium Manager based browser tests for the real TradePilot React/Spring Boot application.

## Project Location

This project belongs at:

```bash
e2e-tests
```

It should sit beside `backend` and `frontend` in the TradePilot repo.

## Required Configuration

Regular tests require local or QA users that already exist and are email-verified:

```bash
export TRADEPILOT_TEST_EMAIL="pro-test-user@example.test"
export TRADEPILOT_TEST_PASSWORD="replace-me"
export TRADEPILOT_FREE_TEST_EMAIL="free-test-user@example.test"
export TRADEPILOT_FREE_TEST_PASSWORD="replace-me"
```

Trade detail and Trading Memory tests also require a known trade owned by `TRADEPILOT_TEST_EMAIL`:

```bash
mvn test -Dtradepilot.testTradeId=123
```

Never use real customer credentials. Never commit `.env` files.

## Start TradePilot Locally

Backend:

```bash
cd backend
mvn spring-boot:run
```

The backend defaults to `http://localhost:8080` and requires its normal local database/JWT environment variables. See `docs/ENVIRONMENT_VARIABLES.md`.

Frontend:

```bash
cd frontend
VITE_API_BASE_URL=http://localhost:8080/api npm run dev -- --host 127.0.0.1 --port 5173
```

The frontend runs at `http://localhost:5173`.

## Seed Or Prepare Test Data

Use a disposable local or QA database. The regular Selenium suite expects:

- a Pro test user with at least one trading account
- a Free test user
- a known seeded trade ID for `TradeDetailTest`

If dev tools are enabled locally, you can log in and use the app's Dev Tools page to seed demo data. This repo also exposes dev endpoints only when `DEV_TOOLS_ENABLED=true`; do not enable them in production.

## Run Tests

Compile only:

```bash
cd e2e-tests
mvn test-compile
```

Run the regular deterministic suite:

```bash
cd e2e-tests
mvn test -Dtradepilot.baseUrl=http://localhost:5173 -Dheadless=true -Dtradepilot.testTradeId=123
```

Run one test class:

```bash
mvn test -Dtest=LoginTest -Dtradepilot.baseUrl=http://localhost:5173 -Dheadless=true
```

Run against QA:

```bash
mvn test -Dtradepilot.baseUrl=https://qa.example.test -Dheadless=true -Dtradepilot.testTradeId=123
```

Run staging AI/Qdrant Trading Memory smoke tests:

```bash
mvn test -Pai-integration -Dtradepilot.baseUrl=https://qa.example.test -Dheadless=true -Dtradepilot.testTradeId=123
```

## Convenient Runner

From the TradePilot repo root:

```bash
./e2e-tests/scripts/run-e2e.sh -Dtradepilot.testTradeId=123
```

The runner checks Java, Maven, npm, and curl; starts backend/frontend only if their configured URLs are not already responding; waits for readiness; runs Maven; and stops only processes it started.

Optional runner variables:

```bash
TRADEPILOT_BACKEND_URL=http://localhost:8080
TRADEPILOT_FRONTEND_URL=http://localhost:5173
TRADEPILOT_BASE_URL=http://localhost:5173
HEADLESS=true
```

## Screenshots And Diagnostics

Failure screenshots and short sanitized failure notes are written to:

```bash
e2e-tests/test-output
```

The runner also writes backend/frontend logs there for processes it starts.

## Production Safety

Mutating tests reject production TradePilot URLs. Production smoke mode must be read-only and tagged separately:

```bash
mvn test -Pproduction-smoke -Dtradepilot.baseUrl=https://app.tradepilot.app -DproductionSmoke=true
```

No production smoke tests are currently implemented.

## Resetting Test Data

Use the app's documented local/dev reset flow or a disposable database/schema. Do not point mutating Selenium tests at production PostgreSQL, Qdrant, Stripe, or OpenAI-backed customer data.
