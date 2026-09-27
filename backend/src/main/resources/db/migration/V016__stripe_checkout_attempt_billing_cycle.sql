alter table stripe_checkout_attempts
    add column if not exists billing_cycle varchar(10);
