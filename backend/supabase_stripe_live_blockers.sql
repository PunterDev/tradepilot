-- Stripe Live blocker hardening.
-- Run after supabase_stripe_production_safety.sql.
--
-- Migration safety:
-- 1. Existing credit-pack rows may not have customer, payment intent, charge,
--    pack, refund, or dispute columns. This migration backfills only safe
--    numeric defaults and leaves unknown Stripe references null.
-- 2. Before applying unique payment-intent/charge indexes, inspect duplicates:
--      select stripe_payment_intent_id, count(*) from ai_credit_purchases
--      where stripe_payment_intent_id is not null and stripe_payment_intent_id <> ''
--      group by stripe_payment_intent_id having count(*) > 1;
--      select stripe_charge_id, count(*) from ai_credit_purchases
--      where stripe_charge_id is not null and stripe_charge_id <> ''
--      group by stripe_charge_id having count(*) > 1;
-- 3. If duplicates exist, reconcile against Stripe before rerunning.
-- 4. Rollback/recovery: dropping the added indexes/columns restores the prior
--    schema, but refund/dispute audit data captured after deploy would be lost.

create table if not exists stripe_checkout_attempts (
    id bigserial primary key,
    user_id bigint not null,
    stripe_checkout_session_id varchar(255),
    status varchar(30) not null,
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists uk_stripe_checkout_attempts_user
    on stripe_checkout_attempts(user_id);

create index if not exists idx_stripe_checkout_attempts_status_expires
    on stripe_checkout_attempts(status, expires_at);

alter table ai_credit_purchases
    add column if not exists stripe_customer_id varchar(255),
    add column if not exists stripe_charge_id varchar(255),
    add column if not exists pack_id varchar(50),
    add column if not exists refunded_amount_cents integer,
    add column if not exists reversed_credits integer,
    add column if not exists dispute_status varchar(50),
    add column if not exists refunded_at timestamptz,
    add column if not exists reversed_at timestamptz,
    add column if not exists disputed_at timestamptz;

update ai_credit_purchases
set refunded_amount_cents = 0
where refunded_amount_cents is null;

update ai_credit_purchases
set reversed_credits = 0
where reversed_credits is null;

alter table ai_credit_purchases
    alter column refunded_amount_cents set default 0,
    alter column refunded_amount_cents set not null,
    alter column reversed_credits set default 0,
    alter column reversed_credits set not null;

do $$
begin
    if exists (
        select 1
        from ai_credit_purchases
        where stripe_payment_intent_id is not null and stripe_payment_intent_id <> ''
        group by stripe_payment_intent_id
        having count(*) > 1
    ) then
        raise exception 'Cannot create unique Stripe payment intent index: duplicate non-null stripe_payment_intent_id values exist.';
    end if;

    if exists (
        select 1
        from ai_credit_purchases
        where stripe_charge_id is not null and stripe_charge_id <> ''
        group by stripe_charge_id
        having count(*) > 1
    ) then
        raise exception 'Cannot create unique Stripe charge index: duplicate non-null stripe_charge_id values exist.';
    end if;
end $$;

create unique index if not exists ux_ai_credit_purchases_payment_intent_not_null
    on ai_credit_purchases(stripe_payment_intent_id)
    where stripe_payment_intent_id is not null and stripe_payment_intent_id <> '';

create unique index if not exists ux_ai_credit_purchases_charge_not_null
    on ai_credit_purchases(stripe_charge_id)
    where stripe_charge_id is not null and stripe_charge_id <> '';

create index if not exists idx_ai_credit_purchases_customer
    on ai_credit_purchases(stripe_customer_id);
