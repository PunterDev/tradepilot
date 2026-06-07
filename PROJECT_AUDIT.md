# TradePilot Project Audit

This is a concise current-state audit after the latest cleanup pass.

## Current Major Features

- Public marketing homepage with pricing, privacy, terms, and contact pages.
- JWT authentication with registration, login, `/auth/me`, email verification, password reset, and protected routing.
- User-scoped trading accounts, trade import, trade list/detail, and dashboard analytics.
- PnL calendar, daily PnL, setup/symbol/weekday breakdowns, Friday/OPEX analytics, behavioral insights, and performance heatmaps.
- Trade Detail modal with cached AI review support and previous/next navigation.
- Trade Replay / Session Review with timeline, best/worst trade, session statistics, cached AI session review, and rule-for-tomorrow flow.
- AI Daily Coach, Weekly Coach, Trading DNA, AI trade review, and chart/trade analysis endpoints.
- Subscription plan limits, usage tracking, Stripe checkout/portal/webhook integration, and admin manual plan management.
- Admin portal for users, billing/revenue, contact message CRM, feature usage, retention analytics, observability, and dev tools.
- Dev-only demo data seeding, clearing, mock upgrade/downgrade, and feature usage backfill.

## Backend Modules

- `analytics`: dashboard, calendar, heatmap, Friday/OPEX, behavioral/session analytics.
- `billing`: plan types, subscription status, usage limits, Stripe client/services/controllers, usage events.
- `coach`: daily/weekly coach snapshot entities, async generation, prompt/build/hash/mapper services.
- `controller`: account, auth, trade, admin, contact, journal, stats, AI, discipline, session review controllers.
- `dev`: dev-only seed/mock tools guarded by profile/property availability.
- `discipline`: deterministic violation detection and score calculation.
- `dto`: request/response contracts for auth, trade, admin, AI, coach, contact, session, stats, discipline.
- `entity`: JPA entities for users, accounts, trades, AI review/cache records, contact messages, journals, auth tokens, violations, audit logs.
- `featureusage`: user feature event tracking plus admin adoption/trend/user analytics.
- `observability`: request, slow request, AI cost, cache, email, Stripe, query performance, alerts, and admin observability dashboard services.
- `repository`: Spring Data repositories and trade specifications.
- `retention`: admin DAU/WAU/MAU, retention trends, cohorts, plan segments, and at-risk user analytics.
- `security`: JWT service/filter, user details service, method/security filter chain configuration.
- `service`: core application services for auth, accounts, trades, admin dashboards, contact CRM, AI review, session review, journals, stats, and aggregate caching.
- `tradingdna`: Trading DNA controller, DTOs, snapshot entity/repository, prompt builder, AI generator, and cache-first service.

## Frontend Routes / Pages

- Public:
  - `/`
  - `/login`
  - `/register`
  - `/privacy`
  - `/terms`
  - `/contact`
  - `/verify-email`
  - `/forgot-password`
  - `/reset-password`
- Trader app:
  - `/dashboard`
  - `/analytics`
  - `/calendar`
  - `/session-review`
  - `/sessions/review`
  - `/trading-dna`
  - `/coach`
  - `/billing`
  - `/dev-tools` when enabled

## Admin Routes / Pages

- `/admin`: SaaS admin command center.
- `/admin/users`: users CRM and manual plan/role management.
- `/admin/contact-messages`: contact inbox/mini CRM with notes/reply/status flow.
- `/admin/feature-usage`: feature adoption, usage ranking, trends, and power users.
- `/admin/retention`: DAU/WAU/MAU, retention, cohorts, at-risk users, plan segmentation.
- `/admin/observability`: API performance, slow requests, AI cost, cache, email, Stripe, alert health.
- `/billing`: role-aware billing page; admin sees revenue/subscription dashboard, users see plan/usage/checkout.
- `/dev-tools`: admin/dev utility page when enabled.

## AI Features

- AI Trade Review with cached review visibility and regenerate/usage behavior.
- AI trade/chart analysis flow with async polling.
- Daily Coach and Weekly Coach with persisted snapshots and async generation.
- Trading DNA with cache-first snapshots, account/all-account scope, input fingerprints, and force refresh.
- Session Review AI coach with cache-first `userId/accountId/date` behavior.
- Legacy weekly reflection endpoint is still present and documented in README.

## Billing Features

- Free/Pro plan fields on users.
- Subscription status tracking.
- Monthly usage events and usage limit enforcement for AI reviews, Daily Coach, Weekly Coach, Trading DNA, Session Review, account creation, and imports.
- Stripe checkout session, billing portal session, webhook handling, and customer recovery.
- Admin manual upgrade/downgrade with audit logs.
- Admin billing summary separates Stripe-paying subscribers from manual Pro users.

## Observability Features

- API request metrics and slow request events.
- AI request metrics including tokens, estimated cost, feature, model, duration, and success.
- Cache hit/miss metrics for aggregate and AI cache paths.
- Email delivery events.
- Stripe event metrics.
- Query performance events using logical query names, not raw SQL.
- System alert storage and dashboard display.
- Admin observability summary/trend endpoints with short TTL caching and bounded result lists.

## Known Manual-Review Files Left In Place

- `backend/src/main/java/com/tradepilot/controller/AiReflectionController.java`: old weekly reflection flow, but README still documents `/api/ai/weekly-review`.
- `backend/src/main/java/com/tradepilot/service/AiReflectionService.java` and `backend/src/main/java/com/tradepilot/ai/*`: support the documented weekly reflection endpoint.
- `backend/src/main/java/com/tradepilot/controller/AiChartReviewController.java`: backend endpoint exists without a current obvious route-level frontend caller; kept because it is a public API surface.
- `backend/src/main/java/com/tradepilot/controller/DevAuthController.java`: conditional local dev helper documented by README and protected by `app.dev-auth.enabled`.
- `frontend/src/hooks/useAiReviewPolling.ts`: still used by `TradeDetailModal`.
- `frontend/src/api/adminApi.ts`: still used by the admin dashboard and shared admin response types.

## Recommended Next Engineering Steps

0. Use `DEPLOYMENT_READINESS_CHECKLIST.md` as the production go/no-go checklist before launch or major deployment.
1. Decide whether the legacy weekly reflection endpoint should remain as a supported API or be migrated into the newer Daily/Weekly Coach model.
2. Decide whether chart review remains a product feature; if yes, add a visible UI entry point or document it as API-only.
3. Add automated unused-export checks such as `ts-prune`/`knip` or ESLint unused-import rules for frontend cleanup confidence.
4. Add ArchUnit-style or simple package-reference checks for backend dead-code cleanup confidence.
5. Continue reducing Spring startup/test time by slicing controller/service tests where full context is unnecessary.
6. Add migration smoke tests against a disposable Postgres instance when the Supabase schema stabilizes.
7. Add admin pagination and sorting coverage tests for high-volume user/contact/feature-event tables.
8. Keep observability summaries read-only on request paths; avoid adding alert evaluator jobs until slow endpoint regressions are consistently below target.
