create table if not exists market_bars (
    id bigserial primary key,
    symbol varchar(10) not null,
    continuous_symbol varchar(20) not null,
    source varchar(32) not null default 'DATABENTO',
    instrument_id bigint not null,
    timeframe varchar(10) not null,
    ts_event timestamptz not null,
    open numeric(16, 6) not null,
    high numeric(16, 6) not null,
    low numeric(16, 6) not null,
    close numeric(16, 6) not null,
    volume bigint not null,
    created_at timestamptz not null default now(),
    constraint uk_market_bar unique (continuous_symbol, timeframe, ts_event)
);

create index if not exists idx_market_bars_lookup
    on market_bars (continuous_symbol, timeframe, ts_event);

create index if not exists idx_market_bars_instrument
    on market_bars (instrument_id, ts_event);

create table if not exists market_data_import_chunks (
    id bigserial primary key,
    symbol varchar(10) not null,
    continuous_symbol varchar(20) not null,
    timeframe varchar(10) not null,
    chunk_start timestamptz not null,
    chunk_end timestamptz not null,
    records_received bigint not null,
    inserted_bars bigint not null,
    completed_at timestamptz not null default now(),
    constraint uk_market_data_import_chunk unique (continuous_symbol, timeframe, chunk_start, chunk_end)
);

create index if not exists idx_market_data_import_chunks_lookup
    on market_data_import_chunks (continuous_symbol, timeframe, chunk_start, chunk_end);

alter table market_bars enable row level security;
alter table market_data_import_chunks enable row level security;
