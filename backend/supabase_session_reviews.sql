-- AI Session Review cache for Trade Replay / Session Review.
-- Run once in Supabase/Postgres environments that predate this feature.

CREATE TABLE IF NOT EXISTS ai_session_reviews (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id bigint NOT NULL REFERENCES trading_accounts(id) ON DELETE CASCADE,
    session_date date NOT NULL,
    review_hash varchar(64) NOT NULL,
    summary text,
    what_went_well text,
    what_hurt_performance text,
    biggest_mistake text,
    best_setup text,
    worst_decision text,
    emotional_pattern text,
    execution_pattern text,
    risk_management_pattern text,
    one_rule_for_tomorrow text,
    created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE ai_session_reviews
    ADD COLUMN IF NOT EXISTS emotional_pattern text;

ALTER TABLE ai_session_reviews
    ADD COLUMN IF NOT EXISTS what_hurt_performance text;

ALTER TABLE ai_session_reviews
    ADD COLUMN IF NOT EXISTS execution_pattern text;

ALTER TABLE ai_session_reviews
    ADD COLUMN IF NOT EXISTS risk_management_pattern text;

WITH ranked_reviews AS (
    SELECT
        id,
        row_number() OVER (
            PARTITION BY user_id, account_id, session_date
            ORDER BY created_at DESC, id DESC
        ) AS duplicate_rank
    FROM ai_session_reviews
)
DELETE FROM ai_session_reviews
WHERE id IN (
    SELECT id
    FROM ranked_reviews
    WHERE duplicate_rank > 1
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_ai_session_reviews_user_account_date'
    ) THEN
        ALTER TABLE ai_session_reviews
            ADD CONSTRAINT uk_ai_session_reviews_user_account_date
            UNIQUE (user_id, account_id, session_date);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ai_session_reviews_user_id
    ON ai_session_reviews (user_id);

CREATE INDEX IF NOT EXISTS idx_ai_session_reviews_user_account_date
    ON ai_session_reviews (user_id, account_id, session_date);

CREATE INDEX IF NOT EXISTS idx_ai_session_reviews_user_account_date_hash
    ON ai_session_reviews (user_id, account_id, session_date, review_hash);

CREATE INDEX IF NOT EXISTS idx_ai_session_reviews_created_at
    ON ai_session_reviews (created_at);
