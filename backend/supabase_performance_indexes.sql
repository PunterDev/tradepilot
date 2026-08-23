-- TradePilot beta performance indexes
-- Safe to run in Supabase after the core schema, feature usage, coach, leak detector,
-- Trading DNA, credit pack, and observability migrations have created their tables.
--
-- Strategy:
-- - Keep composite indexes aligned to user/account/date filters used by dashboards,
--   analytics, CSV import duplicate checks, and AI cache lookups.
-- - Avoid broad duplicate single-column indexes already created by earlier migrations.
-- - Prefer idempotent CREATE INDEX IF NOT EXISTS so this can be applied safely.

-- CSV import duplicate detection and account/date analytics.
CREATE INDEX IF NOT EXISTS idx_trades_user_account_external_key_present
    ON trades(user_id, account_id, external_trade_key)
    WHERE external_trade_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_trades_user_account_date_pnl
    ON trades(user_id, account_id, trade_date, pnl_amount);

CREATE INDEX IF NOT EXISTS idx_trades_user_account_symbol_date
    ON trades(user_id, account_id, symbol, trade_date);

CREATE INDEX IF NOT EXISTS idx_trades_user_account_setup_date
    ON trades(user_id, account_id, setup, trade_date);

CREATE INDEX IF NOT EXISTS idx_trades_user_account_session_date
    ON trades(user_id, account_id, session, trade_date);

CREATE INDEX IF NOT EXISTS idx_trades_user_account_side_date
    ON trades(user_id, account_id, side, trade_date);

-- Account selectors and account management pages.
CREATE INDEX IF NOT EXISTS idx_trading_accounts_user_created_at
    ON trading_accounts(user_id, created_at DESC);

-- Import history and cleanup previews.
CREATE INDEX IF NOT EXISTS idx_import_batches_user_account_imported_at
    ON import_batches(user_id, account_id, imported_at DESC);

-- AI cache and admin review lookups.
CREATE INDEX IF NOT EXISTS idx_ai_trade_reviews_user_account_created_at
    ON ai_trade_reviews(user_id, account_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_trade_analyses_user_created_at
    ON ai_trade_analyses(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_session_reviews_user_account_created_at
    ON ai_session_reviews(user_id, account_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_coach_snapshot_user_account_status_updated
    ON ai_coach_snapshot(user_id, account_id, status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_trading_dna_snapshots_user_account_updated
    ON trading_dna_snapshots(user_id, account_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_leak_detector_snapshots_user_account_updated_desc
    ON ai_leak_detector_snapshots(user_id, account_id, updated_at DESC);

-- Credit pack purchase history and support investigations.
CREATE INDEX IF NOT EXISTS idx_ai_credit_purchases_user_completed_at
    ON ai_credit_purchases(user_id, completed_at DESC);
