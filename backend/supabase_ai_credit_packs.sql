-- TradePilot AI credit packs
-- Safe to run in Supabase. Adds durable purchased credit balances and an idempotent purchase ledger.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS purchased_ai_credits_balance INTEGER NOT NULL DEFAULT 0;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_purchased_ai_credits_balance_nonnegative;

ALTER TABLE users
    ADD CONSTRAINT users_purchased_ai_credits_balance_nonnegative
    CHECK (purchased_ai_credits_balance >= 0);

ALTER TABLE ai_credit_usage
    ADD COLUMN IF NOT EXISTS monthly_credits INTEGER;

ALTER TABLE ai_credit_usage
    ADD COLUMN IF NOT EXISTS purchased_credits INTEGER;

ALTER TABLE ai_credit_usage
    DROP CONSTRAINT IF EXISTS ai_credit_usage_monthly_credits_nonnegative;

ALTER TABLE ai_credit_usage
    ADD CONSTRAINT ai_credit_usage_monthly_credits_nonnegative
    CHECK (monthly_credits IS NULL OR monthly_credits >= 0);

ALTER TABLE ai_credit_usage
    DROP CONSTRAINT IF EXISTS ai_credit_usage_purchased_credits_nonnegative;

ALTER TABLE ai_credit_usage
    ADD CONSTRAINT ai_credit_usage_purchased_credits_nonnegative
    CHECK (purchased_credits IS NULL OR purchased_credits >= 0);

CREATE TABLE IF NOT EXISTS ai_credit_purchases (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stripe_checkout_session_id VARCHAR(255) NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    credits_purchased INTEGER NOT NULL,
    amount_cents INTEGER NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    CONSTRAINT ai_credit_purchases_session_unique UNIQUE (stripe_checkout_session_id),
    CONSTRAINT ai_credit_purchases_credits_positive CHECK (credits_purchased > 0),
    CONSTRAINT ai_credit_purchases_amount_nonnegative CHECK (amount_cents >= 0),
    CONSTRAINT ai_credit_purchases_user_fk FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_ai_credit_purchases_user_id
    ON ai_credit_purchases(user_id);

CREATE INDEX IF NOT EXISTS idx_ai_credit_purchases_created_at
    ON ai_credit_purchases(created_at);
