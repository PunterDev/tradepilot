# TradePilot Supabase SQL Run Order

This document defines the recommended order for running the SQL files in `backend/` on a fresh Supabase Postgres database.

All current SQL files are written to be safe to rerun where practical, using `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, or explicit constraint repair blocks. For existing production databases, run these during a quiet maintenance window, review any failed check constraints for legacy invalid data, and take a backup first.

## Fresh Database Order

| Order | File | Required | Type | Safe to rerun | Purpose |
| --- | --- | --- | --- | --- | --- |
| 1 | `backend/supabase_schema_consistency_fixes.sql` | Yes | Base schema + compatibility repair | Yes | Creates/repairs core app tables and columns used by JPA entities: users, accounts, imports, trades, journal entries, AI trade tables, coach snapshots, discipline tables, and core indexes/checks. Run first because later files depend on `users`, `trading_accounts`, and `trades`. |
| 2 | `backend/supabase_billing_limits.sql` | Yes | Billing/usage schema | Yes | Adds user plan/subscription fields, admin audit logs, and `ai_usage_events`. Repairs the `usage_type` check constraint so `SESSION_REVIEW` is allowed. |
| 3 | `backend/supabase_admin_portal.sql` | Yes for admin portal | Admin schema/index repair | Yes | Adds/repairs admin role support, `pro_until`, admin audit indexes, and the admin audit action check constraint. Some objects overlap with billing by design. |
| 4 | `backend/supabase_user_isolation_indexes.sql` | Yes | User isolation + auth repair | Mostly yes | Adds user-scoping indexes, repairs the trade external-key uniqueness model, creates `auth_tokens`, and adds token type checks. It intentionally drops old global trade uniqueness constraints and the old AI trade analysis cache constraint before adding user-scoped replacements. |
| 5 | `backend/supabase_contact_messages.sql` | Yes for public contact/admin CRM | Feature schema | Yes | Creates/repairs `contact_messages`, mini-CRM fields, status migration from `READ` to `OPEN`, contact status checks, and contact message indexes. |
| 6 | `backend/supabase_feature_usage.sql` | Yes for admin feature usage/retention | Analytics/event schema | Yes | Creates `feature_usage_events`, feature usage indexes, and current feature enum/check coverage. |
| 7 | `backend/supabase_session_reviews.sql` | Yes for Session Review | Feature schema + cache | Mostly yes | Creates `ai_session_reviews`, adds newer AI session fields, removes duplicate rows before adding the unique `(user_id, account_id, session_date)` constraint, and adds lookup indexes. |
| 8 | `backend/supabase_trading_dna_performance.sql` | Yes for Trading DNA | Feature schema + performance | Yes | Creates/repairs `trading_dna_snapshots`, token/cost metadata columns, and Trading DNA fingerprint/cache indexes. Also adds trade fingerprint indexes. |
| 9 | `backend/supabase_observability.sql` | Yes for admin observability | Observability schema + performance | Yes | Creates observability tables for API, slow request, AI, cache, email, Stripe, query performance, and system alerts. Adds performance indexes for observability and admin analytics dashboards. |

## Required Base Schema Files

Run these before any feature or observability file:

1. `backend/supabase_schema_consistency_fixes.sql`
2. `backend/supabase_billing_limits.sql`
3. `backend/supabase_admin_portal.sql`
4. `backend/supabase_user_isolation_indexes.sql`

The first file is the true base/bootstrap repair file. The next three add app-wide concerns that many features assume are present: plan fields, admin audit records, auth token tables, and user-scoped uniqueness/indexes.

## Feature-Specific SQL Files

Run these after the base files:

- `backend/supabase_contact_messages.sql`
- `backend/supabase_feature_usage.sql`
- `backend/supabase_session_reviews.sql`
- `backend/supabase_trading_dna_performance.sql`

These files back public contact CRM, feature usage analytics/retention, cached Session Review, and cached Trading DNA.

## Observability SQL Files

Run this after base and feature tables:

- `backend/supabase_observability.sql`

This file creates request, slow request, AI cost, cache, email, Stripe, query performance, and alert tables. It also adds observability dashboard indexes.

## Files To Run Last

Run these late in the sequence:

- `backend/supabase_trading_dna_performance.sql`
- `backend/supabase_observability.sql`

Trading DNA performance should run after `trades`, `trading_accounts`, and user-scoped indexes exist. Observability should run last because it is mostly additive monitoring infrastructure and performance indexing for the already-created application tables.

## Required Files

Run these for the current application to work end-to-end:

- `backend/supabase_schema_consistency_fixes.sql`
- `backend/supabase_billing_limits.sql`
- `backend/supabase_admin_portal.sql`
- `backend/supabase_user_isolation_indexes.sql`
- `backend/supabase_contact_messages.sql`
- `backend/supabase_feature_usage.sql`
- `backend/supabase_session_reviews.sql`
- `backend/supabase_trading_dna_performance.sql`
- `backend/supabase_observability.sql`

## Repair / Compatibility Files

These files are especially useful for databases that existed before newer TradePilot features:

- `backend/supabase_schema_consistency_fixes.sql`
- `backend/supabase_user_isolation_indexes.sql`
- `backend/supabase_billing_limits.sql`
- `backend/supabase_admin_portal.sql`
- `backend/supabase_contact_messages.sql`
- `backend/supabase_session_reviews.sql`
- `backend/supabase_trading_dna_performance.sql`

## Performance / Index Files

These files mainly add indexes or performance-facing tables, although some also create feature tables:

- `backend/supabase_user_isolation_indexes.sql`
- `backend/supabase_feature_usage.sql`
- `backend/supabase_trading_dna_performance.sql`
- `backend/supabase_observability.sql`
- `backend/supabase_admin_portal.sql`
- `backend/supabase_contact_messages.sql`

## Safe To Rerun

All files are intended to be rerunnable for normal additive migrations and repairs:

- `backend/supabase_schema_consistency_fixes.sql`
- `backend/supabase_billing_limits.sql`
- `backend/supabase_admin_portal.sql`
- `backend/supabase_user_isolation_indexes.sql`
- `backend/supabase_contact_messages.sql`
- `backend/supabase_feature_usage.sql`
- `backend/supabase_session_reviews.sql`
- `backend/supabase_trading_dna_performance.sql`
- `backend/supabase_observability.sql`

Use extra care with `backend/supabase_user_isolation_indexes.sql` and `backend/supabase_session_reviews.sql` because they include compatibility repair steps that drop outdated constraints or remove duplicate session review rows before adding the current uniqueness model.

## Existing Production Database Notes

- Back up the database before applying these scripts.
- Run `backend/supabase_schema_consistency_fixes.sql` first even on existing databases; it is additive and repairs missing core columns/indexes.
- Run files in the fresh-database order unless you are deliberately applying a single targeted repair.
- `backend/supabase_user_isolation_indexes.sql` intentionally drops old global constraints that conflict with user-scoped imports:
  - `trades_external_trade_key_key`
  - `uk_ai_trade_analysis_cache`
- `backend/supabase_session_reviews.sql` deletes duplicate `ai_session_reviews` rows before adding a unique constraint. It keeps the newest row per `(user_id, account_id, session_date)`.
- Constraint repair blocks may fail if old rows contain invalid enum strings. Clean those rows first, then rerun the affected script.
- The important `ai_usage_events` allowed values are:
  - `AI_TRADE_REVIEW`
  - `DAILY_COACH`
  - `WEEKLY_COACH`
  - `TRADING_DNA`
  - `SESSION_REVIEW`

## Ordering Issues Found

- Feature-specific SQL files assume core tables already exist. On a fresh Supabase database, running `supabase_billing_limits.sql`, `supabase_feature_usage.sql`, or `supabase_observability.sql` before core tables exist can fail due to foreign keys to `users`. The fix is to run `supabase_schema_consistency_fixes.sql` first.
- Some files overlap intentionally, especially billing/admin audit and user/index repairs. This is acceptable because the scripts use idempotent DDL patterns.
