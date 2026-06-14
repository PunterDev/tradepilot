-- Adds AI Leak Detector to the AI usage ledger constraint.
-- Safe to run in Supabase/PostgreSQL after supabase_billing_limits.sql.

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
        'SESSION_REVIEW'
    ));
