-- A canonical setup now records independent Model A and Model B results.
-- Preserve historical single-exit values, but do not require them for new rows.
alter table backtest_trades
    alter column raw_r drop not null;

alter table backtest_trades
    alter column weighted_r drop not null;

alter table backtest_trades
    alter column outcome drop not null;

alter table backtest_trades
    alter column exit_model drop not null;

-- V020/V021 ran after the global RLS migration, so make the new private-research
-- tables part of the same database security baseline.
alter table backtest_runs enable row level security;

alter table backtest_trades enable row level security;
