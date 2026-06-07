-- Subscription plan limits and AI usage tracking for Supabase/Postgres.
-- Run once in environments that already had TradePilot tables before billing.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS plan varchar(20) NOT NULL DEFAULT 'FREE',
    ADD COLUMN IF NOT EXISTS role varchar(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS subscription_status varchar(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS current_period_start timestamptz,
    ADD COLUMN IF NOT EXISTS current_period_end timestamptz,
    ADD COLUMN IF NOT EXISTS stripe_customer_id varchar(255),
    ADD COLUMN IF NOT EXISTS stripe_subscription_id varchar(255),
    ADD COLUMN IF NOT EXISTS stripe_cancel_at_period_end boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS stripe_cancel_at timestamptz,
    ADD COLUMN IF NOT EXISTS stripe_canceled_at timestamptz,
    ADD COLUMN IF NOT EXISTS pro_until timestamptz;

UPDATE users
SET plan = 'FREE'
WHERE plan IS NULL OR plan = '';

UPDATE users
SET subscription_status = 'NONE'
WHERE subscription_status IS NULL OR subscription_status = '';

UPDATE users
SET role = 'USER'
WHERE role IS NULL OR role = '';

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id bigserial PRIMARY KEY,
    admin_user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action varchar(40) NOT NULL,
    reason varchar(500),
    before_value text,
    after_value text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_admin_user_id
    ON admin_audit_logs (admin_user_id);

CREATE INDEX IF NOT EXISTS idx_admin_audit_target_user_id
    ON admin_audit_logs (target_user_id);

CREATE INDEX IF NOT EXISTS idx_admin_audit_created_at
    ON admin_audit_logs (created_at);

CREATE TABLE IF NOT EXISTS ai_usage_events (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    usage_type varchar(40) NOT NULL,
    related_resource_id bigint,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_events_user_id
    ON ai_usage_events (user_id);

CREATE INDEX IF NOT EXISTS idx_ai_usage_events_user_type_created
    ON ai_usage_events (user_id, usage_type, created_at);

CREATE INDEX IF NOT EXISTS idx_ai_usage_events_created_at
    ON ai_usage_events (created_at);

DO $$
DECLARE
    constraint_record record;
BEGIN
    FOR constraint_record IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'ai_usage_events'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%usage_type%'
    LOOP
        EXECUTE format('ALTER TABLE ai_usage_events DROP CONSTRAINT IF EXISTS %I', constraint_record.conname);
    END LOOP;
END $$;

ALTER TABLE ai_usage_events
    ADD CONSTRAINT chk_ai_usage_events_usage_type
    CHECK (usage_type IN (
        'AI_TRADE_REVIEW',
        'DAILY_COACH',
        'WEEKLY_COACH',
        'TRADING_DNA',
        'SESSION_REVIEW'
    ));

CREATE INDEX IF NOT EXISTS idx_users_plan
    ON users (plan);

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users (role);

CREATE INDEX IF NOT EXISTS idx_users_subscription_status
    ON users (subscription_status);

CREATE INDEX IF NOT EXISTS idx_users_stripe_customer_id
    ON users (stripe_customer_id);

CREATE INDEX IF NOT EXISTS idx_users_stripe_subscription_id
    ON users (stripe_subscription_id);

CREATE INDEX IF NOT EXISTS idx_users_billing_lookup
    ON users (id, plan, subscription_status, current_period_end);
