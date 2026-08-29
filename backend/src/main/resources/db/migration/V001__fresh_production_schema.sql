-- TradePilot canonical fresh production schema.
-- This Flyway baseline is intended for a brand-new empty PostgreSQL/Supabase database.
-- It represents the current application schema directly instead of replaying the
-- historical supabase_*.sql correction stack.

create table users (
    id bigserial primary key,
    name varchar(100) not null,
    email varchar(150) not null unique,
    password_hash varchar(255) not null,
    email_verified boolean not null default false,
    plan varchar(20) not null default 'FREE',
    role varchar(20) not null default 'USER',
    subscription_status varchar(20) not null default 'NONE',
    current_period_start timestamp,
    current_period_end timestamp,
    stripe_customer_id varchar(255),
    stripe_subscription_id varchar(255),
    stripe_cancel_at_period_end boolean not null default false,
    stripe_cancel_at timestamp,
    stripe_canceled_at timestamp,
    pro_until timestamp,
    purchased_ai_credits_balance integer not null default 0,
    token_version integer not null default 0,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint chk_users_plan check (plan in ('FREE', 'PRO')),
    constraint chk_users_role check (role in ('USER', 'ADMIN')),
    constraint chk_users_subscription_status check (subscription_status in ('NONE', 'ACTIVE', 'TRIALING', 'PAST_DUE', 'CANCELED', 'UNPAID', 'INCOMPLETE', 'INCOMPLETE_EXPIRED'))
);

create unique index ux_users_stripe_customer_id_not_null
    on users(stripe_customer_id)
    where stripe_customer_id is not null and stripe_customer_id <> '';

create unique index ux_users_stripe_subscription_id_not_null
    on users(stripe_subscription_id)
    where stripe_subscription_id is not null and stripe_subscription_id <> '';

create index idx_users_created_at on users(created_at);
create index idx_users_plan on users(plan);
create index idx_users_role on users(role);
create index idx_users_subscription_status on users(subscription_status);
create index idx_users_lower_email on users(lower(email));
create index idx_users_lower_name on users(lower(name));

create table trading_accounts (
    id bigserial primary key,
    user_id bigint references users(id) on delete cascade,
    name varchar(255) not null,
    prop_firm varchar(255) not null,
    account_number varchar(255),
    account_type varchar(255),
    starting_balance numeric(19, 2),
    default_risk_amount numeric(18, 2),
    status varchar(255) not null default 'ACTIVE',
    created_at timestamp not null default now(),
    constraint chk_trading_accounts_default_risk_amount check (default_risk_amount is null or default_risk_amount > 0)
);

create index idx_trading_accounts_user_id on trading_accounts(user_id);
create index idx_trading_accounts_user_created_at on trading_accounts(user_id, created_at);

create table import_batches (
    id bigserial primary key,
    user_id bigint references users(id) on delete cascade,
    account_id bigint references trading_accounts(id) on delete set null,
    file_name varchar(255),
    total_rows integer,
    imported_rows integer,
    skipped_duplicates integer,
    imported_at timestamp
);

create index idx_import_batches_user_id on import_batches(user_id);
create index idx_import_batches_account_id on import_batches(account_id);
create index idx_import_batches_user_account_imported_at on import_batches(user_id, account_id, imported_at);

create table trades (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint references trading_accounts(id) on delete set null,
    import_batch_id bigint references import_batches(id) on delete set null,
    symbol varchar(20) not null,
    original_broker_symbol varchar(80),
    contract_symbol varchar(40),
    contract_symbol_source varchar(30),
    contract_symbol_confirmed_at timestamp,
    market varchar(20) not null,
    trade_date date not null,
    entry_time timestamp,
    exit_time timestamp,
    side varchar(20) not null,
    setup varchar(50),
    session varchar(20) not null,
    entry_price numeric(18, 6),
    stop_price numeric(18, 6),
    target_price numeric(18, 6),
    exit_price numeric(18, 6),
    quantity integer,
    risk_amount numeric(18, 2),
    pnl_amount numeric(18, 2),
    pnl_r numeric(10, 2),
    outcome varchar(20) not null,
    confidence integer,
    confidence_rating integer,
    emotion varchar(50),
    execution_rating integer,
    followed_plan boolean,
    mistake_tag varchar(50),
    notes text,
    psychology_notes text,
    lesson_learned text,
    screenshot_url varchar(500),
    external_trade_key varchar(255),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_trades_user_account_external_key unique (user_id, account_id, external_trade_key),
    constraint chk_trades_risk_amount check (risk_amount is null or risk_amount > 0),
    constraint ck_trades_contract_symbol_source check (contract_symbol_source is null or contract_symbol_source in ('IMPORTED', 'NORMALIZED', 'USER_CONFIRMED', 'ADMIN_BACKFILL'))
);

create index idx_trades_user_id on trades(user_id);
create index idx_trades_account_id on trades(account_id);
create index idx_trades_user_account_date on trades(user_id, account_id, trade_date);
create index idx_trades_user_account_date_entry on trades(user_id, account_id, trade_date, entry_time);
create index idx_trades_user_entry_time on trades(user_id, entry_time);
create index idx_trades_account_entry_time on trades(account_id, entry_time);
create index idx_trades_user_account_entry_time on trades(user_id, account_id, entry_time);
create index idx_trades_contract_symbol on trades(contract_symbol);
create index idx_trades_original_broker_symbol on trades(original_broker_symbol);
create index idx_trades_user_account_external_key_present on trades(user_id, account_id, external_trade_key) where external_trade_key is not null;
create index idx_trades_user_account_date_pnl on trades(user_id, account_id, trade_date, pnl_amount);
create index idx_trades_user_account_symbol_date on trades(user_id, account_id, symbol, trade_date);
create index idx_trades_user_account_setup_date on trades(user_id, account_id, setup, trade_date);
create index idx_trades_user_account_session_date on trades(user_id, account_id, session, trade_date);
create index idx_trades_user_account_side_date on trades(user_id, account_id, side, trade_date);

create table journal_entries (
    id bigserial primary key,
    user_id bigint references users(id) on delete cascade,
    account_id bigint references trading_accounts(id) on delete set null,
    journal_date date not null,
    mood varchar(50),
    setup_quality varchar(100),
    mistakes varchar(1000),
    lesson_learned varchar(1000),
    notes varchar(2000),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_journal_user_account_date unique (user_id, account_id, journal_date)
);

create index idx_journal_entries_user_id on journal_entries(user_id);
create index idx_journal_entries_user_account_date on journal_entries(user_id, account_id, journal_date);

create table ai_reflections (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    start_date date not null,
    end_date date not null,
    prompt_text text,
    response_text text not null,
    created_at timestamp not null default now()
);

create index idx_ai_reflections_user_id on ai_reflections(user_id);
create index idx_ai_reflections_user_period on ai_reflections(user_id, start_date, end_date);

create table ai_trade_reviews (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    trade_id bigint references trades(id) on delete cascade,
    account_id bigint references trading_accounts(id) on delete set null,
    review_date varchar(255),
    symbol varchar(255),
    pnl double precision,
    grade varchar(255),
    review_version varchar(255),
    review_hash varchar(255),
    summary text,
    mistakes_json text,
    strengths_json text,
    improvement_plan_json text,
    created_at timestamptz,
    status varchar(255),
    error_message text,
    tags_json text,
    constraint uk_ai_trade_reviews_cache unique (user_id, trade_id, review_version, review_hash)
);

create index idx_ai_trade_reviews_user_id on ai_trade_reviews(user_id);
create index idx_ai_trade_reviews_trade_id on ai_trade_reviews(trade_id);
create index idx_ai_trade_reviews_account_id on ai_trade_reviews(account_id);
create index idx_ai_trade_reviews_cache on ai_trade_reviews(user_id, trade_id, review_version, review_hash);
create index idx_ai_trade_reviews_user_account_created_at on ai_trade_reviews(user_id, account_id, created_at);

create table ai_trade_analyses (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    trade_id bigint not null references trades(id) on delete cascade,
    review_version varchar(32) not null,
    review_hash varchar(128) not null,
    status varchar(32) not null,
    symbol varchar(32),
    pnl numeric(14, 2),
    grade varchar(8),
    execution_score integer,
    discipline_score integer,
    risk_score integer,
    summary text,
    market_context text,
    entry_analysis text,
    exit_analysis text,
    risk_analysis text,
    psychology_analysis text,
    chart_analysis text,
    mistakes_json text,
    strengths_json text,
    improvement_plan_json text,
    tags_json text,
    has_chart_image boolean not null default false,
    error_message text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_ai_trade_analysis_cache unique (user_id, trade_id, review_version, review_hash)
);

create index idx_ai_trade_analysis_user_id on ai_trade_analyses(user_id);
create index idx_ai_trade_analysis_trade_id on ai_trade_analyses(trade_id);
create index idx_ai_trade_analysis_hash on ai_trade_analyses(review_hash);
create index idx_ai_trade_analysis_user_cache on ai_trade_analyses(user_id, trade_id, review_version, review_hash);
create index idx_ai_trade_analyses_user_created_at on ai_trade_analyses(user_id, created_at);

create table ai_coach_snapshot (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint not null references trading_accounts(id) on delete cascade,
    snapshot_type varchar(20) not null,
    period_start date not null,
    period_end date not null,
    status varchar(20) not null default 'PENDING',
    total_trades integer,
    net_pnl numeric(12, 2),
    win_rate numeric(6, 2),
    profit_factor numeric(8, 2),
    best_setup varchar(100),
    worst_setup varchar(100),
    best_session varchar(50),
    worst_session varchar(50),
    biggest_mistake varchar(255),
    deterministic_summary_json text,
    ai_response_json text,
    review_hash varchar(128),
    error_message varchar(1000),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_ai_coach_snapshot unique (user_id, account_id, snapshot_type, period_start, period_end, review_hash),
    constraint chk_ai_coach_snapshot_type check (snapshot_type in ('DAILY', 'WEEKLY')),
    constraint chk_ai_coach_snapshot_status check (status in ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

create index idx_ai_coach_snapshot_user_id on ai_coach_snapshot(user_id);
create index idx_ai_coach_snapshot_account_id on ai_coach_snapshot(account_id);
create index idx_ai_coach_snapshot_user_account_type_period on ai_coach_snapshot(user_id, account_id, snapshot_type, period_start, period_end);
create index idx_ai_coach_snapshot_user_account_status_updated on ai_coach_snapshot(user_id, account_id, status, updated_at);

create table daily_discipline_scores (
    id bigserial primary key,
    user_id bigint,
    trade_date date,
    account_id bigint,
    score integer,
    total_trades integer,
    violation_count integer,
    major_violation_count integer,
    critical_violation_count integer,
    current_clean_day_streak integer,
    clean_day boolean,
    created_at timestamp,
    updated_at timestamp
);

create index idx_daily_discipline_scores_user_id on daily_discipline_scores(user_id);
create index idx_daily_discipline_scores_user_account_date on daily_discipline_scores(user_id, account_id, trade_date);

create table trade_rule_violations (
    id bigserial primary key,
    user_id bigint,
    trade_id bigint,
    rule_code varchar(255),
    rule_title varchar(255),
    category varchar(255),
    severity varchar(255),
    reason text,
    source varchar(255),
    created_at timestamp
);

create index idx_trade_rule_violations_user_id on trade_rule_violations(user_id);
create index idx_trade_rule_violations_trade_id on trade_rule_violations(trade_id);
create index idx_trade_rule_violations_user_trade on trade_rule_violations(user_id, trade_id);

create table admin_audit_logs (
    id bigserial primary key,
    admin_user_id bigint not null,
    target_user_id bigint not null,
    action varchar(40) not null,
    reason varchar(500),
    before_value text,
    after_value text,
    created_at timestamp not null default now()
);

create index idx_admin_audit_admin_user_id on admin_audit_logs(admin_user_id);
create index idx_admin_audit_target_user_id on admin_audit_logs(target_user_id);
create index idx_admin_audit_created_at on admin_audit_logs(created_at);
create index idx_admin_audit_admin_created_at on admin_audit_logs(admin_user_id, created_at);
create index idx_admin_audit_target_created_at on admin_audit_logs(target_user_id, created_at);

create table auth_tokens (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    token_hash varchar(255) not null,
    type varchar(40) not null,
    expires_at timestamp not null,
    used_at timestamp,
    created_at timestamp not null default now(),
    constraint chk_auth_tokens_type check (type in ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

create index idx_auth_tokens_user_id on auth_tokens(user_id);
create index idx_auth_tokens_token_hash on auth_tokens(token_hash);
create index idx_auth_tokens_type on auth_tokens(type);
create index idx_auth_tokens_expires_at on auth_tokens(expires_at);
create index idx_auth_tokens_lookup on auth_tokens(token_hash, type, expires_at);

create table contact_messages (
    id bigserial primary key,
    name varchar(120) not null,
    email varchar(180) not null,
    subject varchar(180) not null,
    message varchar(5000) not null,
    status varchar(20) not null default 'NEW',
    email_sent boolean not null default false,
    email_error varchar(1000),
    remote_address varchar(120),
    user_agent varchar(500),
    admin_notes varchar(5000),
    lead_source varchar(120),
    replied_at timestamp,
    last_viewed_at timestamp,
    reply_body varchar(5000),
    reply_subject varchar(180),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint chk_contact_messages_status check (status in ('NEW', 'VIEWED', 'REPLIED', 'ARCHIVED'))
);

create index idx_contact_messages_status on contact_messages(status);
create index idx_contact_messages_created_at on contact_messages(created_at);
create index idx_contact_messages_status_created_at on contact_messages(status, created_at);

create table ai_usage_events (
    id bigserial primary key,
    user_id bigint not null,
    usage_type varchar(80) not null,
    credits_used integer not null,
    created_at timestamp not null default now(),
    constraint chk_ai_usage_events_usage_type check (usage_type in ('AI_TRADE_REVIEW', 'TRADE_REVIEW', 'SESSION_REVIEW', 'DAILY_COACH', 'WEEKLY_COACH', 'TRADING_DNA', 'AI_LEAK_DETECTOR', 'PERFORMANCE_TIMELINE', 'PLAYBOOK_AI_DRAFT', 'PLAYBOOK_AI_REVIEW'))
);

create index idx_ai_usage_events_user_id on ai_usage_events(user_id);
create index idx_ai_usage_events_user_type_created on ai_usage_events(user_id, usage_type, created_at);
create index idx_ai_usage_events_created_at on ai_usage_events(created_at);

create table ai_credit_usage (
    id bigserial primary key,
    user_id bigint not null,
    feature varchar(40) not null,
    credits integer not null,
    monthly_credits integer,
    purchased_credits integer,
    created_at timestamp not null default now(),
    request_id bigint,
    metadata text,
    constraint ai_credit_usage_monthly_credits_nonnegative check (monthly_credits is null or monthly_credits >= 0),
    constraint ai_credit_usage_purchased_credits_nonnegative check (purchased_credits is null or purchased_credits >= 0)
);

create index idx_ai_credit_usage_user_id on ai_credit_usage(user_id);
create index idx_ai_credit_usage_created_at on ai_credit_usage(created_at);
create index idx_ai_credit_usage_user_created on ai_credit_usage(user_id, created_at);
create index idx_ai_credit_usage_user_feature_created on ai_credit_usage(user_id, feature, created_at);

create table ai_credit_purchases (
    id bigserial primary key,
    user_id bigint not null,
    stripe_checkout_session_id varchar(255) not null,
    stripe_customer_id varchar(255),
    stripe_payment_intent_id varchar(255),
    stripe_charge_id varchar(255),
    pack_id varchar(50),
    credits_purchased integer not null,
    amount_cents integer not null,
    refunded_amount_cents integer not null default 0,
    reversed_credits integer not null default 0,
    refund_reversal_credits integer not null default 0,
    dispute_reversal_credits integer not null default 0,
    dispute_restored_credits integer not null default 0,
    currency varchar(10) not null,
    status varchar(30) not null,
    dispute_status varchar(50),
    created_at timestamp not null default now(),
    completed_at timestamp,
    refunded_at timestamp,
    reversed_at timestamp,
    disputed_at timestamp,
    constraint uk_ai_credit_purchases_checkout_session unique (stripe_checkout_session_id),
    constraint chk_ai_credit_purchases_refund_reversal_nonnegative check (refund_reversal_credits >= 0),
    constraint chk_ai_credit_purchases_dispute_reversal_nonnegative check (dispute_reversal_credits >= 0),
    constraint chk_ai_credit_purchases_dispute_restored_nonnegative check (dispute_restored_credits >= 0),
    constraint chk_ai_credit_purchases_dispute_restored_lte_reversal check (dispute_restored_credits <= dispute_reversal_credits)
);

create index idx_ai_credit_purchases_user_id on ai_credit_purchases(user_id);
create index idx_ai_credit_purchases_created_at on ai_credit_purchases(created_at);
create index idx_ai_credit_purchases_payment_intent on ai_credit_purchases(stripe_payment_intent_id);
create index idx_ai_credit_purchases_charge on ai_credit_purchases(stripe_charge_id);
create index idx_ai_credit_purchases_customer on ai_credit_purchases(stripe_customer_id);
create index idx_ai_credit_purchases_user_completed_at on ai_credit_purchases(user_id, completed_at);
create unique index ux_ai_credit_purchases_payment_intent_not_null on ai_credit_purchases(stripe_payment_intent_id) where stripe_payment_intent_id is not null and stripe_payment_intent_id <> '';
create unique index ux_ai_credit_purchases_charge_not_null on ai_credit_purchases(stripe_charge_id) where stripe_charge_id is not null and stripe_charge_id <> '';

create table processed_stripe_events (
    id bigserial primary key,
    event_id varchar(120) not null unique,
    event_type varchar(120) not null,
    processed_at timestamp not null default now(),
    stripe_event_created timestamp
);

create index idx_processed_stripe_events_processed_at on processed_stripe_events(processed_at);
create index idx_processed_stripe_events_event_type_processed_at on processed_stripe_events(event_type, processed_at);
create index idx_processed_stripe_events_stripe_created on processed_stripe_events(stripe_event_created);

create table stripe_subscription_states (
    id bigserial primary key,
    subscription_id varchar(255) not null unique,
    last_event_id varchar(120) not null,
    last_event_type varchar(120) not null,
    last_event_created timestamp not null,
    updated_at timestamp not null default now()
);

create index idx_stripe_subscription_states_last_event_created on stripe_subscription_states(last_event_created);

create table stripe_checkout_attempts (
    id bigserial primary key,
    user_id bigint not null,
    stripe_checkout_session_id varchar(255),
    status varchar(30) not null,
    expires_at timestamp not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_stripe_checkout_attempts_user unique (user_id)
);

create index idx_stripe_checkout_attempts_status_expires on stripe_checkout_attempts(status, expires_at);

create table feature_usage_events (
    id bigserial primary key,
    user_id bigint not null,
    feature varchar(50) not null,
    action varchar(80),
    event_type varchar(40),
    account_id bigint,
    session_id varchar(120),
    entity_type varchar(80),
    entity_id varchar(120),
    source varchar(80),
    plan_at_event varchar(20),
    metadata_json text,
    occurred_at timestamp,
    created_at timestamp not null default now()
);

create index idx_feature_usage_user_id on feature_usage_events(user_id);
create index idx_feature_usage_created_at on feature_usage_events(created_at);
create index idx_feature_usage_feature_created on feature_usage_events(feature, created_at);
create index idx_feature_usage_user_feature_created on feature_usage_events(user_id, feature, created_at);
create index idx_feature_usage_feature_action_created on feature_usage_events(feature, action, created_at);
create index idx_feature_usage_user_feature_action_created on feature_usage_events(user_id, feature, action, created_at);
create index idx_feature_usage_events_created_user on feature_usage_events(created_at, user_id);
create index idx_feature_usage_events_user_created on feature_usage_events(user_id, created_at);

create table api_request_metrics (
    id bigserial primary key,
    endpoint varchar(220) not null,
    method varchar(16) not null,
    status integer not null,
    duration_ms bigint not null,
    user_id bigint references users(id) on delete set null,
    created_at timestamp not null default now()
);

create index idx_api_request_metrics_created_at on api_request_metrics(created_at);
create index idx_api_request_metrics_endpoint_created_at on api_request_metrics(endpoint, created_at);
create index idx_api_request_metrics_status_created_at on api_request_metrics(status, created_at);
create index idx_api_request_metrics_user_id_created_at on api_request_metrics(user_id, created_at);
create index idx_api_request_metrics_created_duration on api_request_metrics(created_at, duration_ms);

create table slow_request_events (
    id bigserial primary key,
    endpoint varchar(220) not null,
    duration_ms bigint not null,
    status integer not null,
    user_id bigint references users(id) on delete set null,
    severity varchar(16) not null,
    created_at timestamp not null default now()
);

create index idx_slow_request_events_created_at on slow_request_events(created_at);
create index idx_slow_request_events_endpoint_created_at on slow_request_events(endpoint, created_at);
create index idx_slow_request_events_created_at_desc on slow_request_events(created_at desc);

create table ai_request_metrics (
    id bigserial primary key,
    user_id bigint references users(id) on delete set null,
    feature varchar(64) not null,
    model varchar(80) not null,
    prompt_tokens integer,
    completion_tokens integer,
    total_tokens integer,
    estimated_cost numeric(12, 6),
    duration_ms bigint not null,
    success boolean not null,
    created_at timestamp not null default now()
);

create index idx_ai_request_metrics_created_at on ai_request_metrics(created_at);
create index idx_ai_request_metrics_user_id_created_at on ai_request_metrics(user_id, created_at);
create index idx_ai_request_metrics_feature_created_at on ai_request_metrics(feature, created_at);

create table cache_metrics (
    id bigserial primary key,
    cache_name varchar(120) not null,
    hit boolean not null,
    created_at timestamp not null default now()
);

create index idx_cache_metrics_cache_name_created_at on cache_metrics(cache_name, created_at);
create index idx_cache_metrics_created_at on cache_metrics(created_at);

create table email_delivery_events (
    id bigserial primary key,
    recipient varchar(240) not null,
    template varchar(120) not null,
    success boolean not null,
    duration_ms bigint not null,
    error_message varchar(700),
    created_at timestamp not null default now()
);

create index idx_email_delivery_events_created_at on email_delivery_events(created_at);
create index idx_email_delivery_events_success_created_at on email_delivery_events(success, created_at);

create table stripe_events (
    id bigserial primary key,
    event_type varchar(120) not null,
    success boolean not null,
    duration_ms bigint not null,
    error_message varchar(700),
    created_at timestamp not null default now()
);

create index idx_stripe_events_created_at on stripe_events(created_at);
create index idx_stripe_events_event_type_created_at on stripe_events(event_type, created_at);
create index idx_stripe_events_success_created_at on stripe_events(success, created_at);

create table query_performance_events (
    id bigserial primary key,
    query_name varchar(140) not null,
    duration_ms bigint not null,
    created_at timestamp not null default now()
);

create index idx_query_performance_events_created_at on query_performance_events(created_at);
create index idx_query_performance_events_query_name_created_at on query_performance_events(query_name, created_at);

create table system_alerts (
    id bigserial primary key,
    type varchar(80) not null,
    severity varchar(24) not null,
    message varchar(700) not null,
    resolved boolean not null default false,
    status varchar(24) not null default 'OPEN',
    created_at timestamp not null default now(),
    resolved_at timestamp,
    resolved_by bigint references users(id) on delete set null
);

create index idx_system_alerts_resolved_created_at on system_alerts(resolved, created_at);
create index idx_system_alerts_type_created_at on system_alerts(type, created_at);
create index idx_system_alerts_status_created_at on system_alerts(status, created_at);
create index idx_system_alerts_severity_created_at on system_alerts(severity, created_at);
create index idx_system_alerts_resolved_at on system_alerts(resolved_at);
create index idx_system_alerts_resolved_by_created_at on system_alerts(resolved_by, created_at);

create table ai_session_reviews (
    id bigserial primary key,
    user_id bigint not null,
    account_id bigint not null,
    session_date date not null,
    review_hash varchar(64) not null,
    summary text,
    what_went_well text,
    what_hurt_performance text,
    biggest_mistake text,
    best_setup text,
    worst_decision text,
    emotional_pattern text,
    execution_pattern text,
    risk_management_pattern text,
    one_rule_for_tomorrow text,
    created_at timestamp not null default now(),
    constraint uk_ai_session_reviews_user_account_date unique (user_id, account_id, session_date)
);

create index idx_ai_session_reviews_user_id on ai_session_reviews(user_id);
create index idx_ai_session_reviews_user_account_date on ai_session_reviews(user_id, account_id, session_date);
create index idx_ai_session_reviews_user_account_date_hash on ai_session_reviews(user_id, account_id, session_date, review_hash);
create index idx_ai_session_reviews_created_at on ai_session_reviews(created_at);
create index idx_ai_session_reviews_user_account_created_at on ai_session_reviews(user_id, account_id, created_at);

create table ai_leak_detector_snapshots (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint not null references trading_accounts(id) on delete cascade,
    data_hash varchar(128) not null,
    total_trades integer not null,
    result_json text not null,
    model varchar(80),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index idx_ai_leak_detector_user_id on ai_leak_detector_snapshots(user_id);
create index idx_ai_leak_detector_user_account_hash on ai_leak_detector_snapshots(user_id, account_id, data_hash);
create index idx_ai_leak_detector_user_account_updated on ai_leak_detector_snapshots(user_id, account_id, updated_at);
create index idx_ai_leak_detector_snapshots_user_account_updated_desc on ai_leak_detector_snapshots(user_id, account_id, updated_at desc);

create table trading_dna_snapshots (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint references trading_accounts(id) on delete cascade,
    days integer not null,
    include_all_accounts boolean not null,
    data_hash varchar(128) not null,
    status varchar(32) not null,
    total_trades integer,
    ai_response_json text,
    context_json text,
    timeline_json text,
    model varchar(80),
    prompt_tokens integer,
    completion_tokens integer,
    total_tokens integer,
    estimated_cost numeric(12, 6),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index idx_trading_dna_user_id on trading_dna_snapshots(user_id);
create index idx_trading_dna_user_account_timeframe on trading_dna_snapshots(user_id, account_id, days);
create index idx_trading_dna_data_hash on trading_dna_snapshots(data_hash);
create index idx_trading_dna_user_account_hash on trading_dna_snapshots(user_id, account_id, data_hash);
create index idx_trading_dna_user_account_days_hash on trading_dna_snapshots(user_id, account_id, days, include_all_accounts, data_hash);
create index idx_trading_dna_user_created_at on trading_dna_snapshots(user_id, created_at);
create index idx_trading_dna_snapshots_user_account_updated on trading_dna_snapshots(user_id, account_id, updated_at);

create table performance_timeline_snapshots (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint not null references trading_accounts(id) on delete cascade,
    period_type varchar(20) not null default 'MONTHLY',
    period_start date not null,
    period_end date not null,
    data_status varchar(30) not null,
    trade_count integer not null default 0,
    metrics_json text,
    comparison_json text,
    category_scores_json text,
    evolution_score integer,
    score_confidence varchar(20),
    narrative_json text,
    ai_model varchar(80),
    prompt_version varchar(40),
    data_hash varchar(128) not null,
    generated_at timestamp not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_performance_timeline_account_period unique (account_id, period_type, period_start),
    constraint performance_timeline_data_status_check check (data_status in ('READY', 'LIMITED_DATA', 'NO_ACTIVITY')),
    constraint performance_timeline_score_confidence_check check (score_confidence is null or score_confidence in ('LOW', 'MEDIUM', 'HIGH')),
    constraint performance_timeline_score_range_check check (evolution_score is null or (evolution_score >= 0 and evolution_score <= 100))
);

create index idx_performance_timeline_user_account on performance_timeline_snapshots(user_id, account_id);
create index idx_performance_timeline_account_period on performance_timeline_snapshots(account_id, period_start desc);

create table performance_milestones (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint not null references trading_accounts(id) on delete cascade,
    milestone_type varchar(80) not null,
    earned_date date not null,
    period_start date,
    period_end date,
    evidence_json text,
    created_at timestamp not null default now(),
    constraint uk_performance_milestone_account_type unique (account_id, milestone_type)
);

create index idx_performance_milestones_user_account on performance_milestones(user_id, account_id, earned_date desc);

create table playbook_setup (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint references trading_accounts(id) on delete set null,
    name varchar(120) not null,
    short_description varchar(280),
    description text,
    category varchar(40) not null default 'CUSTOM',
    status varchar(24) not null default 'DRAFT',
    direction varchar(16) not null default 'BOTH',
    instruments_json text not null default '[]',
    sessions_json text not null default '[]',
    timeframes_json text not null default '[]',
    tags_json text not null default '[]',
    market_context_json text not null default '[]',
    risk_rules_json text not null default '[]',
    management_rules_json text not null default '[]',
    post_trade_rules_json text not null default '[]',
    icon_key varchar(60),
    color_key varchar(60),
    image_url varchar(500),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    archived_at timestamp,
    version_number integer not null default 1
);

create index idx_playbook_setup_user_id on playbook_setup(user_id);
create index idx_playbook_setup_account_id on playbook_setup(account_id);
create index idx_playbook_setup_status on playbook_setup(status);
create index idx_playbook_setup_updated_at on playbook_setup(updated_at desc);

create table playbook_checklist_item (
    id bigserial primary key,
    setup_id bigint not null references playbook_setup(id) on delete cascade,
    checklist_type varchar(32) not null,
    label varchar(180) not null,
    description text,
    is_required boolean not null default true,
    weight integer not null default 1,
    display_order integer not null default 0,
    active boolean not null default true,
    expected_value_type varchar(32),
    expected_value_json text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint chk_playbook_checklist_weight check (weight > 0)
);

create index idx_playbook_checklist_setup_id on playbook_checklist_item(setup_id);
create index idx_playbook_checklist_type on playbook_checklist_item(checklist_type);

create table trade_playbook_assignment (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint references trading_accounts(id) on delete set null,
    trade_id bigint not null references trades(id) on delete cascade,
    setup_id bigint not null references playbook_setup(id) on delete cascade,
    setup_quality_grade varchar(16) not null default 'UNGRADED',
    execution_quality_grade varchar(16) not null default 'UNGRADED',
    compliance_score integer,
    compliance_confidence varchar(20),
    checklist_results_json text not null default '{}',
    rule_violations_json text not null default '[]',
    assignment_source varchar(32) not null default 'MANUAL',
    assignment_confidence integer,
    reviewed_by_user boolean not null default false,
    notes text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_trade_playbook_assignment_trade_primary unique (trade_id),
    constraint chk_trade_playbook_score check (compliance_score is null or (compliance_score >= 0 and compliance_score <= 100))
);

create index idx_trade_playbook_assignment_user_id on trade_playbook_assignment(user_id);
create index idx_trade_playbook_assignment_account_id on trade_playbook_assignment(account_id);
create index idx_trade_playbook_assignment_trade_id on trade_playbook_assignment(trade_id);
create index idx_trade_playbook_assignment_setup_id on trade_playbook_assignment(setup_id);

create table playbook_analysis_snapshot (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint,
    setup_id bigint not null references playbook_setup(id) on delete cascade,
    metrics_json text,
    behavior_comparison_json text,
    health_score integer,
    health_score_confidence varchar(20),
    ai_review_json text,
    ai_model varchar(80),
    prompt_version varchar(40) not null,
    calculation_version varchar(40) not null,
    data_hash varchar(128) not null,
    generated_at timestamp not null default now(),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_playbook_analysis_setup_latest unique (setup_id),
    constraint chk_playbook_health_score check (health_score is null or (health_score >= 0 and health_score <= 100))
);

create index idx_playbook_analysis_user_id on playbook_analysis_snapshot(user_id);
create index idx_playbook_analysis_setup_id on playbook_analysis_snapshot(setup_id);
create index idx_playbook_analysis_generated_at on playbook_analysis_snapshot(generated_at desc);

create table playbook_milestone (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    account_id bigint,
    setup_id bigint references playbook_setup(id) on delete cascade,
    milestone_type varchar(80) not null,
    earned_date date not null default current_date,
    evidence_json text,
    created_at timestamp not null default now(),
    constraint uk_playbook_milestone_once unique (user_id, setup_id, milestone_type)
);

create index idx_playbook_milestone_user_id on playbook_milestone(user_id);
create index idx_playbook_milestone_setup_id on playbook_milestone(setup_id);

create table trade_replays (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    trade_id bigint not null references trades(id) on delete cascade,
    status varchar(40) not null,
    original_broker_symbol varchar(40),
    resolved_provider_symbol varchar(40),
    provider varchar(40),
    candle_interval varchar(20),
    window_start timestamp,
    window_end timestamp,
    eligible_at timestamp,
    entry_timestamp timestamp,
    final_exit_timestamp timestamp,
    generated_at timestamp,
    deterministic_analysis_version varchar(40),
    ai_analysis_version varchar(40),
    failure_code varchar(80),
    sanitized_failure_message text,
    retry_count integer not null default 0,
    next_retry_at timestamp,
    metrics_json text,
    ai_summary text,
    reflection_question text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_trade_replays_user_trade unique (user_id, trade_id),
    constraint ck_trade_replays_window_order check (window_start is null or window_end is null or window_start < window_end)
);

create index idx_trade_replays_user_trade on trade_replays(user_id, trade_id);
create index idx_trade_replays_status_next_retry on trade_replays(status, next_retry_at);
create index idx_trade_replays_eligible_at on trade_replays(eligible_at);

create table market_candles (
    id bigserial primary key,
    provider varchar(40) not null,
    contract_symbol varchar(40) not null,
    candle_interval varchar(20) not null,
    candle_timestamp timestamp not null,
    open numeric(18, 6) not null,
    high numeric(18, 6) not null,
    low numeric(18, 6) not null,
    close numeric(18, 6) not null,
    volume numeric(20, 0) not null,
    constraint uk_market_candles_provider_contract_interval_ts unique (provider, contract_symbol, candle_interval, candle_timestamp),
    constraint ck_market_candles_ohlc check (high >= low)
);

create index idx_market_candles_lookup on market_candles(provider, contract_symbol, candle_interval, candle_timestamp);

create table trade_replay_insights (
    id bigserial primary key,
    replay_id bigint not null references trade_replays(id) on delete cascade,
    insight_type varchar(80) not null,
    event_timestamp timestamp,
    event_price numeric(18, 6),
    severity varchar(20),
    deterministic_title varchar(180) not null,
    deterministic_explanation text,
    supporting_metrics_json text,
    ai_explanation text,
    display_order integer not null,
    created_at timestamp not null default now()
);

create index idx_trade_replay_insights_replay_order on trade_replay_insights(replay_id, display_order);

create table trade_memories (
    id varchar(255) primary key,
    trade_id bigint not null,
    user_id bigint not null,
    vector_point_id varchar(255) not null,
    memory_version integer not null,
    content text not null,
    setup_normalized varchar(120),
    emotion_normalized varchar(120),
    mistake_normalized varchar(120),
    setup_source text,
    emotion_source text,
    mistake_source text,
    entry_reason_summary text,
    exit_reason_summary text,
    market_context_summary text,
    source_review_id bigint,
    content_hash varchar(128) not null,
    embedding_status varchar(30) not null,
    last_error text,
    attempt_count integer not null default 0,
    last_attempted_at timestamp,
    next_retry_at timestamp,
    last_error_code varchar(80),
    last_error_message varchar(500),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    embedded_at timestamp,
    constraint uk_trade_memories_trade_version unique (trade_id, memory_version)
);

create index idx_trade_memories_user_id on trade_memories(user_id);
create index idx_trade_memories_embedding_status on trade_memories(embedding_status);
create index idx_trade_memories_updated_at on trade_memories(updated_at);
create index idx_trade_memories_user_status on trade_memories(user_id, embedding_status);
create index idx_trade_memories_status_retry on trade_memories(embedding_status, next_retry_at);
