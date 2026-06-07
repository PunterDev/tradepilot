-- TradePilot performance index verification queries.
-- Run in Supabase SQL editor after applying the performance index migrations.

-- 1. Confirm exact index names and definitions.
select
    schemaname,
    tablename,
    indexname,
    indexdef
from pg_indexes
where schemaname = 'public'
  and indexname in (
      'idx_trades_user_account_date',
      'idx_trades_user_account_date_entry',
      'idx_trading_dna_user_account_hash',
      'idx_trading_dna_user_account_days_hash',
      'idx_ai_session_reviews_user_account_date_hash',
      'idx_users_billing_lookup'
  )
order by tablename, indexname;

-- 2. Verify expected columns and order for the hot-path indexes.
select
    t.relname as table_name,
    i.relname as index_name,
    array_agg(a.attname order by k.ordinality) as indexed_columns
from pg_class t
join pg_index ix on t.oid = ix.indrelid
join pg_class i on i.oid = ix.indexrelid
join lateral unnest(ix.indkey) with ordinality as k(attnum, ordinality) on true
join pg_attribute a on a.attrelid = t.oid and a.attnum = k.attnum
where t.relkind = 'r'
  and i.relname in (
      'idx_trades_user_account_date',
      'idx_trades_user_account_date_entry',
      'idx_trading_dna_user_account_hash',
      'idx_trading_dna_user_account_days_hash',
      'idx_ai_session_reviews_user_account_date_hash',
      'idx_users_billing_lookup'
  )
group by t.relname, i.relname
order by t.relname, i.relname;

-- 3. Check whether the planner uses the session-review generated-cache index.
explain analyze
select *
from ai_session_reviews
where user_id = :user_id
  and account_id = :account_id
  and session_date = :session_date
  and review_hash = :review_hash
order by created_at desc
limit 1;

-- 4. Check whether the planner uses the Trading DNA snapshot hash index.
explain analyze
select *
from trading_dna_snapshots
where user_id = :user_id
  and account_id is not distinct from :account_id
  and days = :days
  and include_all_accounts = :include_all_accounts
  and data_hash = :data_hash
order by updated_at desc
limit 1;

-- 5. Check whether the planner uses the session trade-date index.
explain analyze
select *
from trades
where user_id = :user_id
  and account_id = :account_id
  and trade_date = :trade_date
order by entry_time asc, id asc;

-- 6. Check whether the planner uses the billing lookup index.
explain analyze
select id, plan, subscription_status, current_period_end
from users
where id = :user_id;

-- 7. Get actual cache hit/miss counts for the newly instrumented hot paths.
select
    cache_name,
    count(*) filter (where hit = true) as hits,
    count(*) filter (where hit = false) as misses,
    min(created_at) as first_seen_at,
    max(created_at) as last_seen_at
from cache_metrics
where cache_name in (
    'session-review-cache-hit',
    'session-review-cache-miss',
    'trading-dna-cache-hit',
    'trading-dna-cache-miss'
)
  and created_at >= now() - interval '24 hours'
group by cache_name
order by cache_name;

-- 8. Correlate slow endpoint averages with AI request duration.
select
    feature,
    count(*) as ai_calls,
    round(avg(duration_ms), 1) as avg_ai_ms,
    percentile_cont(0.95) within group (order by duration_ms) as p95_ai_ms,
    count(*) filter (where success = false) as failed_ai_calls
from ai_request_metrics
where feature in ('SESSION_REVIEW', 'TRADING_DNA')
  and created_at >= now() - interval '24 hours'
group by feature
order by feature;
