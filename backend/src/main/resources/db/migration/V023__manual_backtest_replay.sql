create table manual_backtest_sessions (
    id bigserial primary key,
    user_id bigint not null references users(id),
    symbol varchar(10) not null,
    continuous_symbol varchar(20) not null,
    trading_date date not null,
    timezone varchar(64) not null,
    start_time timestamptz not null,
    end_time timestamptz not null,
    cursor_ts timestamptz not null,
    status varchar(20) not null,
    risk_profile varchar(32) not null,
    notes text,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    completed_at timestamptz,
    constraint ck_manual_backtest_session_window check (end_time > start_time),
    constraint ck_manual_backtest_session_status check (status in ('ACTIVE', 'COMPLETED', 'ABANDONED')),
    constraint ck_manual_backtest_risk_profile check (risk_profile in ('UNIFORM_1R', 'PLAYBOOK_WEEKDAY'))
);

create index idx_manual_backtest_sessions_user_created on manual_backtest_sessions(user_id, created_at desc);
create index idx_manual_backtest_sessions_active on manual_backtest_sessions(user_id, status, updated_at desc);

create table manual_backtest_trades (
    id bigserial primary key,
    session_id bigint not null references manual_backtest_sessions(id) on delete cascade,
    user_id bigint not null references users(id),
    symbol varchar(10) not null,
    instrument_id bigint,
    direction varchar(8) not null,
    order_placed_ts timestamptz not null,
    entry_ts timestamptz,
    entry_price numeric(16, 6),
    initial_stop_price numeric(16, 6) not null,
    current_stop_price numeric(16, 6) not null,
    target_price numeric(16, 6) not null,
    exit_ts timestamptz,
    exit_price numeric(16, 6),
    exit_reason varchar(20),
    initial_risk_points numeric(16, 6),
    raw_r numeric(16, 6),
    risk_multiplier numeric(8, 4) not null,
    weighted_r numeric(16, 6),
    status varchar(20) not null,
    pending_action varchar(20),
    notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_manual_backtest_trade_direction check (direction in ('LONG', 'SHORT')),
    constraint ck_manual_backtest_trade_status check (status in ('PENDING', 'OPEN', 'CLOSED', 'CANCELLED')),
    constraint ck_manual_backtest_exit_reason check (exit_reason is null or exit_reason in ('STOP', 'TARGET', 'MANUAL', 'SESSION_END'))
);

create index idx_manual_backtest_trades_session on manual_backtest_trades(session_id, created_at);
create index idx_manual_backtest_trades_user on manual_backtest_trades(user_id, created_at desc);

create table manual_backtest_trade_events (
    id bigserial primary key,
    trade_id bigint not null references manual_backtest_trades(id) on delete cascade,
    session_id bigint not null references manual_backtest_sessions(id) on delete cascade,
    user_id bigint not null references users(id),
    replay_cursor_ts timestamptz not null,
    event_type varchar(40) not null,
    old_price numeric(16, 6),
    new_price numeric(16, 6),
    created_at timestamptz not null default now()
);

create index idx_manual_backtest_trade_events_trade on manual_backtest_trade_events(trade_id, created_at);

alter table manual_backtest_sessions enable row level security;
alter table manual_backtest_trades enable row level security;
alter table manual_backtest_trade_events enable row level security;
