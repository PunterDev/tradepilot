# TradePilot Database Migration Order

Run these SQL files from `backend/` against a fresh Supabase Postgres database in this order.

1. `backend/supabase_schema_consistency_fixes.sql`
   - Creates and repairs the core schema: users, trading accounts, imports, trades, journals, AI review tables, coach snapshots, discipline tables, and core indexes.
   - Includes trade journal metadata columns: `setup`, `confidence`, `emotion`, and `notes`.
   - Adds the AI trade review cache unique index.

2. `backend/supabase_billing_limits.sql`
   - Adds user billing fields, subscription fields, admin audit logs, and AI usage events.
   - Adds billing lookup indexes and usage type constraints.

3. `backend/supabase_ai_usage_events_add_leak_detector.sql`
   - Updates the `ai_usage_events` usage type constraint to allow `AI_LEAK_DETECTOR`.
   - Required before enabling AI Leak Detector generation in an existing production database.

4. `backend/supabase_ai_credit_usage.sql`
   - Adds the pooled monthly AI credit ledger used for Free and Pro credit enforcement.
   - Adds indexes for user, date, and user-feature monthly credit lookups.

5. `backend/supabase_ai_leak_detector_snapshots.sql`
   - Adds cached AI Leak Detector snapshots.
   - Required before enabling `/api/ai/leak-detector`.

6. `backend/supabase_admin_portal.sql`
   - Repairs admin role support and admin audit log indexes/constraints.

7. `backend/supabase_user_isolation_indexes.sql`
   - Adds user-scoped indexes for accounts, trades, journals, AI reviews, AI analyses, coach snapshots, discipline scores, rule violations, and auth tokens.
   - Replaces older global uniqueness models with user-scoped uniqueness where needed.

8. `backend/supabase_trade_journal_metadata.sql`
   - Idempotently adds nullable trade journal metadata fields and constraints.
   - Safe to run after the base schema even if the fields already exist.

9. `backend/supabase_contact_messages.sql`
   - Creates and repairs public contact/admin CRM tables and indexes.

10. `backend/supabase_feature_usage.sql`
    - Creates feature usage events and indexes for admin usage and retention dashboards.

11. `backend/supabase_feature_usage_events_add_ai_leak_detector.sql`
    - Updates the `feature_usage_events` feature constraint to allow `AI_LEAK_DETECTOR`.
    - Required before enabling AI Leak Detector tracking in an existing production database.

12. `backend/supabase_session_reviews.sql`
    - Creates AI session review cache table, uniqueness constraint, and lookup indexes.

13. `backend/supabase_trading_dna_performance.sql`
    - Creates and repairs Trading DNA snapshots, fingerprint fields, cache indexes, and trade fingerprint indexes.

14. `backend/supabase_observability.sql`
    - Creates API, slow request, AI, cache, email, Stripe, query performance, processed Stripe event, and system alert tables.
    - Adds observability and admin dashboard performance indexes.

15. `backend/supabase_performance_index_verification.sql`
    - Verification script only. Run after all migrations to confirm expected indexes and duplicate cleanup state.

## Verification Queries

```sql
select column_name
from information_schema.columns
where table_name = 'trades'
  and column_name in ('setup', 'confidence', 'emotion', 'notes')
order by column_name;
```

```sql
select indexname
from pg_indexes
where tablename = 'ai_trade_reviews'
  and indexname = 'uk_ai_trade_reviews_cache';
```

```sql
select constraint_name
from information_schema.table_constraints
where table_name = 'processed_stripe_events'
  and constraint_type in ('UNIQUE', 'PRIMARY KEY');
```

```sql
select indexname
from pg_indexes
where tablename = 'ai_credit_usage'
order by indexname;
```

```sql
select indexname
from pg_indexes
where tablename = 'ai_leak_detector_snapshots'
order by indexname;
```
