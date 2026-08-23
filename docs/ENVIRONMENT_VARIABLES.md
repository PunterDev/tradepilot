# TradePilot Environment Variables

Production secrets belong in the deployment platform, never in git.

## Backend

Required:

- `DB_URL`: Supabase JDBC URL, including SSL mode.
- `DB_USERNAME`: Supabase database username.
- `DB_PASSWORD`: Supabase database password.
- `JWT_SECRET`: Base64 secret with at least 32 random bytes.
- `JWT_EXPIRATION_MS`: JWT lifetime in milliseconds, for example `1800000`.
- `OPENAI_API_KEY`: OpenAI API key.
- `STRIPE_SECRET_KEY`: Stripe secret key.
- `STRIPE_WEBHOOK_SECRET`: Stripe webhook signing secret.
- `STRIPE_PRO_MONTHLY_PRICE_ID`: Stripe monthly Pro price ID.
- `STRIPE_PRO_YEARLY_PRICE_ID`: Stripe yearly Pro price ID.
- `RESEND_API_KEY`: Resend API key for transactional email delivery.
- `EMAIL_FROM`: Verified Resend sender, for example `TradePilot <no-reply@tradepilot.app>`.
- `CONTACT_EMAIL`: Destination address for public contact form.
- `APP_ALLOWED_ORIGINS`: Comma-separated frontend origins.
- `APP_FRONTEND_URL`: Public frontend URL used for auth and Stripe redirects.
- `APP_HTTP_CLIENT_CONNECT_TIMEOUT`: Shared external HTTP connect timeout. Default `5s`.
- `APP_HTTP_CLIENT_READ_TIMEOUT`: Shared external HTTP read timeout. Default `30s`.
- `APP_ADMIN_EMAIL`: First admin email when bootstrapping is intentionally enabled.
- `APP_ADMIN_BOOTSTRAP_ENABLED`: Must be `false` except during deliberate first-admin setup.
- `APP_AI_ASYNC_CORE_POOL_SIZE`: AI worker core pool size.
- `APP_AI_ASYNC_MAX_POOL_SIZE`: AI worker max pool size.
- `APP_AI_ASYNC_QUEUE_CAPACITY`: AI worker queue capacity.
- `APP_EMAIL_ASYNC_CORE_POOL_SIZE`: Email worker core pool size.
- `APP_EMAIL_ASYNC_MAX_POOL_SIZE`: Email worker max pool size.
- `APP_EMAIL_ASYNC_QUEUE_CAPACITY`: Email worker queue capacity.
- `V2_FEATURES_ENABLED`: Global V2 release flag for Playbook, Trading DNA, Journey, and Trading Memory. Use `false` for V1 launch and `true` for V2 release.
- `V3_FEATURES_ENABLED`: Global V3 release flag for AI Trade Replay. Keep aligned with `VITE_V3_FEATURES_ENABLED`.
- `DEV_AUTH_ENABLED`: Must be `false` in production.
- `DEV_TOOLS_ENABLED`: Must be `false` in production.
- `RATE_LIMIT_CSV_IMPORT_MAX`: CSV preview/import request cap per user. Default `6`.
- `RATE_LIMIT_CSV_IMPORT_WINDOW_SECONDS`: CSV preview/import window. Default `300`.
- `RATE_LIMIT_BILLING_CHECKOUT_MAX`: Billing checkout/portal request cap per user. Default `10`.
- `RATE_LIMIT_BILLING_CHECKOUT_WINDOW_SECONDS`: Billing checkout/portal window. Default `300`.

Required for V2 Trading Memory:

- `QDRANT_URL`: Qdrant cluster URL used for Trading Memory vector search.
- `QDRANT_API_KEY`: Qdrant API key. Leave unset only for local unauthenticated Qdrant.
- `QDRANT_COLLECTION_NAME`: Qdrant collection name, for example `tradepilot_trade_memories`.
- `TRADING_MEMORY_QDRANT_AUTO_CREATE_COLLECTION`: Whether a missing collection can be created automatically. Recommended `true` for first deploy, `false` after validation.
- `TRADING_MEMORY_QDRANT_VALIDATE_ON_STARTUP`: Best-effort Qdrant validation during backend startup. Recommended `true`.
- `TRADING_MEMORY_QDRANT_EXPECTED_DISTANCE`: Expected Qdrant distance metric. Default `COSINE`.
- `TRADING_MEMORY_QDRANT_CREATE_PAYLOAD_INDEXES`: Create/verify filterable payload indexes. Default `true`.
- `TRADING_MEMORY_RETRY_ENABLED`: Enables scheduled retry policy metadata. Default `false`.
- `TRADING_MEMORY_RETRY_MAX_ATTEMPTS`: Maximum indexing attempts before exhaustion. Default `5`.
- `TRADING_MEMORY_RETRY_INITIAL_DELAY_SECONDS`: Initial retry backoff. Default `60`.
- `TRADING_MEMORY_RETRY_MAX_DELAY_SECONDS`: Maximum retry backoff. Default `3600`.
- `TRADING_MEMORY_RETRY_BATCH_SIZE`: Admin retry batch bound. Default `50`.

Accepted aliases:

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `STRIPE_PRO_PRICE_ID` for the monthly price only
- `FRONTEND_URL`

Production safety:

- Do not use localhost in `APP_ALLOWED_ORIGINS` for production.
- Production startup fails if `APP_ALLOWED_ORIGINS` contains localhost or `127.0.0.1` under the `prod` or `production` Spring profile.
- Production startup fails if `APP_FRONTEND_URL` is missing or points to localhost under the `prod` or `production` Spring profile.
- Do not use `*` with credentials-enabled CORS.
- Do not enable `DEV_AUTH_ENABLED` or `DEV_TOOLS_ENABLED` in production.
- Use `JPA_DDL_AUTO=validate` in production.

## Frontend

Required:

- `VITE_API_BASE_URL`: Backend API base URL, for example `https://api.tradepilot.app/api`.
- `VITE_V2_FEATURES_ENABLED`: Frontend V2 release flag. Use `false` for V1 launch and `true` for V2 release.
- `VITE_V3_FEATURES_ENABLED`: Frontend V3/AI Trade Replay release flag. Keep aligned with `V3_FEATURES_ENABLED`.

`VITE_API_BASE_URL` may fall back to local backend during development. Production builds must set it explicitly; the frontend throws a clear runtime configuration error if it is missing.

Optional:

- `VITE_IDLE_TIMEOUT_MINUTES`: Default idle timeout.
- `VITE_IDLE_WARNING_MINUTES`: Warning window before idle logout.

Never expose backend-only secrets through `VITE_*`.

Vite variables are compiled into the frontend bundle. Changing `VITE_V2_FEATURES_ENABLED` requires a frontend rebuild and redeploy. Changing `V2_FEATURES_ENABLED` requires a backend restart or redeploy. See [Feature Flags](./FEATURE_FLAGS.md).
