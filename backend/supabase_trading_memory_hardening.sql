-- Production hardening for AI Trading Memory indexing operations.
-- Safe to run after backend/supabase_trading_memory.sql.

ALTER TABLE trade_memories
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_attempted_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS last_error_code VARCHAR(80) NULL,
    ADD COLUMN IF NOT EXISTS last_error_message VARCHAR(500) NULL;

ALTER TABLE trade_memories
DROP CONSTRAINT IF EXISTS chk_trade_memories_last_error_code;

ALTER TABLE trade_memories
ADD CONSTRAINT chk_trade_memories_last_error_code
CHECK (
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
);

CREATE INDEX IF NOT EXISTS idx_trade_memories_status_retry
    ON trade_memories(embedding_status, next_retry_at);
