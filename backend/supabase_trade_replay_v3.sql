-- AI Trade Replay V3 persistence.
-- Run after the base trade/account/user migrations.

create table if not exists trade_replays (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    trade_id bigint not null references trades(id) on delete cascade,
    status text not null,
    original_broker_symbol varchar(40),
    resolved_provider_symbol varchar(40),
    provider varchar(40),
    candle_interval varchar(20),
    window_start timestamptz,
    window_end timestamptz,
    eligible_at timestamptz,
    entry_timestamp timestamptz,
    final_exit_timestamp timestamptz,
    generated_at timestamptz,
    deterministic_analysis_version varchar(40),
    ai_analysis_version varchar(40),
    failure_code varchar(80),
    sanitized_failure_message text,
    retry_count integer not null default 0,
    next_retry_at timestamptz,
    metrics_json text,
    ai_summary text,
    reflection_question text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_trade_replays_user_trade unique (user_id, trade_id),
    constraint ck_trade_replays_status check (status in (
        'PENDING',
        'WAITING_FOR_MARKET_DATA',
        'FETCHING_DATA',
        'ANALYZING',
        'READY',
        'PROVIDER_NOT_CONFIGURED',
        'UNSUPPORTED_INSTRUMENT',
        'CONTRACT_RESOLUTION_FAILED',
        'NOT_ENOUGH_DATA',
        'DEMO_REPLAY_UNAVAILABLE',
        'FAILED'
    )),
    constraint ck_trade_replays_window_order check (window_start is null or window_end is null or window_start < window_end)
);

create index if not exists idx_trade_replays_user_trade on trade_replays(user_id, trade_id);
create index if not exists idx_trade_replays_status_next_retry on trade_replays(status, next_retry_at);
create index if not exists idx_trade_replays_eligible_at on trade_replays(eligible_at);

create table if not exists market_candles (
    id bigserial primary key,
    provider varchar(40) not null,
    contract_symbol varchar(40) not null,
    candle_interval varchar(20) not null,
    candle_timestamp timestamptz not null,
    open numeric(18,6) not null,
    high numeric(18,6) not null,
    low numeric(18,6) not null,
    close numeric(18,6) not null,
    volume numeric(20,0) not null,
    constraint uk_market_candles_provider_contract_interval_ts unique (provider, contract_symbol, candle_interval, candle_timestamp),
    constraint ck_market_candles_ohlc check (high >= low)
);

create index if not exists idx_market_candles_lookup
    on market_candles(provider, contract_symbol, candle_interval, candle_timestamp);

create table if not exists trade_replay_insights (
    id bigserial primary key,
    replay_id bigint not null references trade_replays(id) on delete cascade,
    insight_type text not null,
    event_timestamp timestamptz,
    event_price numeric(18,6),
    severity varchar(20),
    deterministic_title varchar(180) not null,
    deterministic_explanation text,
    supporting_metrics_json text,
    ai_explanation text,
    display_order integer not null,
    created_at timestamptz not null default now(),
    constraint ck_trade_replay_insights_type check (insight_type in (
        'ENTRY',
        'PARTIAL_EXIT',
        'FINAL_EXIT',
        'MFE',
        'MAE',
        'STRONG_EXCURSION_CAPTURE',
        'GOOD_EXCURSION_CAPTURE',
        'MODERATE_EXCURSION_CAPTURE',
        'LARGE_PROFIT_GIVEBACK',
        'WINNER_TURNED_LOSER',
        'RISK_EFFICIENCY',
        'POST_EXIT_CONTINUATION',
        'EXIT_NEAR_END_OF_MOVE',
        'TARGET_REACHED_AFTER_EXIT',
        'STOP_LEVEL_TRADED_DURING_POSITION',
        'STOP_LEVEL_TRADED_AFTER_EXIT',
        'STOP_LEVEL_TRADED_BEFORE_ENTRY',
        'STOPPED_THEN_RECOVERED',
        'LOW_EXCURSION_CAPTURE',
        'EXTENDED_ENTRY',
        'OVERTRADING_CLUSTER',
        'OUTSIDE_TRADING_WINDOW',
        'DATA_QUALITY_WARNING'
    ))
);

create index if not exists idx_trade_replay_insights_replay_order
    on trade_replay_insights(replay_id, display_order);
