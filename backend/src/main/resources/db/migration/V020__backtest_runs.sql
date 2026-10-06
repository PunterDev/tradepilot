create table backtest_runs (
    id bigserial primary key,
    user_id bigint not null references users(id),
    instrument varchar(10) not null,
    strategy_version varchar(40) not null,
    start_date date not null,
    end_date date not null,
    config_json jsonb not null,
    result_json jsonb,
    status varchar(20) not null,
    error_message varchar(500),
    started_at timestamp,
    completed_at timestamp,
    total_trades integer,
    win_rate numeric(10,6),
    total_r numeric(16,6),
    weighted_total_r numeric(16,6),
    expectancy numeric(16,6),
    max_drawdown_r numeric(16,6),
    created_at timestamp not null
);

create index idx_backtest_runs_user_created on backtest_runs(user_id, created_at desc);
create index idx_backtest_runs_status on backtest_runs(status);

create table backtest_trades (
    id bigserial primary key,
    backtest_run_id bigint not null references backtest_runs(id) on delete cascade,
    trade_date date not null,
    direction varchar(8) not null,
    entry_time timestamp not null,
    entry_price numeric(16,6) not null,
    stop_price numeric(16,6) not null,
    tp1_price numeric(16,6),
    runner_target_price numeric(16,6),
    exit_time timestamp,
    exit_price numeric(16,6),
    raw_r numeric(16,6) not null,
    weighted_r numeric(16,6) not null,
    outcome varchar(20) not null,
    exit_model varchar(20) not null,
    instrument_id bigint not null,
    setup_metadata_json jsonb not null
);

create index idx_backtest_trades_run on backtest_trades(backtest_run_id, entry_time);
