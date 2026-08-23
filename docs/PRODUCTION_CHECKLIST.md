# TradePilot Production Checklist

Run this checklist before pushing, deploying, or opening beta access.

## 1. Secret And Config Gate

- Confirm `application.properties` does not exist.
- Confirm `.env`, `.env.local`, `backend/.env`, `frontend/.env`, and `backend/src/main/resources/application-dev.yml` are ignored.
- Confirm committed config files contain placeholders only.
- Confirm frontend env contains no Stripe secret, webhook secret, OpenAI key, Resend API key, JWT secret, or database credential.
- Confirm backend production env has a unique `JWT_SECRET` with at least 32 random bytes encoded as Base64.
- Confirm `APP_ALLOWED_ORIGINS` is explicit and does not use `*`.
- Confirm production `APP_ALLOWED_ORIGINS` contains no localhost or `127.0.0.1` origins.
- Confirm `APP_FRONTEND_URL` points to the deployed frontend, not localhost.
- Confirm `APP_ADMIN_BOOTSTRAP_ENABLED=false` after first admin setup.
- Confirm V1 launch flags are synchronized: `V2_FEATURES_ENABLED=false` and `VITE_V2_FEATURES_ENABLED=false`.
- Confirm dev flags are explicit: `DEV_AUTH_ENABLED=false` and `DEV_TOOLS_ENABLED=false`.

## 2. Database Gate

- Run migrations in [DB_MIGRATION_ORDER.md](./DB_MIGRATION_ORDER.md).
- Confirm `backend/supabase_trade_replay_v3.sql` ran before `backend/supabase_trade_replay_enum_constraint_sync.sql`.
- Confirm `processed_stripe_events.event_id` is unique.
- Confirm `uk_ai_trade_reviews_cache` exists.
- Confirm `trades` has nullable `setup`, `confidence`, `emotion`, and `notes`.
- Confirm high-traffic indexes for trades, accounts, AI reviews, session reviews, Trading DNA, feature usage, billing, and observability exist.

## 3. Auth And Admin Gate

- Register and verify a normal user.
- Confirm `/api/auth/me` returns `200` with a valid JWT.
- Confirm missing, malformed, and expired JWTs return `401`.
- Confirm normal users receive `403` for `/api/admin/**`.
- Confirm admins can load dashboard, users, billing, contact messages, feature usage, retention, and observability.
- Confirm first admin setup follows [DEPLOYMENT_NOTES.md](./DEPLOYMENT_NOTES.md) and is turned off afterward.

## 4. Stripe Gate

- Configure `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRO_MONTHLY_PRICE_ID`, and `STRIPE_PRO_YEARLY_PRICE_ID`.
- Configure the production webhook endpoint as `/api/stripe/webhook`.
- Confirm webhook signatures are required and invalid signatures return `400`.
- Confirm duplicate webhook event IDs are ignored.
- Confirm `invoice_payment.paid` with a textual invoice ID is handled.
- Confirm monthly checkout uses the monthly price ID.
- Confirm yearly checkout uses the yearly price ID.
- Confirm stale Stripe events do not downgrade manual admin upgrades.

## 5. Email And Contact Gate

- Configure `RESEND_API_KEY`, `EMAIL_FROM`, and `CONTACT_EMAIL`.
- Register a user and confirm verification email delivery.
- Trigger password reset and confirm reset email delivery.
- Submit the public contact form and confirm `contact_messages` is saved.
- Break Resend temporarily and confirm contact messages remain saved with delivery failure metadata.
- Reply from admin contact CRM and confirm normal users cannot access the CRM.

## 6. OpenAI And AI Cost Gate

- Configure `OPENAI_API_KEY`.
- Confirm AI trade review, chart analysis, Daily Coach, Weekly Coach, Trading DNA, and Session Review return safe fallbacks on OpenAI failure.
- For V1 launch, confirm Playbook, Trading DNA, and Journey return `404 FEATURE_DISABLED`; confirm Coach still works.
- Confirm cached AI responses return before usage checks.
- Confirm force refresh consumes AI credits.
- Confirm Free users have 10 monthly AI credits and Pro users have 100 monthly AI credits.
- Confirm Free users receive `403` for Session Review, Daily Coach, Weekly Coach, Trading DNA, and Advanced Analytics API calls.
- Confirm exhausted AI credits return an AI credit limit response and cached AI reads do not consume credits.
- Confirm async AI work uses bounded `ai-worker-*` threads.
- Confirm duplicate AI trade review requests reuse the unique cache row.

## 7. Frontend Gate

- Confirm `VITE_API_BASE_URL` points to the deployed backend `/api` base.
- Confirm the frontend build/deploy environment explicitly sets `VITE_API_BASE_URL`.
- Confirm `GET /api/v1/features` reports Playbook, Trading DNA, and Journey disabled for V1; confirm Coach is enabled.
- Logged-out `/` shows the homepage.
- Logged-in USER `/` redirects to `/dashboard`.
- Logged-in ADMIN `/` redirects to `/admin`.
- `/privacy`, `/terms`, and `/contact` remain public.
- Normal users do not see admin navigation.
- Stale selected account state is cleared or recovered without 403 loops.

## 8. Smoke Test Commands

```bash
npm run build
mvn clean test
```

Stripe local webhook smoke:

```bash
stripe listen --forward-to http://localhost:8080/api/stripe/webhook
stripe trigger checkout.session.completed
stripe trigger customer.subscription.updated
stripe trigger customer.subscription.deleted
stripe trigger invoice.payment_succeeded
stripe trigger invoice.payment_failed
```

CORS preflight smoke:

```bash
curl -i -X OPTIONS "$VITE_API_BASE_URL/auth/me" \
  -H "Origin: https://tradepilot.app" \
  -H "Access-Control-Request-Method: GET"
```

## 9. Final Signoff

- No real secrets are present in tracked or staged files.
- Migrations have been run and verified.
- `npm run build` passes.
- `mvn clean test` passes.
- Stripe, Resend, OpenAI, and Supabase production values are configured in the deployment platform, not committed.
