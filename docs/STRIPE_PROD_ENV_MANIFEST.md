# Stripe Production Environment Manifest

This manifest lists actual configuration names used by the backend and frontend. Never place backend secrets in frontend `VITE_*` variables.

## Render Backend

| Variable | Required | Secret | Expected format | Production validator checks it |
| --- | --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Required | Non-secret | `production` or `prod` | Indirectly enables production validators. |
| `DB_URL` or `DATABASE_URL` | Required | Sensitive config | PostgreSQL JDBC URL | No. Required by Spring datasource. |
| `DB_USERNAME` or `DATABASE_USERNAME` | Required | Secret | Database username | No. Required by Spring datasource. |
| `DB_PASSWORD` or `DATABASE_PASSWORD` | Required | Secret | Database password | No. Required by Spring datasource. |
| `DB_DRIVER` | Optional | Non-secret | `org.postgresql.Driver` | No. Defaults to PostgreSQL driver. |
| `JPA_DDL_AUTO` | Optional | Non-secret | `validate` | No. Keep `validate` in production. |
| `SERVER_PORT` | Optional | Non-secret | Render-provided port | No. |
| `APP_ALLOWED_ORIGINS` | Required | Non-secret | Comma-separated deployed frontend origins, no wildcard, no localhost | Yes. `CorsConfig` rejects empty, wildcard with credentials, and localhost/127.0.0.1 in production. |
| `APP_FRONTEND_URL` | Required | Non-secret | Deployed frontend origin, no trailing path required | Yes. `ProductionUrlConfig` rejects missing, localhost, and 127.0.0.1 in production. |
| `FRONTEND_URL` | Optional fallback | Non-secret | Frontend origin | No. Do not rely on this in production; set `APP_FRONTEND_URL`. |
| `JWT_SECRET` | Required | Secret | Base64 with at least 32 random bytes | Yes. JWT services reject missing, non-Base64, or weak values. |
| `JWT_EXPIRATION_MINUTES` | Optional | Non-secret | Integer minutes | No. Defaults to 30. |
| `JWT_EXPIRATION_MS` | Optional | Non-secret | Integer milliseconds | No. Defaults to 1800000. |
| `STRIPE_SECRET_KEY` | Required | Secret | Live Stripe secret key, `sk_live_...` | Yes. Rejects missing, `sk_test_`, non-live prefix, or obvious test-looking values. |
| `STRIPE_WEBHOOK_SECRET` | Required | Secret | Live webhook signing secret, `whsec_...` | Yes. Rejects missing, non-`whsec_`, or obvious test-looking values. |
| `STRIPE_PRO_MONTHLY_PRICE_ID` | Required | Non-secret | Live Stripe price ID, `price_...` | Yes. Rejects missing, non-`price_`, or obvious test-looking values. |
| `STRIPE_PRO_PRICE_ID` | Optional legacy fallback | Non-secret | Monthly price ID if legacy fallback is used | No direct validator target. Do not rely on this in production. |
| `STRIPE_PRO_YEARLY_PRICE_ID` | Required | Non-secret | Live Stripe price ID, `price_...` | Yes. Rejects missing, non-`price_`, or obvious test-looking values. |
| `OPENAI_API_KEY` | Optional for billing | Secret | OpenAI API key | No. Required only for AI features. |
| `OPENAI_MODEL` | Optional | Non-secret | Model name | No. |
| `RESEND_API_KEY` | Optional for billing | Secret | Resend API key | No. Required for production email delivery. |
| `EMAIL_FROM` | Optional for billing | Non-secret | Verified email sender | No. Required for production email delivery. |
| `DEV_AUTH_ENABLED` | Optional | Non-secret | `false` | No. Must stay false in production. |
| `DEV_TOOLS_ENABLED` | Optional | Non-secret | `false` | No. Must stay false in production. |
| `V2_FEATURES_ENABLED` | Optional | Non-secret | `true` or `false` | No. Keep aligned with frontend release flags. |
| `V3_FEATURES_ENABLED` | Optional | Non-secret | `true` or `false` | No. Keep aligned with frontend release flags. |
| `OBSERVABILITY_RETENTION_STRIPE_EVENTS_DAYS` | Optional | Non-secret | Integer days | No. Defaults to 180. |

## Vercel Frontend

| Variable | Required | Secret | Expected format | Production validator checks it |
| --- | --- | --- | --- | --- |
| `VITE_API_BASE_URL` | Required | Non-secret | Deployed backend API base, for example `https://api.example.com/api` | Frontend runtime throws during production builds if missing. |
| `VITE_V2_FEATURES_ENABLED` | Optional | Non-secret | `true` or `false` | No. Parsed by frontend feature flag config. |
| `VITE_V3_FEATURES_ENABLED` | Optional | Non-secret | `true` or `false` | No. Parsed by frontend feature flag config. |
| `VITE_IDLE_TIMEOUT_MINUTES` | Optional | Non-secret | Integer minutes | No. Defaults in frontend. |
| `VITE_IDLE_WARNING_MINUTES` | Optional | Non-secret | Integer minutes | No. Defaults in frontend. |

## Safety Notes

- There is no frontend Stripe publishable key in the current implementation; Checkout and Customer Portal sessions are created by the backend.
- Backend production startup should fail before serving traffic if Stripe secret, webhook secret, monthly price, yearly price, `APP_FRONTEND_URL`, CORS origins, or JWT secret are unsafe.
- Production defaults in code include localhost for development. Production validators reject localhost when the `production` or `prod` profile is active.
