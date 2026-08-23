-- Preserve exact broker futures contracts for AI Trade Replay.
-- Run after the base trades table exists and before broad Trade Replay backfills.

alter table trades
    add column if not exists original_broker_symbol varchar(80);

alter table trades
    add column if not exists contract_symbol varchar(40);

alter table trades
    add column if not exists contract_symbol_source varchar(30);

alter table trades
    add column if not exists contract_symbol_confirmed_at timestamptz;

alter table trades
    drop constraint if exists ck_trades_contract_symbol_source;

alter table trades
    add constraint ck_trades_contract_symbol_source
    check (
        contract_symbol_source is null
        or contract_symbol_source in ('IMPORTED', 'NORMALIZED', 'USER_CONFIRMED', 'ADMIN_BACKFILL')
    );

create index if not exists idx_trades_contract_symbol
    on trades(contract_symbol);

create index if not exists idx_trades_original_broker_symbol
    on trades(original_broker_symbol);
