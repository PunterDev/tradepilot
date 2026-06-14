-- AI credit ledger for monthly pooled plan limits.
-- Run after the core users table migration.

CREATE TABLE IF NOT EXISTS ai_credit_usage (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature VARCHAR(40) NOT NULL,
    credits INTEGER NOT NULL CHECK (credits > 0),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    request_id BIGINT,
    metadata TEXT
);

CREATE INDEX IF NOT EXISTS idx_ai_credit_usage_user_id
    ON ai_credit_usage(user_id);

CREATE INDEX IF NOT EXISTS idx_ai_credit_usage_created_at
    ON ai_credit_usage(created_at);

CREATE INDEX IF NOT EXISTS idx_ai_credit_usage_user_created
    ON ai_credit_usage(user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_ai_credit_usage_user_feature_created
    ON ai_credit_usage(user_id, feature, created_at);
