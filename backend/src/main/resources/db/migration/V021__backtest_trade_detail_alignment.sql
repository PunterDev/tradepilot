alter table backtest_trades
    add column if not exists session_date date,
    add column if not exists instrument varchar(10),
    add column if not exists continuous_symbol varchar(20),
    add column if not exists setup_time timestamp,
    add column if not exists initial_risk_points numeric(16,6),
    add column if not exists tp1_time timestamp,
    add column if not exists runner_exit_time timestamp,
    add column if not exists runner_exit_price numeric(16,6),
    add column if not exists final_exit_time timestamp,
    add column if not exists final_exit_price numeric(16,6),
    add column if not exists model_a_r numeric(16,6),
    add column if not exists model_b_r numeric(16,6),
    add column if not exists model_a_weighted_r numeric(16,6),
    add column if not exists model_b_weighted_r numeric(16,6),
    add column if not exists weekday_risk_multiplier numeric(8,4),
    add column if not exists ambiguous_intrabar boolean not null default false,
    add column if not exists created_at timestamp not null default now();

create index if not exists idx_backtest_trades_run_session on backtest_trades(backtest_run_id, session_date, entry_time);
