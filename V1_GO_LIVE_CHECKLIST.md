# TradePilot Coach V1 Go-Live Checklist

Audit date: 2026-09-10 (America/Chicago)

## Repository Result

**CONDITIONAL GO**

The V1 code path is ready to deploy. Launch remains conditional on the production-only checks below because this repository cannot inspect Render, Supabase, Stripe Live, Vercel, DNS, or a real inbox.

## Completed Repository Checks

- [x] V1 feature defaults are conservative: `V2_FEATURES_ENABLED=false`, `V3_FEATURES_ENABLED=false`, `MARKET_DATA_PROVIDER=disabled`.
- [x] V2 UI and API routes are inaccessible when V2 is disabled, including AI Pre-Analysis. V3 Trade Replay is also disabled behind its V3 flag.
- [x] Qdrant and Databento are not required for the V1 path. Trading Memory startup validation is bypassed when V2 is disabled; the market-data provider defaults to disabled.
- [x] Frontend and backend V1 templates use false V2/V3 flags, Coach's canonical public origin, and a non-local production API placeholder.
- [x] CORS only permits explicit origins; production rejects localhost and requires `https://www.tradepilotcoach.com`.
- [x] Production startup rejects dev auth/tools, non-database rate limiting, missing admin access protection, unsafe frontend URLs, and incomplete Stripe Live configuration.
- [x] Stripe webhook is signature-verified, idempotent, and rejects `livemode=false` in production.
- [x] CSV imports have format, size, ownership, duplicate, rate-limit, and parser coverage for Tradovate and Topstep.
- [x] Flyway migrations are immutable, ordered V001 through V012, and Hibernate validation remains enabled.
- [x] Public SEO routes are prerendered, canonicalized, and tested; authenticated routes use `noindex,nofollow`.

## Automated Evidence

- [x] Focused backend suite passed: V1/V2/V3 isolation, auth, RBAC/ownership, Stripe billing and live-mode protection, abuse protection, CSV parsers, session classification, Flyway, and database constraint drift tests.
- [x] `npm run build` passed with Node 22.
- [x] Public Playwright regression suite passed: 60 tests across homepage marketing, public branding, and SEO landing pages.
- [x] `mvn clean test` passed: 542 tests, 0 failures, 0 errors.
- [x] `mvn package -DskipTests` passed.

## Required Render Environment

Set these exact V1 values and confirm the deployed process logs a clean production startup:

```text
SPRING_PROFILES_ACTIVE=production
APP_ALLOWED_ORIGINS=https://www.tradepilotcoach.com
APP_FRONTEND_URL=https://www.tradepilotcoach.com
V2_FEATURES_ENABLED=false
V3_FEATURES_ENABLED=false
MARKET_DATA_PROVIDER=disabled
DEV_AUTH_ENABLED=false
DEV_TOOLS_ENABLED=false
RATE_LIMIT_STORE=database
APP_ADMIN_ACCESS_PROTECTION=external-mfa
```

Also configure the production database, a strong production JWT secret, Resend values, and the required Stripe Live values. Do not add Qdrant, Databento, or any V2/V3 provider credential for the V1 launch.

## Stripe Live Checklist

- [ ] Configure Live endpoint: `https://<render-backend-host>/api/stripe/webhook`.
- [ ] Store its Live endpoint signing secret as `STRIPE_WEBHOOK_SECRET` in Render.
- [ ] Configure Live `STRIPE_SECRET_KEY`, `STRIPE_PRO_MONTHLY_PRICE_ID`, and `STRIPE_PRO_YEARLY_PRICE_ID`.
- [ ] Select: `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.payment_succeeded`, `invoice.payment_failed`, `charge.refunded`, `charge.dispute.created`, and `charge.dispute.closed`.
- [ ] Add `invoice_payment.paid` and `invoice_payment.payment_failed` only if selectable for the Stripe account/API version.
- [ ] Do not send a test-mode success event to production. The production backend correctly rejects `livemode=false`.

## Supabase And Migration Checklist

- [ ] Confirm Render connects to the intended production Supabase PostgreSQL database over SSL.
- [ ] Confirm Flyway applies V001 through V012 successfully and Hibernate starts with `ddl-auto=validate`.
- [ ] Confirm `rate_limit_windows` exists after migration for shared production rate limiting.
- [ ] Confirm no schema repair, destructive migration, or manual DDL is required.

## Browser Smoke Checklist

- [ ] Confirm `https://tradepilotcoach.com` permanently redirects to `https://www.tradepilotcoach.com`.
- [ ] Confirm Vercel production build has `VITE_API_BASE_URL` set to the deployed Render `/api` URL and both `VITE_V2_FEATURES_ENABLED=false` and `VITE_V3_FEATURES_ENABLED=false`.
- [ ] Verify registration, email verification, login, logout, password reset request, reset completion, and expired-session handling with controlled inboxes.
- [ ] With a Free account, create an account, import both one Tradovate and one Topstep CSV, verify duplicate prevention, analytics, journal persistence, and user isolation.
- [ ] Verify Daily Coach, Session Review, and billing/credit enforcement. Do not exercise V2/V3 surfaces.
- [ ] Verify AI Pre-Analysis, Trading Memory, Journey, Playbook, and Trade Replay are absent from navigation and unavailable by direct API request.
- [ ] Verify normal users receive `403` for admin endpoints and cannot use `/api/dev/**` or `/api/dev/e2e/prepare`.

## Launch Decision

Proceed only after every unchecked production item is evidenced. Any failure of Stripe Live provisioning, Flyway/Hibernate validation, production environment validation, canonical domain/CORS behavior, or the controlled-user smoke path changes this result to **NO-GO**.
