-- Stripe credit-pack reversal reason accounting.
-- Run after supabase_stripe_live_blockers.sql.
--
-- Migration safety:
-- 1. This migration adds only non-secret, non-negative accounting counters.
-- 2. Existing reversed_credits are conservatively treated as refund reversals
--    unless dispute state shows an active dispute/lost dispute. Review legacy
--    rows with nonzero reversed_credits before enabling automated live refunds.
-- 3. Rerun is safe; all column additions and constraints are idempotent.

alter table ai_credit_purchases
    add column if not exists refund_reversal_credits integer,
    add column if not exists dispute_reversal_credits integer,
    add column if not exists dispute_restored_credits integer;

update ai_credit_purchases
set refund_reversal_credits = case
        when coalesce(reversed_credits, 0) > 0
             and coalesce(status, '') not in ('DISPUTED', 'DISPUTE_LOST')
            then coalesce(reversed_credits, 0)
        else 0
    end
where refund_reversal_credits is null;

update ai_credit_purchases
set dispute_reversal_credits = case
        when coalesce(reversed_credits, 0) > 0
             and coalesce(status, '') in ('DISPUTED', 'DISPUTE_LOST')
            then coalesce(reversed_credits, 0)
        else 0
    end
where dispute_reversal_credits is null;

update ai_credit_purchases
set dispute_restored_credits = 0
where dispute_restored_credits is null;

alter table ai_credit_purchases
    alter column refund_reversal_credits set default 0,
    alter column refund_reversal_credits set not null,
    alter column dispute_reversal_credits set default 0,
    alter column dispute_reversal_credits set not null,
    alter column dispute_restored_credits set default 0,
    alter column dispute_restored_credits set not null;

alter table ai_credit_purchases
    drop constraint if exists chk_ai_credit_purchases_refund_reversal_nonnegative,
    add constraint chk_ai_credit_purchases_refund_reversal_nonnegative
        check (refund_reversal_credits >= 0);

alter table ai_credit_purchases
    drop constraint if exists chk_ai_credit_purchases_dispute_reversal_nonnegative,
    add constraint chk_ai_credit_purchases_dispute_reversal_nonnegative
        check (dispute_reversal_credits >= 0);

alter table ai_credit_purchases
    drop constraint if exists chk_ai_credit_purchases_dispute_restored_nonnegative,
    add constraint chk_ai_credit_purchases_dispute_restored_nonnegative
        check (dispute_restored_credits >= 0);

alter table ai_credit_purchases
    drop constraint if exists chk_ai_credit_purchases_dispute_restored_lte_reversal,
    add constraint chk_ai_credit_purchases_dispute_restored_lte_reversal
        check (dispute_restored_credits <= dispute_reversal_credits);
