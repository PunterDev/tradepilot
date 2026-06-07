# TradePilot Deployment Notes

## Backend

Render-style setup:

- Build command: `mvn clean package -DskipTests`
- Start command: `java -jar target/tradepilot-backend-0.0.1-SNAPSHOT.jar`
- Java version: 17
- Set all backend env vars from [ENVIRONMENT_VARIABLES.md](./ENVIRONMENT_VARIABLES.md).
- Use `JPA_DDL_AUTO=validate` after Supabase SQL migrations are applied.

Health check:

- `GET /actuator/health`

Security:

- `/api/stripe/webhook` is public but requires valid Stripe signature verification.
- `/api/contact` is public and rate limited.
- `/api/admin/**` requires `ADMIN`.
- `/api/dev/**` must remain disabled in production.

## Frontend

Vercel-style setup:

- Build command: `npm run build`
- Output directory: `dist`
- Required env: `VITE_API_BASE_URL=https://your-backend.example.com/api`
- Optional idle envs: `VITE_IDLE_TIMEOUT_MINUTES`, `VITE_IDLE_WARNING_MINUTES`

Routing:

- Configure SPA fallback to `index.html`.
- Keep `/privacy`, `/terms`, and `/contact` public.

## Supabase

- Apply SQL in [DB_MIGRATION_ORDER.md](./DB_MIGRATION_ORDER.md).
- Enable backups before beta launch.
- Use the pooled JDBC endpoint if deploying on serverless or low-connection platforms.
- Confirm SSL mode is present in the JDBC URL.

## First Admin Setup

1. Register and verify the intended admin account.
2. Set `APP_ADMIN_EMAIL` to that email.
3. Temporarily set `APP_ADMIN_BOOTSTRAP_ENABLED=true`.
4. Redeploy or restart backend.
5. Verify the user is `ADMIN`.
6. Set `APP_ADMIN_BOOTSTRAP_ENABLED=false`.
7. Redeploy again.

Do not leave admin bootstrap enabled.

## Stripe

- Configure Product/Prices for monthly and yearly Pro.
- Set `STRIPE_PRO_MONTHLY_PRICE_ID` and `STRIPE_PRO_YEARLY_PRICE_ID`.
- Configure Customer Portal in Stripe.
- Add production webhook endpoint: `https://your-backend.example.com/api/stripe/webhook`.
- Subscribe at minimum to checkout session, subscription updated/deleted, invoice paid/succeeded, and invoice failed events.
- Copy the endpoint signing secret into `STRIPE_WEBHOOK_SECRET`.

## SMTP

- Use SMTP app-specific credentials.
- Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM`.
- Verify account verification, password reset, contact form, and admin reply flows.

## OpenAI

- Set `OPENAI_API_KEY`.
- Monitor AI request metrics and daily cost alerting.
- Tune `APP_AI_ASYNC_*` values if beta traffic exceeds the default small worker pool.

## Smoke Test

After deploy:

1. Visit frontend homepage while logged out.
2. Register and verify a test user.
3. Log in and create an account.
4. Import a small CSV.
5. Open Analytics, Calendar, Session Review, Trading DNA, Coach, and Billing.
6. Start monthly checkout and cancel.
7. Start yearly checkout and verify the Stripe price in test mode.
8. Submit the contact form.
9. Log in as admin and verify admin pages.
10. Confirm backend logs contain no secrets, JWTs, reset tokens, webhook signatures, raw prompts, or SMTP credentials.
