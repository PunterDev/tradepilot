# TradePilot Production Secret Readiness

Audit date: 2026-08-29

Scope: tracked repository files only. No external services were accessed, no credentials were tested, and no production systems were touched.

## Summary

The tracked repository contains no usable hardcoded service credentials. Development configuration should be supplied through local ignored files or environment variables, using `backend/.env.example` as the template.

Production readiness is not blocked by repository credential exposure; production configuration remains an external deployment responsibility.

## Search Coverage

Tracked files were searched for these credential-like terms and patterns:

- `password`, `secret`, `token`, `api-key`, `apikey`
- `OPENAI`, `STRIPE`, `RESEND`, `DATABENTO`, `QDRANT`, `SUPABASE`, `JWT`
- `DATABASE_URL`, `DB_PASSWORD`
- `sk_test_`, `sk_live_`, `rk_`, `whsec_`, `postgresql://`, `Bearer`

Inspected configuration surfaces:

- Spring `application.yml`
- Spring `application-*.yml`
- test configuration
- backend and frontend `.env.example`
- Dockerfile
- GitHub Actions
- Vercel config
- frontend Vite and Playwright config

## Findings

| File | Classification | Finding | Action |
| --- | --- | --- | --- |
| `backend/src/main/resources/application-dev.yml` | `LOCAL_IGNORED_CONFIG` | Ignored local development configuration. It is not tracked and is absent from reachable Git history. | Excluded from repository credential findings. Keep local configuration ignored. |
| `backend/src/main/resources/application.yml` | `SAFE_PLACEHOLDER` | Production-facing config uses environment-variable references for database, JWT, OpenAI, Resend, Databento, Qdrant, Stripe, CORS, and frontend URL. Stripe and integration keys default to blank where optional/nonconfigured behavior is supported. | No secret values committed. |
| `backend/src/test/resources/application-test.yml` | `TEST_FIXTURE` | H2 test database and explicit fake JWT secret. | Acceptable test fixture. Do not reuse outside tests. |
| `backend/.env.example` | `SAFE_PLACEHOLDER` | Example env manifest contains placeholder values only. | Updated password/JWT placeholders to more obviously non-secret text. |
| `frontend/.env.example` | `SAFE_PLACEHOLDER` | Frontend example contains localhost development API URL and feature flags. | No frontend secrets present. Configure production URL in Vercel. |
| `.github/workflows/postgres-flyway-baseline.yml` | `SAFE_PLACEHOLDER` | CI uses no checked-in secrets for tests. | No action required. |
| `backend/tradepilot_backend_dockerfile.Dockerfile` | `SAFE_PLACEHOLDER` | No embedded credentials. Runtime env is expected from deployment platform. | No action required. |
| `frontend/vercel.json`, `frontend/vite.config.ts`, `frontend/playwright.config.ts` | `SAFE_PLACEHOLDER` | No embedded credentials. Localhost defaults are development/test only. | No action required. |
| `backend/src/test/java/**` | `TEST_FIXTURE` | Stripe-shaped values, JWT values, malformed Bearer tokens, and Databento test values are explicit test fixtures. | Acceptable. They are non-production tests and use redacted/fake values. |
| `docs/**` | `SAFE_PLACEHOLDER` / `UNKNOWN` | Docs reference env names and Stripe key shapes. No real values were printed by this audit. Historical audit docs warn about webhook secret limitations. | No action required. |

## Production Startup Secret Enforcement

Verified startup/config behavior:

- `JWT_SECRET` is required by `JwtService` and `JwtTokenProvider`; missing, non-Base64, or shorter-than-32-byte decoded secrets fail startup.
- `STRIPE_SECRET_KEY` is required in `prod`/`production`; test-mode and non-live-looking values fail startup.
- `STRIPE_WEBHOOK_SECRET` is required in `prod`/`production`; missing, non-`whsec_`, or obvious test-looking values fail startup.
- `STRIPE_PRO_MONTHLY_PRICE_ID` and `STRIPE_PRO_YEARLY_PRICE_ID` are required in `prod`/`production`; missing or obvious test-looking values fail startup.
- `APP_FRONTEND_URL` must be non-localhost in `prod`/`production`.
- `DB_URL`/`DATABASE_URL`, `DB_USERNAME`/`DATABASE_USERNAME`, and `DB_PASSWORD`/`DATABASE_PASSWORD` have no usable production fallback in `application.yml`; missing database settings prevent the application from connecting/validating.

Optional integration behavior:

- `OPENAI_API_KEY` defaults blank; AI calls fail safely when the key is missing.
- `RESEND_API_KEY` defaults blank; email client logs that Resend is not configured and does not expose a key.
- `DATABENTO_API_KEY` defaults blank; market data requests fail as provider-not-configured unless a key is supplied.
- `QDRANT_API_KEY` and `QDRANT_URL` default blank; Trading Memory startup validation logs a sanitized warning and does not block application startup.

## Git Ignore Review

`.gitignore` protects local secret-bearing files:

- `.env`
- `.env.*`
- `*.env`
- `application.env`
- `backend/application.env`
- `backend/application-local.yml`
- `backend/application-local.properties`
- `backend/src/main/resources/application-dev.yml`

Important: ignore rules do not protect files once already tracked. `backend/src/main/resources/application-dev.yml` is ignored and is not currently tracked; keep it that way.

## Rotation Decision

No credential rotation or repository-history cleanup is required from this repository audit: `application-dev.yml` is ignored, untracked, and absent from reachable Git history, and no real provider credential was found in tracked files or reachable history. Rotate a credential only if it was separately shared or exposed outside this repository.

## Production Environment Checklist

Required backend secrets:

- `DB_URL` or `DATABASE_URL`
- `DB_USERNAME` or `DATABASE_USERNAME`
- `DB_PASSWORD` or `DATABASE_PASSWORD`
- `JWT_SECRET`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `STRIPE_PRO_MONTHLY_PRICE_ID`
- `STRIPE_PRO_YEARLY_PRICE_ID`

Required backend non-secrets:

- `APP_FRONTEND_URL`
- `APP_ALLOWED_ORIGINS`
- `JPA_DDL_AUTO=validate`
- `FLYWAY_ENABLED=true`
- `DEV_AUTH_ENABLED=false`
- `DEV_TOOLS_ENABLED=false`

Optional backend secrets:

- `OPENAI_API_KEY`
- `RESEND_API_KEY`
- `DATABENTO_API_KEY`
- `QDRANT_API_KEY`

Required frontend production value:

- `VITE_API_BASE_URL` set to the deployed backend API URL.

## Residual Risk

No remaining tracked file contains a verified production secret, but historical exposure means rotation is mandatory. Local ignored build output may still contain old compiled resources until cleaned; it is not tracked, but developers should run `mvn clean` or delete `backend/target` after pulling this cleanup.
