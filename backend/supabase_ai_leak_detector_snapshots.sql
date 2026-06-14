CREATE TABLE IF NOT EXISTS ai_leak_detector_snapshots (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL REFERENCES trading_accounts(id) ON DELETE CASCADE,
    data_hash VARCHAR(128) NOT NULL,
    total_trades INTEGER NOT NULL,
    result_json TEXT NOT NULL,
    model VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_leak_detector_user_id
    ON ai_leak_detector_snapshots(user_id);

CREATE INDEX IF NOT EXISTS idx_ai_leak_detector_user_account_hash
    ON ai_leak_detector_snapshots(user_id, account_id, data_hash);

CREATE INDEX IF NOT EXISTS idx_ai_leak_detector_user_account_updated
    ON ai_leak_detector_snapshots(user_id, account_id, updated_at);
