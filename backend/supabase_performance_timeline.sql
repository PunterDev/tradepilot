-- TradePilot AI Performance Timeline
-- Safe Supabase migration for monthly Journey snapshots and deterministic milestones.

CREATE TABLE IF NOT EXISTS performance_timeline_snapshots (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL REFERENCES trading_accounts(id) ON DELETE CASCADE,
    period_type VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    data_status VARCHAR(30) NOT NULL,
    trade_count INTEGER NOT NULL DEFAULT 0,
    metrics_json TEXT,
    comparison_json TEXT,
    category_scores_json TEXT,
    evolution_score INTEGER,
    score_confidence VARCHAR(20),
    narrative_json TEXT,
    ai_model VARCHAR(80),
    prompt_version VARCHAR(40),
    data_hash VARCHAR(128) NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_performance_timeline_account_period UNIQUE (account_id, period_type, period_start),
    CONSTRAINT performance_timeline_data_status_check CHECK (data_status IN ('READY', 'LIMITED_DATA', 'NO_ACTIVITY')),
    CONSTRAINT performance_timeline_score_confidence_check CHECK (score_confidence IS NULL OR score_confidence IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT performance_timeline_score_range_check CHECK (evolution_score IS NULL OR (evolution_score >= 0 AND evolution_score <= 100))
);

CREATE INDEX IF NOT EXISTS idx_performance_timeline_user_account
    ON performance_timeline_snapshots(user_id, account_id);

CREATE INDEX IF NOT EXISTS idx_performance_timeline_account_period
    ON performance_timeline_snapshots(account_id, period_start DESC);

CREATE TABLE IF NOT EXISTS performance_milestones (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL REFERENCES trading_accounts(id) ON DELETE CASCADE,
    milestone_type VARCHAR(80) NOT NULL,
    earned_date DATE NOT NULL,
    period_start DATE,
    period_end DATE,
    evidence_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_performance_milestone_account_type UNIQUE (account_id, milestone_type)
);

CREATE INDEX IF NOT EXISTS idx_performance_milestones_user_account
    ON performance_milestones(user_id, account_id, earned_date DESC);

ALTER TABLE ai_usage_events
DROP CONSTRAINT IF EXISTS ai_usage_events_usage_type_check;

ALTER TABLE ai_usage_events
ADD CONSTRAINT ai_usage_events_usage_type_check
CHECK (
    usage_type IN (
        'AI_TRADE_REVIEW',
        'TRADE_REVIEW',
        'SESSION_REVIEW',
        'DAILY_COACH',
        'WEEKLY_COACH',
        'TRADING_DNA',
        'AI_LEAK_DETECTOR',
        'PERFORMANCE_TIMELINE'
    )
);

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
        'SESSION_REVIEW',
        'TRADE_DETAIL',
        'CALENDAR_VIEW',
        'ANALYTICS_VIEW'
    )
);
