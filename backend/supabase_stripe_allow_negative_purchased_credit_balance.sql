-- Stripe purchased-credit reversal safety.
-- Run after supabase_stripe_reversal_reason_accounting.sql and before enabling
-- Live refund/dispute webhooks.
--
-- Refund/dispute policy allows purchased_ai_credits_balance to become negative
-- when a user has already consumed purchased credits before a full refund or
-- dispute reversal. Monthly PRO allowance is tracked separately and is not
-- represented by this column.
--
-- Rerun is safe.

alter table users
    drop constraint if exists users_purchased_ai_credits_balance_nonnegative;
