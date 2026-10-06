alter table manual_backtest_trades
    add column if not exists entry_order_type varchar(10) not null default 'MARKET',
    add column if not exists configured_entry_price numeric(16, 6);

alter table manual_backtest_trades
    add constraint ck_manual_backtest_entry_order_type
    check (entry_order_type in ('MARKET', 'LIMIT', 'STOP'));
