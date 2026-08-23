-- Adds Performance Timeline/Journey to the feature usage analytics constraint.
-- Safe to run in Supabase/PostgreSQL after supabase_feature_usage.sql.
-- This preserves all existing feature values and handles both historical
-- constraint names used by previous migrations.

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
        'SESSION_REVIEW',
        'TRADE_DETAIL',
        'CALENDAR_VIEW',
        'ANALYTICS_VIEW'
    ));
