-- User-scoped data isolation support for Supabase/Postgres.
-- Run once in environments that already had TradePilot tables before this change.

-- Old builds created a global unique key on trades.external_trade_key. Drop it
-- so two different users can import the same broker export without colliding.
ALTER TABLE trades DROP CONSTRAINT IF EXISTS trades_external_trade_key_key;

CREATE INDEX IF NOT EXISTS idx_trading_accounts_user_id
    ON trading_accounts (user_id);

CREATE INDEX IF NOT EXISTS idx_import_batches_user_id
    ON import_batches (user_id);

CREATE INDEX IF NOT EXISTS idx_import_batches_account_id
    ON import_batches (account_id);

CREATE INDEX IF NOT EXISTS idx_trades_user_id
    ON trades (user_id);

CREATE INDEX IF NOT EXISTS idx_trades_account_id
    ON trades (account_id);

CREATE INDEX IF NOT EXISTS idx_trades_user_account_date
    ON trades (user_id, account_id, trade_date);

CREATE INDEX IF NOT EXISTS idx_trades_user_entry_time
    ON trades (user_id, entry_time);

CREATE INDEX IF NOT EXISTS idx_trades_account_entry_time
    ON trades (account_id, entry_time);

CREATE INDEX IF NOT EXISTS idx_trades_user_account_entry_time
    ON trades (user_id, account_id, entry_time);

CREATE UNIQUE INDEX IF NOT EXISTS uk_trades_user_account_external_key
    ON trades (user_id, account_id, external_trade_key)
    WHERE external_trade_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ai_trade_reviews_user_id
    ON ai_trade_reviews (user_id);

CREATE INDEX IF NOT EXISTS idx_ai_trade_reviews_trade_id
    ON ai_trade_reviews (trade_id);

CREATE INDEX IF NOT EXISTS idx_ai_trade_reviews_cache
    ON ai_trade_reviews (user_id, trade_id, review_version, review_hash);

DELETE FROM ai_trade_reviews duplicate
USING ai_trade_reviews keeper
WHERE duplicate.id > keeper.id
  AND duplicate.user_id = keeper.user_id
  AND duplicate.trade_id = keeper.trade_id
  AND duplicate.review_version = keeper.review_version
  AND duplicate.review_hash = keeper.review_hash
  AND duplicate.trade_id IS NOT NULL
  AND duplicate.review_version IS NOT NULL
  AND duplicate.review_hash IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_trade_reviews_cache
    ON ai_trade_reviews (user_id, trade_id, review_version, review_hash);

ALTER TABLE ai_trade_analyses DROP CONSTRAINT IF EXISTS uk_ai_trade_analysis_cache;

CREATE INDEX IF NOT EXISTS idx_ai_trade_analysis_user_id
    ON ai_trade_analyses (user_id);

CREATE INDEX IF NOT EXISTS idx_ai_trade_analysis_trade_id
    ON ai_trade_analyses (trade_id);

CREATE INDEX IF NOT EXISTS idx_ai_trade_analysis_hash
    ON ai_trade_analyses (review_hash);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_trade_analysis_cache
    ON ai_trade_analyses (user_id, trade_id, review_version, review_hash);

CREATE INDEX IF NOT EXISTS idx_ai_coach_snapshot_user_id
    ON ai_coach_snapshot (user_id);

CREATE INDEX IF NOT EXISTS idx_ai_coach_snapshot_account_id
    ON ai_coach_snapshot (account_id);

CREATE INDEX IF NOT EXISTS idx_ai_coach_snapshot_user_account_type_period
    ON ai_coach_snapshot (user_id, account_id, snapshot_type, period_start, period_end);

CREATE INDEX IF NOT EXISTS idx_daily_discipline_scores_user_id
    ON daily_discipline_scores (user_id);

CREATE INDEX IF NOT EXISTS idx_daily_discipline_scores_user_account_date
    ON daily_discipline_scores (user_id, account_id, trade_date);

CREATE INDEX IF NOT EXISTS idx_trade_rule_violations_user_id
    ON trade_rule_violations (user_id);

CREATE INDEX IF NOT EXISTS idx_trade_rule_violations_trade_id
    ON trade_rule_violations (trade_id);

CREATE INDEX IF NOT EXISTS idx_trade_rule_violations_user_trade
    ON trade_rule_violations (user_id, trade_id);

CREATE INDEX IF NOT EXISTS idx_journal_entries_user_id
    ON journal_entries (user_id);

CREATE INDEX IF NOT EXISTS idx_journal_entries_user_account_date
    ON journal_entries (user_id, account_id, journal_date);

CREATE INDEX IF NOT EXISTS idx_ai_reflections_user_id
    ON ai_reflections (user_id);

CREATE INDEX IF NOT EXISTS idx_ai_reflections_user_period
    ON ai_reflections (user_id, start_date, end_date);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified boolean NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS auth_tokens (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL,
    type varchar(40) NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_auth_tokens_user_id
    ON auth_tokens (user_id);

CREATE INDEX IF NOT EXISTS idx_auth_tokens_token_hash
    ON auth_tokens (token_hash);

CREATE INDEX IF NOT EXISTS idx_auth_tokens_type
    ON auth_tokens (type);

CREATE INDEX IF NOT EXISTS idx_auth_tokens_expires_at
    ON auth_tokens (expires_at);

CREATE INDEX IF NOT EXISTS idx_auth_tokens_lookup
    ON auth_tokens (token_hash, type, expires_at);

ALTER TABLE auth_tokens DROP CONSTRAINT IF EXISTS chk_auth_tokens_type;
ALTER TABLE auth_tokens
    ADD CONSTRAINT chk_auth_tokens_type
    CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'));
