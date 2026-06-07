-- Trading DNA cache/input-hash performance support.
-- Safe to run repeatedly in Supabase/Postgres.

CREATE TABLE IF NOT EXISTS trading_dna_snapshots (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id bigint NULL REFERENCES trading_accounts(id) ON DELETE CASCADE,
    days integer NOT NULL,
    include_all_accounts boolean NOT NULL,
    data_hash varchar(128) NOT NULL,
    status varchar(32) NOT NULL,
    total_trades integer NULL,
    ai_response_json text NULL,
    context_json text NULL,
    timeline_json text NULL,
    model varchar(80) NULL,
    prompt_tokens integer NULL,
    completion_tokens integer NULL,
    total_tokens integer NULL,
    estimated_cost numeric(12, 6) NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

ALTER TABLE trading_dna_snapshots
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS account_id bigint REFERENCES trading_accounts(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS days integer,
    ADD COLUMN IF NOT EXISTS include_all_accounts boolean,
    ADD COLUMN IF NOT EXISTS data_hash varchar(128),
    ADD COLUMN IF NOT EXISTS status varchar(32),
    ADD COLUMN IF NOT EXISTS total_trades integer,
    ADD COLUMN IF NOT EXISTS ai_response_json text,
    ADD COLUMN IF NOT EXISTS context_json text,
    ADD COLUMN IF NOT EXISTS timeline_json text,
    ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT now();

ALTER TABLE trading_dna_snapshots
    ADD COLUMN IF NOT EXISTS model varchar(80);

ALTER TABLE trading_dna_snapshots
    ADD COLUMN IF NOT EXISTS prompt_tokens integer;

ALTER TABLE trading_dna_snapshots
    ADD COLUMN IF NOT EXISTS completion_tokens integer;

ALTER TABLE trading_dna_snapshots
    ADD COLUMN IF NOT EXISTS total_tokens integer;

ALTER TABLE trading_dna_snapshots
    ADD COLUMN IF NOT EXISTS estimated_cost numeric(12, 6);

CREATE INDEX IF NOT EXISTS idx_trading_dna_user_id
    ON trading_dna_snapshots(user_id);

CREATE INDEX IF NOT EXISTS idx_trading_dna_user_account_timeframe
    ON trading_dna_snapshots(user_id, account_id, days);

CREATE INDEX IF NOT EXISTS idx_trading_dna_data_hash
    ON trading_dna_snapshots(data_hash);

CREATE INDEX IF NOT EXISTS idx_trading_dna_user_account_hash
    ON trading_dna_snapshots(user_id, account_id, data_hash);

CREATE INDEX IF NOT EXISTS idx_trading_dna_user_account_days_hash
    ON trading_dna_snapshots(user_id, account_id, days, include_all_accounts, data_hash);

CREATE INDEX IF NOT EXISTS idx_trading_dna_user_created_at
    ON trading_dna_snapshots(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_trades_user_account_trade_updated
    ON trades(user_id, account_id, trade_date, updated_at);

CREATE INDEX IF NOT EXISTS idx_trades_user_account_date
    ON trades(user_id, account_id, trade_date);

CREATE INDEX IF NOT EXISTS idx_trades_user_account_date_entry
    ON trades(user_id, account_id, trade_date, entry_time);

CREATE INDEX IF NOT EXISTS idx_trades_user_trade_updated
    ON trades(user_id, trade_date, updated_at);
