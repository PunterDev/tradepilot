-- TradePilot AI Trading Memory
-- Safe Supabase/PostgreSQL migration. Run after core users/trades/accounts,
-- AI reviews, feature usage, and V2 feature migrations.

CREATE TABLE IF NOT EXISTS trade_memories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_id BIGINT NOT NULL REFERENCES trades(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vector_point_id UUID NOT NULL,
    memory_version INTEGER NOT NULL DEFAULT 1,
    summary TEXT NOT NULL,
    setup_normalized VARCHAR(120),
    emotion_normalized VARCHAR(120),
    mistake_normalized VARCHAR(120),
    setup_source TEXT,
    emotion_source TEXT,
    mistake_source TEXT,
    entry_reason_summary TEXT,
    exit_reason_summary TEXT,
    market_context_summary TEXT,
    source_review_id BIGINT NULL REFERENCES ai_trade_reviews(id) ON DELETE SET NULL,
    content_hash VARCHAR(128) NOT NULL,
    embedding_status VARCHAR(30) NOT NULL,
    last_error TEXT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempted_at TIMESTAMPTZ NULL,
    next_retry_at TIMESTAMPTZ NULL,
    last_error_code VARCHAR(80) NULL,
    last_error_message VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    embedded_at TIMESTAMPTZ NULL,
    CONSTRAINT uk_trade_memories_trade_id UNIQUE (trade_id),
    CONSTRAINT uk_trade_memories_vector_point_id UNIQUE (vector_point_id),
    CONSTRAINT chk_trade_memories_embedding_status CHECK (embedding_status IN ('PENDING', 'INDEXED', 'FAILED', 'STALE')),
    CONSTRAINT chk_trade_memories_last_error_code CHECK (
        last_error_code IS NULL
        OR last_error_code IN (
            'QDRANT_UNAVAILABLE',
            'QDRANT_CONFIGURATION_INVALID',
            'EMBEDDING_PROVIDER_UNAVAILABLE',
            'EMBEDDING_AUTHENTICATION_FAILED',
            'EMBEDDING_DIMENSION_MISMATCH',
            'TIMEOUT',
            'RATE_LIMITED',
            'UNKNOWN'
        )
    )
);

CREATE INDEX IF NOT EXISTS idx_trade_memories_user_id
    ON trade_memories(user_id);

CREATE INDEX IF NOT EXISTS idx_trade_memories_embedding_status
    ON trade_memories(embedding_status);

CREATE INDEX IF NOT EXISTS idx_trade_memories_updated_at
    ON trade_memories(updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_trade_memories_user_status
    ON trade_memories(user_id, embedding_status);

CREATE INDEX IF NOT EXISTS idx_trade_memories_status_retry
    ON trade_memories(embedding_status, next_retry_at);

ALTER TABLE feature_usage_events
DROP CONSTRAINT IF EXISTS feature_usage_events_feature_check;

ALTER TABLE feature_usage_events
DROP CONSTRAINT IF EXISTS chk_feature_usage_events_feature;

ALTER TABLE feature_usage_events
ADD CONSTRAINT chk_feature_usage_events_feature
CHECK (
    feature IN (
        'AI_REVIEW',
        'DAILY_COACH',
        'WEEKLY_COACH',
        'TRADING_DNA',
        'AI_LEAK_DETECTOR',
        'PERFORMANCE_TIMELINE',
        'PLAYBOOK',
        'TRADING_MEMORY',
        'SESSION_REVIEW',
        'TRADE_DETAIL',
        'CALENDAR_VIEW',
        'ANALYTICS_VIEW'
    )
);
