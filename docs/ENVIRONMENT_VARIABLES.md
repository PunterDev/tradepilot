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
- `APP_ADMIN_EMAIL`: First admin email when bootstrapping is intentionally enabled.
- `APP_ADMIN_BOOTSTRAP_ENABLED`: Must be `false` except during deliberate first-admin setup.
- `APP_AI_ASYNC_CORE_POOL_SIZE`: AI worker core pool size.
- `APP_AI_ASYNC_MAX_POOL_SIZE`: AI worker max pool size.
- `APP_AI_ASYNC_QUEUE_CAPACITY`: AI worker queue capacity.
- `APP_EMAIL_ASYNC_CORE_POOL_SIZE`: Email worker core pool size.
- `APP_EMAIL_ASYNC_MAX_POOL_SIZE`: Email worker max pool size.
- `APP_EMAIL_ASYNC_QUEUE_CAPACITY`: Email worker queue capacity.

Accepted aliases:

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `STRIPE_PRO_PRICE_ID` for the monthly price only
- `FRONTEND_URL`

Production safety:

- Do not use localhost in `APP_ALLOWED_ORIGINS` for production.
- Do not use `*` with credentials-enabled CORS.
- Do not enable `DEV_AUTH_ENABLED` or `DEV_TOOLS_ENABLED` in production.
- Use `JPA_DDL_AUTO=validate` in production.

## Frontend

Required:

- `VITE_API_BASE_URL`: Backend API base URL, for example `https://api.tradepilot.app/api`.

Optional:

- `VITE_IDLE_TIMEOUT_MINUTES`: Default idle timeout.
- `VITE_IDLE_WARNING_MINUTES`: Warning window before idle logout.

Never expose backend-only secrets through `VITE_*`.
