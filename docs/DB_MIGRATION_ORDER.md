# TradePilot Database Migration Order

Run these SQL files from `backend/` against a fresh Supabase Postgres database in this order. Apply SQL migrations before starting the backend with `JPA_DDL_AUTO=validate`.

Legend:
- Types: `CREATE TABLE`, `ALTER TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`, `DATA BACKFILL`, `VERIFICATION ONLY`
- Risk: `non-destructive`, `potentially locking`, `data-changing`

1. `backend/supabase_schema_consistency_fixes.sql`
   - Purpose: core users, accounts, imports, trades, journals, AI review/cache, coach, discipline, and base indexes.
   - Types: `CREATE TABLE`, `ALTER TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
   - Risk: non-destructive on fresh DB; potentially locking on existing large tables.

2. `backend/supabase_billing_limits.sql`
   - Purpose: user billing/subscription fields, admin audit logs, AI usage events, billing lookup indexes.
   - Types: `ALTER TABLE`, `CREATE TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
   - Risk: non-destructive; potentially locking when adding constraints/indexes.

3. `backend/supabase_ai_credit_usage.sql`
   - Purpose: monthly AI credit ledger for Free/Pro credit enforcement.
   - Types: `CREATE TABLE`, `CREATE INDEX`
   - Risk: non-destructive.

4. `backend/supabase_ai_credit_packs.sql`
   - Purpose: AI credit purchase ledger, credit transaction audit trail, Stripe checkout/session fields, and credit-pack related billing support.
   - Types: `CREATE TABLE`, `ALTER TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
   - Risk: non-destructive; potentially locking on existing billing/user tables.
   - Dependency note: run before `supabase_performance_indexes.sql`, which adds indexes for credit-purchase support.

5. `backend/supabase_ai_usage_events_add_leak_detector.sql`
   - Purpose: allows `AI_LEAK_DETECTOR` in AI usage constraints.
   - Types: `CHECK CONSTRAINT`
   - Risk: potentially locking while replacing the constraint.

6. `backend/supabase_ai_leak_detector_snapshots.sql`
   - Purpose: cached AI Leak Detector snapshots.
   - Types: `CREATE TABLE`, `CREATE INDEX`
   - Risk: non-destructive.

7. `backend/supabase_admin_portal.sql`
   - Purpose: admin role support and admin audit log indexes/constraints.
   - Types: `ALTER TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
   - Risk: non-destructive; potentially locking on users/admin audit tables.

8. `backend/supabase_user_isolation_indexes.sql`
   - Purpose: user-scoped uniqueness and lookup indexes across accounts, trades, journals, AI reviews, analyses, coach, discipline, violations, and auth tokens.
   - Types: `CREATE INDEX`, `CHECK CONSTRAINT`
   - Risk: potentially locking; verify duplicate cleanup state before enforcing user-scoped uniqueness on existing data.

9. `backend/supabase_trade_journal_metadata.sql`
   - Purpose: nullable journal metadata fields on trades: setup, confidence, emotion, notes.
   - Types: `ALTER TABLE`, `CHECK CONSTRAINT`
   - Risk: non-destructive; potentially locking.

10. `backend/supabase_account_r_multiple_support.sql`
    - Purpose: account/trade R-multiple support used by risk and analytics views.
    - Types: `ALTER TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
    - Risk: non-destructive; potentially locking.

11. `backend/supabase_contact_messages.sql`
    - Purpose: public contact and admin CRM tables/indexes.
    - Types: `CREATE TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
    - Risk: non-destructive.

12. `backend/supabase_feature_usage.sql`
    - Purpose: feature usage events for admin usage, adoption, and retention dashboards.
    - Types: `CREATE TABLE`, `CREATE INDEX`
    - Risk: non-destructive.

13. `backend/supabase_feature_usage_events_add_ai_leak_detector.sql`
    - Purpose: allows `AI_LEAK_DETECTOR` feature telemetry.
    - Types: `CHECK CONSTRAINT`
    - Risk: potentially locking while replacing the constraint.

14. `backend/supabase_performance_timeline.sql`
    - Purpose: Journey/AI Performance Timeline snapshots and milestones.
    - Types: `CREATE TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
    - Risk: non-destructive; required before enabling `/api/v1/ai/performance-timeline`.

15. `backend/supabase_feature_usage_events_add_performance_timeline.sql`
    - Purpose: compatibility constraint update for existing databases to allow `PERFORMANCE_TIMELINE` telemetry.
    - Types: `CHECK CONSTRAINT`
    - Risk: potentially locking.

16. `backend/supabase_playbook.sql`
    - Purpose: AI Trading Playbook setup, checklist, trade assignment, analysis snapshot, and milestone tables.
    - Types: `CREATE TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
    - Risk: non-destructive; required before enabling `/api/v1/playbook` or `/api/v1/ai/playbook/**`.

17. `backend/supabase_trading_memory.sql`
    - Purpose: `trade_memories` source-of-truth metadata and indexing status.
    - Types: `CREATE TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
    - Risk: non-destructive; required before enabling Trading Memory.

18. `backend/supabase_trading_memory_hardening.sql`
    - Purpose: retry metadata and retry lookup index for Trading Memory.
    - Types: `ALTER TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
    - Risk: non-destructive; potentially locking.

19. `backend/supabase_feature_usage_events_add_trading_memory_actions.sql`
    - Purpose: allows current action telemetry values, including Trading Memory actions.
    - Types: `CHECK CONSTRAINT`
    - Risk: potentially locking.

20. `backend/supabase_feature_funnels.sql`
    - Purpose: action-level feature telemetry columns, canonical action constraint, and funnel analytics indexes.
    - Types: `ALTER TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`, `DATA BACKFILL`
    - Risk: non-destructive but data-changing; fills existing missing action values with `VIEW`.

21. `backend/supabase_session_reviews.sql`
    - Purpose: AI session review cache table, uniqueness constraint, and lookup indexes.
    - Types: `CREATE TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
    - Risk: non-destructive.

22. `backend/supabase_trading_dna_performance.sql`
    - Purpose: Trading DNA snapshots, fingerprint fields, cache indexes, and trade fingerprint indexes.
    - Types: `CREATE TABLE`, `ALTER TABLE`, `CREATE INDEX`
    - Risk: non-destructive; potentially locking on trade indexes.

23. `backend/supabase_observability.sql`
    - Purpose: API, slow request, AI, cache, email, Stripe, query performance, processed Stripe event, and system alert tables.
    - Types: `CREATE TABLE`, `ALTER TABLE`, `CREATE INDEX`, `DATA BACKFILL`
    - Risk: non-destructive but data-changing for existing system alert status values.

24. `backend/supabase_observability_request_diagnostics.sql`
    - Purpose: request diagnostics/error-code columns and indexes used by admin observability.
    - Types: `ALTER TABLE`, `CREATE INDEX`
    - Risk: non-destructive; potentially locking.

25. `backend/supabase_performance_indexes.sql`
    - Purpose: beta-launch composite indexes for analytics, duplicate detection, AI cache lookup, import history, and credit purchase support.
    - Types: `CREATE INDEX`
    - Risk: non-destructive; potentially locking on large tables.

26. `backend/supabase_user_token_version.sql`
    - Purpose: `users.token_version` for stateless JWT invalidation after password reset.
    - Types: `ALTER TABLE`
    - Risk: non-destructive; potentially locking.

27. `backend/supabase_trade_session_unknown.sql`
    - Purpose: allows `UNKNOWN` trade session so imports can preserve unclassified sessions.
    - Types: `CHECK CONSTRAINT`
    - Risk: potentially locking.

28. `backend/supabase_trade_contract_symbols.sql`
    - Purpose: exact futures contract fields on `trades`: original broker symbol, normalized contract symbol, source, confirmed timestamp.
    - Types: `ALTER TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
    - Risk: non-destructive; potentially locking.

29. `backend/supabase_trade_replay_v3.sql`
    - Purpose: AI Trade Replay persistence tables for replay state, cached candles, and replay insights.
    - Types: `CREATE TABLE`, `CREATE INDEX`, `CHECK CONSTRAINT`
    - Risk: non-destructive.

30. `backend/supabase_trade_replay_enum_constraint_sync.sql`
    - Purpose: synchronizes Trade Replay status/insight CHECK constraints with current Java enums.
    - Types: `CHECK CONSTRAINT`, `VERIFICATION ONLY`
    - Risk: non-destructive if existing data is valid; intentionally aborts if unexpected values exist.
    - Dependency note: must run after `supabase_trade_replay_v3.sql`.

31. `backend/supabase_performance_index_verification.sql`
    - Purpose: read-only release verification for expected indexes and duplicate cleanup state.
    - Types: `VERIFICATION ONLY`
    - Risk: non-destructive.

## Required Replay Order

```text
backend/supabase_trade_replay_v3.sql
↓
backend/supabase_trade_replay_enum_constraint_sync.sql
```

The enum sync must not run before the replay tables and baseline constraints exist.

## Verification Queries

```sql
select column_name
from information_schema.columns
where table_name = 'trades'
  and column_name in ('setup', 'confidence', 'emotion', 'notes', 'contract_symbol', 'original_broker_symbol')
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
select table_name
from information_schema.tables
where table_schema = 'public'
  and table_name in (
    'ai_credit_usage',
    'ai_credit_purchases',
    'ai_credit_transactions',
    'performance_timeline_snapshots',
    'performance_milestones',
    'trade_replays',
    'trade_replay_insights',
    'market_candles'
  )
order by table_name;
```

```sql
select conname, pg_get_constraintdef(oid)
from pg_constraint
where conname in ('ck_trade_replays_status', 'ck_trade_replay_insights_type')
order by conname;
```

```sql
select indexname
from pg_indexes
where tablename in ('trades', 'ai_session_reviews', 'trading_dna_snapshots', 'trade_replays', 'ai_credit_purchases')
order by tablename, indexname;
```
