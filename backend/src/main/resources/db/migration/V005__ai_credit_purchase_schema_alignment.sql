-- Align ai_credit_purchases with the current JPA entity and production baseline.
--
-- This migration is intentionally additive/idempotent for databases that already
-- have historical credit-pack purchase rows. Zero is the safe default for
-- accounting counters when no refund/dispute/reversal has been recorded.

alter table ai_credit_purchases
    add column if not exists refunded_amount_cents integer;

alter table ai_credit_purchases
    add column if not exists reversed_credits integer;

alter table ai_credit_purchases
    add column if not exists refund_reversal_credits integer;

alter table ai_credit_purchases
    add column if not exists dispute_reversal_credits integer;

alter table ai_credit_purchases
    add column if not exists dispute_restored_credits integer;

update ai_credit_purchases
set refunded_amount_cents = 0
where refunded_amount_cents is null;

update ai_credit_purchases
set reversed_credits = 0
where reversed_credits is null;

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
    alter column refunded_amount_cents set default 0;

alter table ai_credit_purchases
    alter column refunded_amount_cents set not null;

alter table ai_credit_purchases
    alter column reversed_credits set default 0;

alter table ai_credit_purchases
    alter column reversed_credits set not null;

alter table ai_credit_purchases
    alter column refund_reversal_credits set default 0;

alter table ai_credit_purchases
    alter column refund_reversal_credits set not null;

alter table ai_credit_purchases
    alter column dispute_reversal_credits set default 0;

alter table ai_credit_purchases
    alter column dispute_reversal_credits set not null;

alter table ai_credit_purchases
    alter column dispute_restored_credits set default 0;

alter table ai_credit_purchases
    alter column dispute_restored_credits set not null;

alter table ai_credit_purchases
    drop constraint if exists chk_ai_credit_purchases_refunded_amount_nonnegative;

alter table ai_credit_purchases
    add constraint chk_ai_credit_purchases_refunded_amount_nonnegative
        check (refunded_amount_cents >= 0);

alter table ai_credit_purchases
    drop constraint if exists chk_ai_credit_purchases_reversed_credits_nonnegative;

alter table ai_credit_purchases
    add constraint chk_ai_credit_purchases_reversed_credits_nonnegative
        check (reversed_credits >= 0);

alter table ai_credit_purchases
    drop constraint if exists chk_ai_credit_purchases_refund_reversal_nonnegative;

alter table ai_credit_purchases
    add constraint chk_ai_credit_purchases_refund_reversal_nonnegative
        check (refund_reversal_credits >= 0);

alter table ai_credit_purchases
    drop constraint if exists chk_ai_credit_purchases_dispute_reversal_nonnegative;

alter table ai_credit_purchases
    add constraint chk_ai_credit_purchases_dispute_reversal_nonnegative
        check (dispute_reversal_credits >= 0);

alter table ai_credit_purchases
    drop constraint if exists chk_ai_credit_purchases_dispute_restored_nonnegative;

alter table ai_credit_purchases
    add constraint chk_ai_credit_purchases_dispute_restored_nonnegative
        check (dispute_restored_credits >= 0);

alter table ai_credit_purchases
    drop constraint if exists chk_ai_credit_purchases_dispute_restored_lte_reversal;

alter table ai_credit_purchases
    add constraint chk_ai_credit_purchases_dispute_restored_lte_reversal
        check (dispute_restored_credits <= dispute_reversal_credits);
