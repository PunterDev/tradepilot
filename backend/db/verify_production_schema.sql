-- TradePilot production schema verification.
-- Read-only. Run after Flyway migration against the target PostgreSQL/Supabase database.

select installed_rank, version, description, success
from flyway_schema_history
order by installed_rank;

with required_tables(table_name) as (
    values
        ('users'),
        ('trading_accounts'),
        ('import_batches'),
        ('trades'),
        ('journal_entries'),
        ('ai_reflections'),
        ('ai_trade_reviews'),
        ('ai_trade_analyses'),
        ('ai_coach_snapshot'),
        ('daily_discipline_scores'),
        ('trade_rule_violations'),
        ('admin_audit_logs'),
        ('auth_tokens'),
        ('contact_messages'),
        ('ai_usage_events'),
        ('ai_credit_usage'),
        ('ai_credit_purchases'),
        ('processed_stripe_events'),
        ('stripe_subscription_states'),
        ('stripe_checkout_attempts'),
        ('user_feature_previews'),
        ('feature_usage_events'),
        ('api_request_metrics'),
        ('slow_request_events'),
        ('ai_request_metrics'),
        ('cache_metrics'),
        ('email_delivery_events'),
        ('stripe_events'),
        ('query_performance_events'),
        ('system_alerts'),
        ('ai_session_reviews'),
        ('ai_leak_detector_snapshots'),
        ('trading_dna_snapshots'),
        ('performance_timeline_snapshots'),
        ('performance_milestones'),
        ('playbook_setup'),
        ('playbook_checklist_item'),
        ('trade_playbook_assignment'),
        ('playbook_analysis_snapshot'),
        ('playbook_milestone'),
        ('trade_replays'),
        ('market_candles'),
        ('trade_replay_insights'),
        ('trade_memories'),
        ('rate_limit_windows')
)
select 'required_table' as check_name,
       r.table_name,
       case when t.table_name is null then 'MISSING' else 'OK' end as result
from required_tables r
left join information_schema.tables t
    on t.table_schema = 'public'
   and t.table_name = r.table_name
order by r.table_name;

with required_columns(table_name, column_name) as (
    values
        ('users', 'stripe_customer_id'),
        ('users', 'stripe_subscription_id'),
        ('users', 'stripe_cancel_at_period_end'),
        ('users', 'pro_until'),
        ('users', 'purchased_ai_credits_balance'),
        ('users', 'token_version'),
        ('users', 'analytics_excluded'),
        ('ai_credit_purchases', 'stripe_checkout_session_id'),
        ('ai_credit_purchases', 'stripe_payment_intent_id'),
        ('ai_credit_purchases', 'stripe_charge_id'),
        ('ai_credit_purchases', 'stripe_customer_id'),
        ('ai_credit_purchases', 'pack_id'),
        ('ai_credit_purchases', 'amount_cents'),
        ('ai_credit_purchases', 'currency'),
        ('ai_credit_purchases', 'refunded_amount_cents'),
        ('ai_credit_purchases', 'reversed_credits'),
        ('ai_credit_purchases', 'refund_reversal_credits'),
        ('ai_credit_purchases', 'dispute_reversal_credits'),
        ('ai_credit_purchases', 'dispute_restored_credits'),
        ('ai_credit_purchases', 'dispute_status'),
        ('processed_stripe_events', 'event_id'),
        ('processed_stripe_events', 'stripe_event_created'),
        ('stripe_subscription_states', 'subscription_id'),
        ('stripe_subscription_states', 'last_event_id'),
        ('stripe_subscription_states', 'last_event_type'),
        ('stripe_subscription_states', 'last_event_created'),
        ('stripe_checkout_attempts', 'user_id'),
        ('stripe_checkout_attempts', 'stripe_checkout_session_id'),
        ('user_feature_previews', 'user_id'),
        ('user_feature_previews', 'feature_key'),
        ('user_feature_previews', 'status'),
        ('user_feature_previews', 'reserved_resource_id'),
        ('trades', 'original_broker_symbol'),
        ('trades', 'contract_symbol'),
        ('trades', 'contract_symbol_source'),
        ('trade_memories', 'account_id'),
        ('trade_memories', 'last_error_code'),
        ('trade_memories', 'last_error_message'),
        ('rate_limit_windows', 'rate_key'),
        ('rate_limit_windows', 'window_start'),
        ('rate_limit_windows', 'request_count'),
        ('rate_limit_windows', 'expires_at')
)
select 'required_column' as check_name,
       r.table_name || '.' || r.column_name as object_name,
       case when c.column_name is null then 'MISSING' else 'OK' end as result
from required_columns r
left join information_schema.columns c
    on c.table_schema = 'public'
   and c.table_name = r.table_name
   and c.column_name = r.column_name
order by object_name;

with required_indexes(index_name) as (
    values
        ('ux_users_stripe_customer_id_not_null'),
        ('ux_users_stripe_subscription_id_not_null'),
        ('uk_ai_credit_purchases_checkout_session'),
        ('ux_ai_credit_purchases_payment_intent_not_null'),
        ('ux_ai_credit_purchases_charge_not_null'),
        ('processed_stripe_events_event_id_key'),
        ('stripe_subscription_states_subscription_id_key'),
        ('uk_stripe_checkout_attempts_user'),
        ('uk_user_feature_previews_user_feature'),
        ('idx_processed_stripe_events_stripe_created'),
        ('idx_stripe_subscription_states_last_event_created'),
        ('idx_user_feature_previews_user_status'),
        ('rate_limit_windows_pkey'),
        ('idx_rate_limit_windows_expires_at')
)
select 'required_index_or_unique' as check_name,
       r.index_name,
       case when i.indexname is null and con.conname is null then 'MISSING' else 'OK' end as result
from required_indexes r
left join pg_indexes i
    on i.schemaname = 'public'
   and i.indexname = r.index_name
left join pg_constraint con
    on con.connamespace = 'public'::regnamespace
   and con.conname = r.index_name
order by r.index_name;

with required_constraints(constraint_name) as (
    values
        ('chk_ai_credit_purchases_refunded_amount_nonnegative'),
        ('chk_ai_credit_purchases_reversed_credits_nonnegative'),
        ('chk_ai_credit_purchases_refund_reversal_nonnegative'),
        ('chk_ai_credit_purchases_dispute_reversal_nonnegative'),
        ('chk_ai_credit_purchases_dispute_restored_nonnegative'),
        ('chk_ai_credit_purchases_dispute_restored_lte_reversal'),
        ('ai_credit_usage_monthly_credits_nonnegative'),
        ('ai_credit_usage_purchased_credits_nonnegative'),
        ('chk_user_feature_previews_status'),
        ('chk_users_subscription_status')
        ,('chk_rate_limit_windows_request_count_nonnegative')
)
select 'required_constraint' as check_name,
       r.constraint_name,
       case when c.conname is null then 'MISSING' else 'OK' end as result
from required_constraints r
left join pg_constraint c
    on c.connamespace = 'public'::regnamespace
   and c.conname = r.constraint_name
order by r.constraint_name;

select 'obsolete_constraint_absent' as check_name,
       'users_purchased_ai_credits_balance_nonnegative' as object_name,
       case when exists (
           select 1
           from pg_constraint
           where connamespace = 'public'::regnamespace
             and conname = 'users_purchased_ai_credits_balance_nonnegative'
       ) then 'PRESENT_UNSAFE' else 'OK' end as result;

select 'flyway_expected_version' as check_name,
       'V001__fresh_production_schema' as object_name,
       case when exists (
           select 1
           from flyway_schema_history
           where version = '001'
             and description = 'fresh production schema'
             and success = true
       ) then 'OK' else 'MISSING' end as result;

select 'flyway_expected_version' as check_name,
       'V002__first_insight_activation' as object_name,
       case when exists (
           select 1
           from flyway_schema_history
           where version = '002'
             and description = 'first insight activation'
             and success = true
       ) then 'OK' else 'MISSING' end as result;

select 'flyway_expected_version' as check_name,
       'V003__seo_acquisition_analytics' as object_name,
       case when exists (
           select 1
           from flyway_schema_history
           where version = '003'
             and description = 'seo acquisition analytics'
             and success = true
       ) then 'OK' else 'MISSING' end as result;

select 'flyway_expected_version' as check_name,
       'V004__seo_analytics_hardening' as object_name,
       case when exists (
           select 1
           from flyway_schema_history
           where version = '004'
             and description = 'seo analytics hardening'
             and success = true
       ) then 'OK' else 'MISSING' end as result;

select 'flyway_expected_version' as check_name,
       'V005__ai_credit_purchase_schema_alignment' as object_name,
       case when exists (
           select 1
           from flyway_schema_history
           where version = '005'
             and description = 'ai credit purchase schema alignment'
             and success = true
       ) then 'OK' else 'MISSING' end as result;

select 'flyway_expected_version' as check_name,
       'V006__distributed_rate_limit_windows' as object_name,
       case when exists (
           select 1
           from flyway_schema_history
           where version = '006'
             and description = 'distributed rate limit windows'
             and success = true
       ) then 'OK' else 'MISSING' end as result;
