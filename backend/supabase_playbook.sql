-- TradePilot AI Trading Playbook
-- Safe, additive Supabase/PostgreSQL migration.

CREATE TABLE IF NOT EXISTS playbook_setup (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id BIGINT REFERENCES trading_accounts(id) ON DELETE SET NULL,
    name VARCHAR(120) NOT NULL,
    short_description VARCHAR(280),
    description TEXT,
    category VARCHAR(40) NOT NULL DEFAULT 'CUSTOM',
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    direction VARCHAR(16) NOT NULL DEFAULT 'BOTH',
    instruments_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    sessions_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    timeframes_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    tags_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    market_context_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    risk_rules_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    management_rules_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    post_trade_rules_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    icon_key VARCHAR(60),
    color_key VARCHAR(60),
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    archived_at TIMESTAMP,
    version_number INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT chk_playbook_setup_status CHECK (status IN ('DRAFT','TESTING','VALIDATED','PAUSED','ARCHIVED')),
    CONSTRAINT chk_playbook_setup_direction CHECK (direction IN ('LONG','SHORT','BOTH')),
    CONSTRAINT chk_playbook_setup_category CHECK (category IN ('CONTINUATION','REVERSAL','BREAKOUT','PULLBACK','LIQUIDITY_SWEEP','FVG','IFVG','OPENING_RANGE','TREND_FOLLOWING','MEAN_REVERSION','CUSTOM'))
);

CREATE TABLE IF NOT EXISTS playbook_checklist_item (
    id BIGSERIAL PRIMARY KEY,
    setup_id BIGINT NOT NULL REFERENCES playbook_setup(id) ON DELETE CASCADE,
    checklist_type VARCHAR(32) NOT NULL,
    label VARCHAR(180) NOT NULL,
    description TEXT,
    is_required BOOLEAN NOT NULL DEFAULT true,
    weight INTEGER NOT NULL DEFAULT 1,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    expected_value_type VARCHAR(32),
    expected_value_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_playbook_checklist_type CHECK (checklist_type IN ('CONTEXT','ENTRY','INVALIDATION','RISK','MANAGEMENT','POST_TRADE')),
    CONSTRAINT chk_playbook_checklist_weight CHECK (weight > 0)
);

CREATE TABLE IF NOT EXISTS trade_playbook_assignment (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id BIGINT REFERENCES trading_accounts(id) ON DELETE SET NULL,
    trade_id BIGINT NOT NULL REFERENCES trades(id) ON DELETE CASCADE,
    setup_id BIGINT NOT NULL REFERENCES playbook_setup(id) ON DELETE CASCADE,
    setup_quality_grade VARCHAR(16) NOT NULL DEFAULT 'UNGRADED',
    execution_quality_grade VARCHAR(16) NOT NULL DEFAULT 'UNGRADED',
    compliance_score INTEGER,
    compliance_confidence VARCHAR(20),
    checklist_results_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    rule_violations_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    assignment_source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    assignment_confidence INTEGER,
    reviewed_by_user BOOLEAN NOT NULL DEFAULT false,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_trade_playbook_assignment_trade_primary UNIQUE (trade_id),
    CONSTRAINT chk_trade_playbook_setup_grade CHECK (setup_quality_grade IN ('A','B','C','D','F','UNGRADED')),
    CONSTRAINT chk_trade_playbook_execution_grade CHECK (execution_quality_grade IN ('A','B','C','D','F','UNGRADED')),
    CONSTRAINT chk_trade_playbook_confidence CHECK (compliance_confidence IS NULL OR compliance_confidence IN ('INSUFFICIENT','LOW','MEDIUM','HIGH')),
    CONSTRAINT chk_trade_playbook_source CHECK (assignment_source IN ('MANUAL','BULK_MANUAL','AI_SUGGESTED','IMPORTED','SYSTEM_INFERRED')),
    CONSTRAINT chk_trade_playbook_score CHECK (compliance_score IS NULL OR (compliance_score >= 0 AND compliance_score <= 100))
);

CREATE TABLE IF NOT EXISTS playbook_analysis_snapshot (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id BIGINT,
    setup_id BIGINT NOT NULL REFERENCES playbook_setup(id) ON DELETE CASCADE,
    metrics_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    behavior_comparison_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    health_score INTEGER,
    health_score_confidence VARCHAR(20),
    ai_review_json JSONB,
    ai_model VARCHAR(80),
    prompt_version VARCHAR(40) NOT NULL,
    calculation_version VARCHAR(40) NOT NULL,
    data_hash VARCHAR(128) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_playbook_analysis_setup_latest UNIQUE (setup_id),
    CONSTRAINT chk_playbook_health_score CHECK (health_score IS NULL OR (health_score >= 0 AND health_score <= 100)),
    CONSTRAINT chk_playbook_health_confidence CHECK (health_score_confidence IS NULL OR health_score_confidence IN ('INSUFFICIENT','LOW','MEDIUM','HIGH'))
);

CREATE TABLE IF NOT EXISTS playbook_milestone (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id BIGINT,
    setup_id BIGINT REFERENCES playbook_setup(id) ON DELETE CASCADE,
    milestone_type VARCHAR(80) NOT NULL,
    earned_date DATE NOT NULL DEFAULT CURRENT_DATE,
    evidence_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_playbook_milestone_once UNIQUE (user_id, setup_id, milestone_type)
);

CREATE INDEX IF NOT EXISTS idx_playbook_setup_user_id ON playbook_setup(user_id);
CREATE INDEX IF NOT EXISTS idx_playbook_setup_account_id ON playbook_setup(account_id);
CREATE INDEX IF NOT EXISTS idx_playbook_setup_status ON playbook_setup(status);
CREATE INDEX IF NOT EXISTS idx_playbook_setup_updated_at ON playbook_setup(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_playbook_checklist_setup_id ON playbook_checklist_item(setup_id);
CREATE INDEX IF NOT EXISTS idx_trade_playbook_assignment_user_id ON trade_playbook_assignment(user_id);
CREATE INDEX IF NOT EXISTS idx_trade_playbook_assignment_account_id ON trade_playbook_assignment(account_id);
CREATE INDEX IF NOT EXISTS idx_trade_playbook_assignment_trade_id ON trade_playbook_assignment(trade_id);
CREATE INDEX IF NOT EXISTS idx_trade_playbook_assignment_setup_id ON trade_playbook_assignment(setup_id);
CREATE INDEX IF NOT EXISTS idx_playbook_analysis_user_id ON playbook_analysis_snapshot(user_id);
CREATE INDEX IF NOT EXISTS idx_playbook_analysis_setup_id ON playbook_analysis_snapshot(setup_id);
CREATE INDEX IF NOT EXISTS idx_playbook_analysis_generated_at ON playbook_analysis_snapshot(generated_at DESC);
CREATE INDEX IF NOT EXISTS idx_playbook_milestone_user_id ON playbook_milestone(user_id);
CREATE INDEX IF NOT EXISTS idx_playbook_milestone_setup_id ON playbook_milestone(setup_id);

ALTER TABLE feature_usage_events
    DROP CONSTRAINT IF EXISTS feature_usage_events_feature_check;
ALTER TABLE feature_usage_events
    DROP CONSTRAINT IF EXISTS chk_feature_usage_events_feature;
ALTER TABLE feature_usage_events
    ADD CONSTRAINT chk_feature_usage_events_feature
    CHECK (feature IN (
        'AI_REVIEW',
        'DAILY_COACH',
        'WEEKLY_COACH',
        'TRADING_DNA',
        'AI_LEAK_DETECTOR',
        'PERFORMANCE_TIMELINE',
        'PLAYBOOK',
        'TRADING_MEMORY',
        'SESSION_REVIEW',
        'TRADE_REPLAY',
        'TRADE_DETAIL',
        'CALENDAR_VIEW',
        'ANALYTICS_VIEW'
    ));

ALTER TABLE ai_usage_events
    DROP CONSTRAINT IF EXISTS ai_usage_events_usage_type_check;
ALTER TABLE ai_usage_events
    DROP CONSTRAINT IF EXISTS chk_ai_usage_events_usage_type;
ALTER TABLE ai_usage_events
    ADD CONSTRAINT chk_ai_usage_events_usage_type
    CHECK (usage_type IN (
        'AI_TRADE_REVIEW',
        'DAILY_COACH',
        'WEEKLY_COACH',
        'TRADING_DNA',
        'AI_LEAK_DETECTOR',
        'PERFORMANCE_TIMELINE',
        'SESSION_REVIEW',
        'PLAYBOOK_AI_DRAFT',
        'PLAYBOOK_AI_REVIEW'
    ));
