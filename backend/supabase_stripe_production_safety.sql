-- Stripe production safety hardening.
-- Run after billing, credit-pack, and observability migrations.
--
-- Migration safety:
-- 1. The duplicate preflight checks below intentionally fail before unique indexes
--    are created if historical rows already share a non-null Stripe customer or
--    subscription id.
-- 2. If this migration fails, inspect duplicates with:
--      select stripe_customer_id, count(*) from users
--      where stripe_customer_id is not null and stripe_customer_id <> ''
--      group by stripe_customer_id having count(*) > 1;
--      select stripe_subscription_id, count(*) from users
--      where stripe_subscription_id is not null and stripe_subscription_id <> ''
--      group by stripe_subscription_id having count(*) > 1;
-- 3. Resolve duplicates manually by reconciling against Stripe before rerunning.

alter table processed_stripe_events
    add column if not exists stripe_event_created timestamptz;

create index if not exists idx_processed_stripe_events_stripe_created
    on processed_stripe_events(stripe_event_created);

create table if not exists stripe_subscription_states (
    id bigserial primary key,
    subscription_id varchar(255) not null unique,
    last_event_id varchar(120) not null,
    last_event_type varchar(120) not null,
    last_event_created timestamptz not null,
    updated_at timestamptz not null default now()
);

create index if not exists idx_stripe_subscription_states_last_event_created
    on stripe_subscription_states(last_event_created);

do $$
begin
    if exists (
        select 1
        from users
        where stripe_customer_id is not null and stripe_customer_id <> ''
        group by stripe_customer_id
        having count(*) > 1
    ) then
        raise exception 'Cannot create unique Stripe customer index: duplicate non-null stripe_customer_id values exist. Reconcile historical users before rerunning migration.';
    end if;

    if exists (
        select 1
        from users
        where stripe_subscription_id is not null and stripe_subscription_id <> ''
        group by stripe_subscription_id
        having count(*) > 1
    ) then
        raise exception 'Cannot create unique Stripe subscription index: duplicate non-null stripe_subscription_id values exist. Reconcile historical users before rerunning migration.';
    end if;
end $$;

create unique index if not exists ux_users_stripe_customer_id_not_null
    on users(stripe_customer_id)
    where stripe_customer_id is not null and stripe_customer_id <> '';

create unique index if not exists ux_users_stripe_subscription_id_not_null
    on users(stripe_subscription_id)
    where stripe_subscription_id is not null and stripe_subscription_id <> '';
