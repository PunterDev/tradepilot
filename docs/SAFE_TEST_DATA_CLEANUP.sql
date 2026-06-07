-- TradePilot safe test/demo data cleanup script.
--
-- This script is intentionally conservative:
-- - It never uses TRUNCATE.
-- - It never uses unqualified DELETE FROM table.
-- - Preview blocks use ROLLBACK by default.
-- - Commit blocks require explicit parameter edits before use.
--
-- Reviewed schema sources:
-- - backend/supabase_schema_consistency_fixes.sql
-- - backend/supabase_user_isolation_indexes.sql
-- - backend/supabase_billing_limits.sql
-- - backend/supabase_contact_messages.sql
-- - backend/supabase_feature_usage.sql
-- - backend/supabase_session_reviews.sql
-- - backend/supabase_trading_dna_performance.sql
-- - backend/supabase_observability.sql

-- ============================================================================
-- 1. Schema dependency analysis
-- ============================================================================
--
-- Core user-owned graph:
-- users
--   -> auth_tokens(user_id)                         ON DELETE CASCADE
--   -> trading_accounts(user_id)                    ON DELETE CASCADE
--        -> trades(account_id)                      ON DELETE SET NULL
--        -> import_batches(account_id)              ON DELETE SET NULL
--        -> journal_entries(account_id)             ON DELETE SET NULL
--        -> ai_coach_snapshot(account_id)           ON DELETE CASCADE
--        -> ai_session_reviews(account_id)          ON DELETE CASCADE
--        -> trading_dna_snapshots(account_id)       ON DELETE CASCADE
--   -> import_batches(user_id)                      ON DELETE CASCADE
--        -> trades(import_batch_id)                 ON DELETE SET NULL
--   -> trades(user_id)                              ON DELETE CASCADE
--        -> ai_trade_reviews(trade_id)              ON DELETE CASCADE
--        -> ai_trade_analyses(trade_id)             ON DELETE CASCADE
--        -> trade_rule_violations(trade_id)         ON DELETE CASCADE
--   -> journal_entries(user_id)                     ON DELETE CASCADE
--   -> ai_reflections(user_id)                      ON DELETE CASCADE
--   -> ai_trade_reviews(user_id)                    ON DELETE CASCADE
--   -> ai_trade_analyses(user_id)                   ON DELETE CASCADE
--   -> ai_coach_snapshot(user_id)                   ON DELETE CASCADE
--   -> ai_session_reviews(user_id)                  ON DELETE CASCADE
--   -> trading_dna_snapshots(user_id)               ON DELETE CASCADE
--   -> daily_discipline_scores(user_id)             no FK in migration
--   -> trade_rule_violations(user_id)               ON DELETE CASCADE
--   -> ai_usage_events(user_id)                     ON DELETE CASCADE
--   -> feature_usage_events(user_id)                ON DELETE CASCADE
--   -> admin_audit_logs(admin_user_id,target_user_id) ON DELETE CASCADE
--
-- Observability graph:
-- users
--   -> api_request_metrics(user_id)                 ON DELETE SET NULL
--   -> slow_request_events(user_id)                 ON DELETE SET NULL
--   -> ai_request_metrics(user_id)                  ON DELETE SET NULL
--   -> system_alerts(resolved_by)                   ON DELETE SET NULL
--
-- Global/non-user-owned tables:
-- contact_messages                                  no FK; can match by email/content
-- cache_metrics                                     no user id; preserve by default
-- email_delivery_events                             no FK; can match by recipient
-- stripe_events                                     no user id; preserve by default
-- processed_stripe_events                           no user id; preserve by default
-- query_performance_events                          no user id; preserve by default
-- system_alerts                                     mostly global; preserve by default
--
-- Unique constraints/indexes relevant to cleanup:
-- users.email unique
-- trades(user_id, account_id, external_trade_key) unique partial index
-- ai_trade_reviews(user_id, trade_id, review_version, review_hash) unique
-- ai_trade_analyses(user_id, trade_id, review_version, review_hash) unique
-- journal_entries(user_id, account_id, journal_date) unique
-- ai_coach_snapshot(user_id, account_id, snapshot_type, period_start, period_end, review_hash) unique
-- ai_session_reviews(user_id, account_id, session_date) unique
-- processed_stripe_events.event_id unique

-- ============================================================================
-- 2. Preview count queries
-- ============================================================================
--
-- These queries only count rows. Review every count before running any DELETE.

WITH candidate_users AS (
    SELECT id, email
    FROM users
    WHERE lower(email) ~ '(test|demo|sample|fake|qa|dev|example)'
)
SELECT 'users' AS table_name, count(*) AS candidate_rows FROM candidate_users
UNION ALL
SELECT 'auth_tokens', count(*) FROM auth_tokens WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'trading_accounts', count(*) FROM trading_accounts WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'import_batches', count(*) FROM import_batches WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'trades', count(*) FROM trades WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'trade_rule_violations', count(*) FROM trade_rule_violations WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'daily_discipline_scores', count(*) FROM daily_discipline_scores WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'journal_entries', count(*) FROM journal_entries WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'ai_reflections', count(*) FROM ai_reflections WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'ai_trade_reviews', count(*) FROM ai_trade_reviews WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'ai_trade_analyses', count(*) FROM ai_trade_analyses WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'ai_coach_snapshot', count(*) FROM ai_coach_snapshot WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'ai_session_reviews', count(*) FROM ai_session_reviews WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'trading_dna_snapshots', count(*) FROM trading_dna_snapshots WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'ai_usage_events', count(*) FROM ai_usage_events WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'feature_usage_events', count(*) FROM feature_usage_events WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'admin_audit_logs', count(*) FROM admin_audit_logs
WHERE admin_user_id IN (SELECT id FROM candidate_users)
   OR target_user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'api_request_metrics', count(*) FROM api_request_metrics WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'slow_request_events', count(*) FROM slow_request_events WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'ai_request_metrics', count(*) FROM ai_request_metrics WHERE user_id IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'system_alerts_resolved_by', count(*) FROM system_alerts WHERE resolved_by IN (SELECT id FROM candidate_users)
UNION ALL
SELECT 'contact_messages_by_email_or_content', count(*) FROM contact_messages
WHERE lower(email) ~ '(test|demo|sample|fake|qa|dev|example)'
   OR lower(subject) ~ '(test|demo|sample|fake|qa|dev|example)'
   OR lower(message) ~ '(test|demo|sample|fake|qa|dev|example)'
UNION ALL
SELECT 'email_delivery_events_by_recipient', count(*) FROM email_delivery_events
WHERE lower(recipient) ~ '(test|demo|sample|fake|qa|dev|example)';

-- Preview one specific user before targeted cleanup.
-- Change only the target_email value.
WITH params AS (
    SELECT 'change-me@example.com'::text AS target_email
),
target_user AS (
    SELECT id, email
    FROM users
    WHERE lower(email) = lower((SELECT target_email FROM params))
)
SELECT 'users' AS table_name, count(*) AS candidate_rows FROM target_user
UNION ALL
SELECT 'auth_tokens', count(*) FROM auth_tokens WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'trading_accounts', count(*) FROM trading_accounts WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'import_batches', count(*) FROM import_batches WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'trades', count(*) FROM trades WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'trade_rule_violations', count(*) FROM trade_rule_violations WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'daily_discipline_scores', count(*) FROM daily_discipline_scores WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'journal_entries', count(*) FROM journal_entries WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'ai_reflections', count(*) FROM ai_reflections WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'ai_trade_reviews', count(*) FROM ai_trade_reviews WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'ai_trade_analyses', count(*) FROM ai_trade_analyses WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'ai_coach_snapshot', count(*) FROM ai_coach_snapshot WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'ai_session_reviews', count(*) FROM ai_session_reviews WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'trading_dna_snapshots', count(*) FROM trading_dna_snapshots WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'ai_usage_events', count(*) FROM ai_usage_events WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'feature_usage_events', count(*) FROM feature_usage_events WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'admin_audit_logs', count(*) FROM admin_audit_logs
WHERE admin_user_id IN (SELECT id FROM target_user)
   OR target_user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'api_request_metrics', count(*) FROM api_request_metrics WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'slow_request_events', count(*) FROM slow_request_events WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'ai_request_metrics', count(*) FROM ai_request_metrics WHERE user_id IN (SELECT id FROM target_user)
UNION ALL
SELECT 'system_alerts_resolved_by', count(*) FROM system_alerts WHERE resolved_by IN (SELECT id FROM target_user)
UNION ALL
SELECT 'contact_messages_by_email', count(*) FROM contact_messages
WHERE lower(email) = lower((SELECT target_email FROM params))
UNION ALL
SELECT 'email_delivery_events_by_recipient', count(*) FROM email_delivery_events
WHERE lower(recipient) = lower((SELECT target_email FROM params));

-- ============================================================================
-- 3. User cleanup SQL
-- ============================================================================
--
-- Preview mode. Change target_email, run, inspect "deleted_*" row counts,
-- then ROLLBACK keeps data unchanged.

BEGIN;

WITH params AS (
    SELECT 'change-me@example.com'::text AS target_email
),
target_user AS (
    SELECT id, email
    FROM users
    WHERE lower(email) = lower((SELECT target_email FROM params))
),
deleted_email_events AS (
    DELETE FROM email_delivery_events
    WHERE lower(recipient) = lower((SELECT target_email FROM params))
    RETURNING id
),
deleted_contact_messages AS (
    DELETE FROM contact_messages
    WHERE lower(email) = lower((SELECT target_email FROM params))
    RETURNING id
),
deleted_feature_usage AS (
    DELETE FROM feature_usage_events
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_usage AS (
    DELETE FROM ai_usage_events
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_request_metrics AS (
    DELETE FROM ai_request_metrics
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_api_request_metrics AS (
    DELETE FROM api_request_metrics
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_slow_request_events AS (
    DELETE FROM slow_request_events
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
updated_system_alerts AS (
    UPDATE system_alerts
    SET resolved_by = NULL
    WHERE resolved_by IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_admin_audit AS (
    DELETE FROM admin_audit_logs
    WHERE admin_user_id IN (SELECT id FROM target_user)
       OR target_user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_auth_tokens AS (
    DELETE FROM auth_tokens
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_session_reviews AS (
    DELETE FROM ai_session_reviews
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_trading_dna AS (
    DELETE FROM trading_dna_snapshots
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_coach AS (
    DELETE FROM ai_coach_snapshot
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_reflections AS (
    DELETE FROM ai_reflections
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_trade_analyses AS (
    DELETE FROM ai_trade_analyses
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_trade_reviews AS (
    DELETE FROM ai_trade_reviews
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_trade_rule_violations AS (
    DELETE FROM trade_rule_violations
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_daily_discipline AS (
    DELETE FROM daily_discipline_scores
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_journal_entries AS (
    DELETE FROM journal_entries
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_trades AS (
    DELETE FROM trades
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_import_batches AS (
    DELETE FROM import_batches
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_accounts AS (
    DELETE FROM trading_accounts
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_users AS (
    DELETE FROM users
    WHERE id IN (SELECT id FROM target_user)
    RETURNING id
)
SELECT 'email_delivery_events' AS table_name, count(*) AS deleted_rows FROM deleted_email_events
UNION ALL SELECT 'contact_messages', count(*) FROM deleted_contact_messages
UNION ALL SELECT 'feature_usage_events', count(*) FROM deleted_feature_usage
UNION ALL SELECT 'ai_usage_events', count(*) FROM deleted_ai_usage
UNION ALL SELECT 'ai_request_metrics', count(*) FROM deleted_ai_request_metrics
UNION ALL SELECT 'api_request_metrics', count(*) FROM deleted_api_request_metrics
UNION ALL SELECT 'slow_request_events', count(*) FROM deleted_slow_request_events
UNION ALL SELECT 'system_alerts_resolved_by_nullified', count(*) FROM updated_system_alerts
UNION ALL SELECT 'admin_audit_logs', count(*) FROM deleted_admin_audit
UNION ALL SELECT 'auth_tokens', count(*) FROM deleted_auth_tokens
UNION ALL SELECT 'ai_session_reviews', count(*) FROM deleted_ai_session_reviews
UNION ALL SELECT 'trading_dna_snapshots', count(*) FROM deleted_trading_dna
UNION ALL SELECT 'ai_coach_snapshot', count(*) FROM deleted_ai_coach
UNION ALL SELECT 'ai_reflections', count(*) FROM deleted_ai_reflections
UNION ALL SELECT 'ai_trade_analyses', count(*) FROM deleted_ai_trade_analyses
UNION ALL SELECT 'ai_trade_reviews', count(*) FROM deleted_ai_trade_reviews
UNION ALL SELECT 'trade_rule_violations', count(*) FROM deleted_trade_rule_violations
UNION ALL SELECT 'daily_discipline_scores', count(*) FROM deleted_daily_discipline
UNION ALL SELECT 'journal_entries', count(*) FROM deleted_journal_entries
UNION ALL SELECT 'trades', count(*) FROM deleted_trades
UNION ALL SELECT 'import_batches', count(*) FROM deleted_import_batches
UNION ALL SELECT 'trading_accounts', count(*) FROM deleted_accounts
UNION ALL SELECT 'users', count(*) FROM deleted_users;

ROLLBACK;

-- Commit mode for one explicit user.
-- Only run after the preview row counts are exactly what you expect.
-- Change target_email in this block too, then replace ROLLBACK with COMMIT.

BEGIN;

WITH params AS (
    SELECT 'change-me@example.com'::text AS target_email
),
target_user AS (
    SELECT id, email
    FROM users
    WHERE lower(email) = lower((SELECT target_email FROM params))
),
deleted_email_events AS (
    DELETE FROM email_delivery_events
    WHERE lower(recipient) = lower((SELECT target_email FROM params))
    RETURNING id
),
deleted_contact_messages AS (
    DELETE FROM contact_messages
    WHERE lower(email) = lower((SELECT target_email FROM params))
    RETURNING id
),
deleted_feature_usage AS (
    DELETE FROM feature_usage_events
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_usage AS (
    DELETE FROM ai_usage_events
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_request_metrics AS (
    DELETE FROM ai_request_metrics
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_api_request_metrics AS (
    DELETE FROM api_request_metrics
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_slow_request_events AS (
    DELETE FROM slow_request_events
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
updated_system_alerts AS (
    UPDATE system_alerts
    SET resolved_by = NULL
    WHERE resolved_by IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_admin_audit AS (
    DELETE FROM admin_audit_logs
    WHERE admin_user_id IN (SELECT id FROM target_user)
       OR target_user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_auth_tokens AS (
    DELETE FROM auth_tokens
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_session_reviews AS (
    DELETE FROM ai_session_reviews
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_trading_dna AS (
    DELETE FROM trading_dna_snapshots
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_coach AS (
    DELETE FROM ai_coach_snapshot
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_reflections AS (
    DELETE FROM ai_reflections
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_trade_analyses AS (
    DELETE FROM ai_trade_analyses
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_ai_trade_reviews AS (
    DELETE FROM ai_trade_reviews
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_trade_rule_violations AS (
    DELETE FROM trade_rule_violations
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_daily_discipline AS (
    DELETE FROM daily_discipline_scores
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_journal_entries AS (
    DELETE FROM journal_entries
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_trades AS (
    DELETE FROM trades
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_import_batches AS (
    DELETE FROM import_batches
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_accounts AS (
    DELETE FROM trading_accounts
    WHERE user_id IN (SELECT id FROM target_user)
    RETURNING id
),
deleted_users AS (
    DELETE FROM users
    WHERE id IN (SELECT id FROM target_user)
    RETURNING id
)
SELECT 'email_delivery_events' AS table_name, count(*) AS deleted_rows FROM deleted_email_events
UNION ALL SELECT 'contact_messages', count(*) FROM deleted_contact_messages
UNION ALL SELECT 'feature_usage_events', count(*) FROM deleted_feature_usage
UNION ALL SELECT 'ai_usage_events', count(*) FROM deleted_ai_usage
UNION ALL SELECT 'ai_request_metrics', count(*) FROM deleted_ai_request_metrics
UNION ALL SELECT 'api_request_metrics', count(*) FROM deleted_api_request_metrics
UNION ALL SELECT 'slow_request_events', count(*) FROM deleted_slow_request_events
UNION ALL SELECT 'system_alerts_resolved_by_nullified', count(*) FROM updated_system_alerts
UNION ALL SELECT 'admin_audit_logs', count(*) FROM deleted_admin_audit
UNION ALL SELECT 'auth_tokens', count(*) FROM deleted_auth_tokens
UNION ALL SELECT 'ai_session_reviews', count(*) FROM deleted_ai_session_reviews
UNION ALL SELECT 'trading_dna_snapshots', count(*) FROM deleted_trading_dna
UNION ALL SELECT 'ai_coach_snapshot', count(*) FROM deleted_ai_coach
UNION ALL SELECT 'ai_reflections', count(*) FROM deleted_ai_reflections
UNION ALL SELECT 'ai_trade_analyses', count(*) FROM deleted_ai_trade_analyses
UNION ALL SELECT 'ai_trade_reviews', count(*) FROM deleted_ai_trade_reviews
UNION ALL SELECT 'trade_rule_violations', count(*) FROM deleted_trade_rule_violations
UNION ALL SELECT 'daily_discipline_scores', count(*) FROM deleted_daily_discipline
UNION ALL SELECT 'journal_entries', count(*) FROM deleted_journal_entries
UNION ALL SELECT 'trades', count(*) FROM deleted_trades
UNION ALL SELECT 'import_batches', count(*) FROM deleted_import_batches
UNION ALL SELECT 'trading_accounts', count(*) FROM deleted_accounts
UNION ALL SELECT 'users', count(*) FROM deleted_users;

COMMIT;

-- ============================================================================
-- 4. Demo data cleanup SQL
-- ============================================================================
--
-- Preview mode for seeded/demo/dev-looking data.
-- This targets users whose email explicitly contains test/demo/dev/etc.
-- It also targets orphan demo accounts only when user_id is NULL.
-- It intentionally does not delete normal-looking accounts owned by real users.

BEGIN;

WITH demo_users AS (
    SELECT id
    FROM users
    WHERE lower(email) ~ '(test|demo|sample|fake|qa|dev|example)'
),
orphan_demo_accounts AS (
    SELECT id
    FROM trading_accounts
    WHERE user_id IS NULL
      AND (
          lower(name) ~ '(test|demo|sample|fake|qa|dev|seed)'
          OR lower(coalesce(account_number, '')) ~ '(test|demo|sample|fake|qa|dev|seed)'
      )
),
demo_trade_ids AS (
    SELECT id
    FROM trades
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
),
deleted_contact_messages AS (
    DELETE FROM contact_messages
    WHERE lower(email) ~ '(test|demo|sample|fake|qa|dev|example)'
       OR lower(subject) ~ '(test|demo|sample|fake|qa|dev|example)'
       OR lower(message) ~ '(test|demo|sample|fake|qa|dev|example)'
    RETURNING id
),
deleted_email_events AS (
    DELETE FROM email_delivery_events
    WHERE lower(recipient) ~ '(test|demo|sample|fake|qa|dev|example)'
    RETURNING id
),
deleted_feature_usage AS (
    DELETE FROM feature_usage_events
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_ai_usage AS (
    DELETE FROM ai_usage_events
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_ai_request_metrics AS (
    DELETE FROM ai_request_metrics
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_api_request_metrics AS (
    DELETE FROM api_request_metrics
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_slow_request_events AS (
    DELETE FROM slow_request_events
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
updated_system_alerts AS (
    UPDATE system_alerts
    SET resolved_by = NULL
    WHERE resolved_by IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_admin_audit AS (
    DELETE FROM admin_audit_logs
    WHERE admin_user_id IN (SELECT id FROM demo_users)
       OR target_user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_auth_tokens AS (
    DELETE FROM auth_tokens
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_ai_session_reviews AS (
    DELETE FROM ai_session_reviews
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_trading_dna AS (
    DELETE FROM trading_dna_snapshots
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_ai_coach AS (
    DELETE FROM ai_coach_snapshot
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_ai_reflections AS (
    DELETE FROM ai_reflections
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_ai_trade_analyses AS (
    DELETE FROM ai_trade_analyses
    WHERE user_id IN (SELECT id FROM demo_users)
       OR trade_id IN (SELECT id FROM demo_trade_ids)
    RETURNING id
),
deleted_ai_trade_reviews AS (
    DELETE FROM ai_trade_reviews
    WHERE user_id IN (SELECT id FROM demo_users)
       OR trade_id IN (SELECT id FROM demo_trade_ids)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_trade_rule_violations AS (
    DELETE FROM trade_rule_violations
    WHERE user_id IN (SELECT id FROM demo_users)
       OR trade_id IN (SELECT id FROM demo_trade_ids)
    RETURNING id
),
deleted_daily_discipline AS (
    DELETE FROM daily_discipline_scores
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_journal_entries AS (
    DELETE FROM journal_entries
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_trades AS (
    DELETE FROM trades
    WHERE id IN (SELECT id FROM demo_trade_ids)
    RETURNING id
),
deleted_import_batches AS (
    DELETE FROM import_batches
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
       OR lower(coalesce(file_name, '')) ~ '(test|demo|sample|fake|qa|dev|seed)'
    RETURNING id
),
deleted_orphan_accounts AS (
    DELETE FROM trading_accounts
    WHERE id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_user_accounts AS (
    DELETE FROM trading_accounts
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_users AS (
    DELETE FROM users
    WHERE id IN (SELECT id FROM demo_users)
    RETURNING id
)
SELECT 'contact_messages' AS table_name, count(*) AS deleted_rows FROM deleted_contact_messages
UNION ALL SELECT 'email_delivery_events', count(*) FROM deleted_email_events
UNION ALL SELECT 'feature_usage_events', count(*) FROM deleted_feature_usage
UNION ALL SELECT 'ai_usage_events', count(*) FROM deleted_ai_usage
UNION ALL SELECT 'ai_request_metrics', count(*) FROM deleted_ai_request_metrics
UNION ALL SELECT 'api_request_metrics', count(*) FROM deleted_api_request_metrics
UNION ALL SELECT 'slow_request_events', count(*) FROM deleted_slow_request_events
UNION ALL SELECT 'system_alerts_resolved_by_nullified', count(*) FROM updated_system_alerts
UNION ALL SELECT 'admin_audit_logs', count(*) FROM deleted_admin_audit
UNION ALL SELECT 'auth_tokens', count(*) FROM deleted_auth_tokens
UNION ALL SELECT 'ai_session_reviews', count(*) FROM deleted_ai_session_reviews
UNION ALL SELECT 'trading_dna_snapshots', count(*) FROM deleted_trading_dna
UNION ALL SELECT 'ai_coach_snapshot', count(*) FROM deleted_ai_coach
UNION ALL SELECT 'ai_reflections', count(*) FROM deleted_ai_reflections
UNION ALL SELECT 'ai_trade_analyses', count(*) FROM deleted_ai_trade_analyses
UNION ALL SELECT 'ai_trade_reviews', count(*) FROM deleted_ai_trade_reviews
UNION ALL SELECT 'trade_rule_violations', count(*) FROM deleted_trade_rule_violations
UNION ALL SELECT 'daily_discipline_scores', count(*) FROM deleted_daily_discipline
UNION ALL SELECT 'journal_entries', count(*) FROM deleted_journal_entries
UNION ALL SELECT 'trades', count(*) FROM deleted_trades
UNION ALL SELECT 'import_batches', count(*) FROM deleted_import_batches
UNION ALL SELECT 'orphan_demo_trading_accounts', count(*) FROM deleted_orphan_accounts
UNION ALL SELECT 'demo_user_trading_accounts', count(*) FROM deleted_user_accounts
UNION ALL SELECT 'users', count(*) FROM deleted_users;

ROLLBACK;

-- Commit mode for seeded/demo/dev-looking data.
-- Only run after the preview row counts are exactly what you expect.

BEGIN;

WITH demo_users AS (
    SELECT id
    FROM users
    WHERE lower(email) ~ '(test|demo|sample|fake|qa|dev|example)'
),
orphan_demo_accounts AS (
    SELECT id
    FROM trading_accounts
    WHERE user_id IS NULL
      AND (
          lower(name) ~ '(test|demo|sample|fake|qa|dev|seed)'
          OR lower(coalesce(account_number, '')) ~ '(test|demo|sample|fake|qa|dev|seed)'
      )
),
demo_trade_ids AS (
    SELECT id
    FROM trades
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
),
deleted_contact_messages AS (
    DELETE FROM contact_messages
    WHERE lower(email) ~ '(test|demo|sample|fake|qa|dev|example)'
       OR lower(subject) ~ '(test|demo|sample|fake|qa|dev|example)'
       OR lower(message) ~ '(test|demo|sample|fake|qa|dev|example)'
    RETURNING id
),
deleted_email_events AS (
    DELETE FROM email_delivery_events
    WHERE lower(recipient) ~ '(test|demo|sample|fake|qa|dev|example)'
    RETURNING id
),
deleted_feature_usage AS (
    DELETE FROM feature_usage_events
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_ai_usage AS (
    DELETE FROM ai_usage_events
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_ai_request_metrics AS (
    DELETE FROM ai_request_metrics
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_api_request_metrics AS (
    DELETE FROM api_request_metrics
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_slow_request_events AS (
    DELETE FROM slow_request_events
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
updated_system_alerts AS (
    UPDATE system_alerts
    SET resolved_by = NULL
    WHERE resolved_by IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_admin_audit AS (
    DELETE FROM admin_audit_logs
    WHERE admin_user_id IN (SELECT id FROM demo_users)
       OR target_user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_auth_tokens AS (
    DELETE FROM auth_tokens
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_ai_session_reviews AS (
    DELETE FROM ai_session_reviews
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_trading_dna AS (
    DELETE FROM trading_dna_snapshots
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_ai_coach AS (
    DELETE FROM ai_coach_snapshot
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_ai_reflections AS (
    DELETE FROM ai_reflections
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_ai_trade_analyses AS (
    DELETE FROM ai_trade_analyses
    WHERE user_id IN (SELECT id FROM demo_users)
       OR trade_id IN (SELECT id FROM demo_trade_ids)
    RETURNING id
),
deleted_ai_trade_reviews AS (
    DELETE FROM ai_trade_reviews
    WHERE user_id IN (SELECT id FROM demo_users)
       OR trade_id IN (SELECT id FROM demo_trade_ids)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_trade_rule_violations AS (
    DELETE FROM trade_rule_violations
    WHERE user_id IN (SELECT id FROM demo_users)
       OR trade_id IN (SELECT id FROM demo_trade_ids)
    RETURNING id
),
deleted_daily_discipline AS (
    DELETE FROM daily_discipline_scores
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_journal_entries AS (
    DELETE FROM journal_entries
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_trades AS (
    DELETE FROM trades
    WHERE id IN (SELECT id FROM demo_trade_ids)
    RETURNING id
),
deleted_import_batches AS (
    DELETE FROM import_batches
    WHERE user_id IN (SELECT id FROM demo_users)
       OR account_id IN (SELECT id FROM orphan_demo_accounts)
       OR lower(coalesce(file_name, '')) ~ '(test|demo|sample|fake|qa|dev|seed)'
    RETURNING id
),
deleted_orphan_accounts AS (
    DELETE FROM trading_accounts
    WHERE id IN (SELECT id FROM orphan_demo_accounts)
    RETURNING id
),
deleted_user_accounts AS (
    DELETE FROM trading_accounts
    WHERE user_id IN (SELECT id FROM demo_users)
    RETURNING id
),
deleted_users AS (
    DELETE FROM users
    WHERE id IN (SELECT id FROM demo_users)
    RETURNING id
)
SELECT 'contact_messages' AS table_name, count(*) AS deleted_rows FROM deleted_contact_messages
UNION ALL SELECT 'email_delivery_events', count(*) FROM deleted_email_events
UNION ALL SELECT 'feature_usage_events', count(*) FROM deleted_feature_usage
UNION ALL SELECT 'ai_usage_events', count(*) FROM deleted_ai_usage
UNION ALL SELECT 'ai_request_metrics', count(*) FROM deleted_ai_request_metrics
UNION ALL SELECT 'api_request_metrics', count(*) FROM deleted_api_request_metrics
UNION ALL SELECT 'slow_request_events', count(*) FROM deleted_slow_request_events
UNION ALL SELECT 'system_alerts_resolved_by_nullified', count(*) FROM updated_system_alerts
UNION ALL SELECT 'admin_audit_logs', count(*) FROM deleted_admin_audit
UNION ALL SELECT 'auth_tokens', count(*) FROM deleted_auth_tokens
UNION ALL SELECT 'ai_session_reviews', count(*) FROM deleted_ai_session_reviews
UNION ALL SELECT 'trading_dna_snapshots', count(*) FROM deleted_trading_dna
UNION ALL SELECT 'ai_coach_snapshot', count(*) FROM deleted_ai_coach
UNION ALL SELECT 'ai_reflections', count(*) FROM deleted_ai_reflections
UNION ALL SELECT 'ai_trade_analyses', count(*) FROM deleted_ai_trade_analyses
UNION ALL SELECT 'ai_trade_reviews', count(*) FROM deleted_ai_trade_reviews
UNION ALL SELECT 'trade_rule_violations', count(*) FROM deleted_trade_rule_violations
UNION ALL SELECT 'daily_discipline_scores', count(*) FROM deleted_daily_discipline
UNION ALL SELECT 'journal_entries', count(*) FROM deleted_journal_entries
UNION ALL SELECT 'trades', count(*) FROM deleted_trades
UNION ALL SELECT 'import_batches', count(*) FROM deleted_import_batches
UNION ALL SELECT 'orphan_demo_trading_accounts', count(*) FROM deleted_orphan_accounts
UNION ALL SELECT 'demo_user_trading_accounts', count(*) FROM deleted_user_accounts
UNION ALL SELECT 'users', count(*) FROM deleted_users;

COMMIT;

-- ============================================================================
-- 5. Warnings
-- ============================================================================
--
-- 1. Run the preview count queries first. If any count surprises you, stop.
-- 2. Run the ROLLBACK preview block before the COMMIT block.
-- 3. The demo cleanup intentionally deletes users whose email contains:
--    test, demo, sample, fake, qa, dev, or example.
-- 4. Do not run demo cleanup if a legitimate production user matches that
--    pattern. Use the single-user cleanup block instead.
-- 5. processed_stripe_events and stripe_events are global webhook/audit tables
--    with no user id. They are preserved by default.
-- 6. cache_metrics and query_performance_events are global metrics tables with
--    no user id. They are preserved by default.
-- 7. contact_messages are matched by email/content only because they are not
--    linked to users by FK.
-- 8. system_alerts are preserved; only resolved_by is nullified for deleted
--    users to avoid keeping an invalid reference.
