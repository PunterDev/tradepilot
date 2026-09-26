# V1 Production Verification

V1 runs with `V2_FEATURES_ENABLED=false`, `V3_FEATURES_ENABLED=false`, and `MARKET_DATA_PROVIDER=disabled`. Qdrant, Databento, AI Pre-Analysis beta configuration, and Trade Replay worker configuration are not required for this release.

## Required Environment

| Category | Variables |
| --- | --- |
| Backend startup | `DB_URL` (or `DATABASE_URL`), `DB_USERNAME` (or `DATABASE_USERNAME`), `DB_PASSWORD` (or `DATABASE_PASSWORD`), `JWT_SECRET`, `APP_ALLOWED_ORIGINS`, `APP_FRONTEND_URL`, `RATE_LIMIT_STORE=database`, `APP_ADMIN_ACCESS_PROTECTION=external-mfa`, `SPRING_PROFILES_ACTIVE=production` |
| Frontend build | `VITE_API_BASE_URL`, `VITE_V2_FEATURES_ENABLED=false`, `VITE_V3_FEATURES_ENABLED=false` |
| Stripe Live | `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRO_MONTHLY_PRICE_ID`, `STRIPE_PRO_YEARLY_PRICE_ID` |
| Email delivery | `RESEND_API_KEY`, `EMAIL_FROM`, `CONTACT_EMAIL` |
| Optional V1 integrations | `OPENAI_API_KEY`, `GSC_*`, observability and rate-limit tuning values |
| V2 only | `QDRANT_*`, `TRADING_MEMORY_*`, `AI_PRE_ANALYSIS_BETA_EMAILS` |
| V3 only | `DATABENTO_*`, `REPLAY_*` |

The backend accepts compatibility aliases `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`; prefer the corresponding `DB_*` names. `STRIPE_PRO_PRICE_ID` is a legacy monthly-price alias and should not be used for a new production setup.

Run without printing secret values:

```bash
scripts/check-prod-env.sh
scripts/check-prod-env.sh --template
```

## Health, Schema, And Database

`GET /actuator/health` is public and returns only aggregate application status. It includes database health internally but never exposes database URLs, credentials, or component details. Flyway executes on normal startup and Hibernate remains `validate`; a successful production startup proves the migration chain and entity schema are compatible.

Before traffic, check Render logs for successful Flyway completion and Hibernate startup, then run:

```bash
TRADEPILOT_WEB_URL=https://www.tradepilotcoach.com \
TRADEPILOT_API_URL=https://<render-service>.onrender.com/api \
scripts/prod-smoke.sh
```

The smoke script is read-only. It checks health, public auth endpoint existence with invalid empty payloads, CORS preflight, V2/V3 feature availability, and disabled dev tooling. It never sends credentials or creates a user.

## Controlled Browser Smoke

Use only a dedicated, verified, non-admin smoke account. This test logs in, visits core routes, verifies V2/V3 access stays hidden, tests the blocked Pre-Analysis route, and logs out. It creates no data.

```bash
cd frontend
RUN_PROD_SMOKE=true \
E2E_BASE_URL=https://www.tradepilotcoach.com \
TRADEPILOT_SMOKE_EMAIL=controlled-account@example.com \
TRADEPILOT_SMOKE_PASSWORD=... \
npx playwright test e2e/production-smoke.spec.ts
```

CSV import is intentionally not automated against production. Perform it manually only with the controlled account and disposable fixture data.

## Stripe And Email

Stripe uses only environment-provided Live secrets and price IDs in production. Startup rejects test keys, missing webhook secrets, or invalid Live price configuration. Configure the webhook endpoint as `https://<render-service>.onrender.com/api/stripe/webhook` and select the events in [STRIPE_LIVE_WEBHOOK_EVENTS.md](./STRIPE_LIVE_WEBHOOK_EVENTS.md). Webhook logs identify received, duplicate, successful, and failed events without logging secrets, signatures, or card data.

Resend is server-only. `APP_FRONTEND_URL` controls verification and password-reset links and production startup rejects localhost values. Send one verification and one password-reset message to the controlled inbox after deployment and confirm their links use `https://www.tradepilotcoach.com`.

## Full Repository Gate

Run the non-deploying release gate from the repository root:

```bash
scripts/v1-release-check.sh
```

It requires a clean `master` worktree, validates templates, runs V1 backend tests and package, builds the frontend, and runs public frontend regression. It runs production smoke only when both production URLs are supplied. It never tags, deploys, changes infrastructure, or initiates payments.
