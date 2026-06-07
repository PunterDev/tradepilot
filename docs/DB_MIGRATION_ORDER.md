# TradePilot Database Migration Order

Run these SQL files from `backend/` against a fresh Supabase Postgres database in this order.

1. `backend/supabase_schema_consistency_fixes.sql`
   - Creates and repairs the core schema: users, trading accounts, imports, trades, journals, AI review tables, coach snapshots, discipline tables, and core indexes.
   - Includes trade journal metadata columns: `setup`, `confidence`, `emotion`, and `notes`.
   - Adds the AI trade review cache unique index.

2. `backend/supabase_billing_limits.sql`
   - Adds user billing fields, subscription fields, admin audit logs, and AI usage events.
   - Adds billing lookup indexes and usage type constraints.

3. `backend/supabase_admin_portal.sql`
   - Repairs admin role support and admin audit log indexes/constraints.

4. `backend/supabase_user_isolation_indexes.sql`
   - Adds user-scoped indexes for accounts, trades, journals, AI reviews, AI analyses, coach snapshots, discipline scores, rule violations, and auth tokens.
   - Replaces older global uniqueness models with user-scoped uniqueness where needed.

5. `backend/supabase_trade_journal_metadata.sql`
   - Idempotently adds nullable trade journal metadata fields and constraints.
   - Safe to run after the base schema even if the fields already exist.

6. `backend/supabase_contact_messages.sql`
   - Creates and repairs public contact/admin CRM tables and indexes.

7. `backend/supabase_feature_usage.sql`
   - Creates feature usage events and indexes for admin usage and retention dashboards.

8. `backend/supabase_session_reviews.sql`
   - Creates AI session review cache table, uniqueness constraint, and lookup indexes.

9. `backend/supabase_trading_dna_performance.sql`
   - Creates and repairs Trading DNA snapshots, fingerprint fields, cache indexes, and trade fingerprint indexes.

10. `backend/supabase_observability.sql`
    - Creates API, slow request, AI, cache, email, Stripe, query performance, processed Stripe event, and system alert tables.
    - Adds observability and admin dashboard performance indexes.

11. `backend/supabase_performance_index_verification.sql`
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
