# TradePilot Performance Notes

This document captures the beta-launch performance posture for the backend. It is intentionally practical: the goal is to keep user-facing paths bounded, indexed, and observable before adding heavier scale work.

## Hot Paths

- Trade import rejects files above 5,000 rows and performs one batched duplicate-key lookup per upload instead of one database lookup per row.
- Trade list endpoints clamp page size to 100.
- Admin pagination uses `PageRequestParams`, capped at 50 rows per page.
- AI cache reads are preferred before credit deduction. Cached AI results cost 0 credits.
- Daily and Weekly Coach skip OpenAI generation when the selected day/week has no trades.

## Rate Limits

The `RateLimitInterceptor` normalizes `/api/v1/**` to `/api/**`, so these rules cover both versioned and legacy paths.

| Area | Paths | Default |
| --- | --- | --- |
| Login | `POST /api/auth/login` | 8/min/IP |
| Register | `POST /api/auth/register` | 5/5 min/IP |
| Auth email | `forgot-password`, `reset-password`, `resend-verification`, `verify-email` | 5/5 min/IP |
| Contact | `POST /api/contact` | 3/5 min/IP |
| CSV import | `POST /api/trades/import` | 6/5 min/user |
| Billing checkout | Subscription, portal, and AI credit pack checkout session creation | 10/5 min/user |
| AI generation | AI review, coach generation, session review, Trading DNA, Leak Detector | 12/5 min/user/feature |

Tune these with `app.rate-limit.*` properties for production abuse patterns.

## Index Strategy

Run `backend/supabase_performance_indexes.sql` after the main schema and feature migrations. The script focuses on:

- `trades(user_id, account_id, ...)` composites for dashboard filters and analytics dimensions.
- CSV import duplicate detection for non-null external trade keys.
- Account selector and import history sorting.
- AI snapshot/cache lookup by user/account/status/update time.
- Credit purchase support lookups.

Avoid adding standalone indexes unless a query plan proves they are needed. Most user-owned tables already have user/account indexes from earlier migrations.

## Current N+1 Posture

- Trade list and trade lookup repository methods use `@EntityGraph(attributePaths = {"account"})` to avoid lazy account fetches during DTO mapping.
- Admin user pages use batched trade/account count queries for page contents.
- Feature usage top-users performs bounded follow-up lookups for a top-10 admin report; acceptable for beta.

Known future improvement: `AdminRetentionAnalyticsService` still scans users and feature usage events for summary/cohort reporting. It is admin-only, but should move to aggregate SQL before large beta cohorts.

## Observability

- Request metrics and slow request events are recorded through the observability interceptors.
- CSV import logs requested/imported/skipped row counts, existing duplicate-key count, and duration.
- Slow observability writes are isolated from read-only analytics transactions and do not break user-facing endpoints.

## Beta Load-Test Checklist

1. Import 100, 1,000, and 5,000 row CSV files and confirm import time and duplicate handling.
2. Reimport the same file and confirm skipped duplicates are detected without per-row DB queries.
3. Exercise dashboard, calendar, analytics, Session Review, Trading DNA, and Leak Detector for an account with 5,000+ trades.
4. Verify rate limits return structured 429 responses for auth, contact, import, checkout, and AI generation.
5. Run Supabase query plans for the largest production-like `trades` filters and compare against `supabase_performance_indexes.sql`.
