-- Admin portal support for Supabase/Postgres.
-- Run once in environments that already had TradePilot tables before admin roles.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role varchar(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS pro_until timestamptz;

UPDATE users
SET role = 'USER'
WHERE role IS NULL OR role = '';

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users (role);

CREATE INDEX IF NOT EXISTS idx_users_created_at
    ON users (created_at);

CREATE INDEX IF NOT EXISTS idx_users_plan
    ON users (plan);

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

CREATE INDEX IF NOT EXISTS idx_admin_audit_admin_created_at
    ON admin_audit_logs (admin_user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_admin_audit_target_created_at
    ON admin_audit_logs (target_user_id, created_at);

ALTER TABLE admin_audit_logs DROP CONSTRAINT IF EXISTS chk_admin_audit_logs_action;
ALTER TABLE admin_audit_logs
    ADD CONSTRAINT chk_admin_audit_logs_action
    CHECK (action IN ('UPGRADE_TO_PRO', 'DOWNGRADE_TO_FREE', 'ROLE_CHANGE'));
