-- Adds user-defined account-level 1R support.
-- Existing accounts remain NULL; TradePilot must not invent historical risk values.

ALTER TABLE trading_accounts
    ADD COLUMN IF NOT EXISTS default_risk_amount numeric(18, 2);

ALTER TABLE trading_accounts
    DROP CONSTRAINT IF EXISTS chk_trading_accounts_default_risk_amount;

ALTER TABLE trading_accounts
    ADD CONSTRAINT chk_trading_accounts_default_risk_amount
    CHECK (default_risk_amount IS NULL OR default_risk_amount > 0);

ALTER TABLE trades
    DROP CONSTRAINT IF EXISTS chk_trades_risk_amount;

ALTER TABLE trades
    ADD CONSTRAINT chk_trades_risk_amount
    CHECK (risk_amount IS NULL OR risk_amount > 0);
