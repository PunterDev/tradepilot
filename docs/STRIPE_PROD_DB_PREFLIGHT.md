# Stripe Production DB Preflight

Run these checks against production before applying any Stripe production migration. They are read-only unless explicitly marked otherwise. Do not run destructive cleanup until each result is reconciled against Stripe Dashboard and application support records.

## Migration Order

1. `backend/supabase_stripe_production_safety.sql`
2. `backend/supabase_stripe_live_blockers.sql`
3. `backend/supabase_stripe_reversal_reason_accounting.sql`
4. `backend/supabase_stripe_allow_negative_purchased_credit_balance.sql`

Dependencies:

- `supabase_stripe_production_safety.sql` expects existing `users` and `processed_stripe_events` tables.
- `supabase_stripe_live_blockers.sql` expects `ai_credit_purchases` and the base Stripe safety migration to be applied first.
- `supabase_stripe_reversal_reason_accounting.sql` expects `reversed_credits`, refund fields, and dispute fields added by `supabase_stripe_live_blockers.sql`.
- `supabase_stripe_allow_negative_purchased_credit_balance.sql` expects `users.purchased_ai_credits_balance` and removes the older nonnegative check so refund/dispute reversals can produce the documented negative purchased-credit state.

## Preflight Queries

Duplicate Stripe customer IDs:

```sql
select stripe_customer_id, count(*) as row_count, array_agg(id order by id) as user_ids
from users
where stripe_customer_id is not null and stripe_customer_id <> ''
group by stripe_customer_id
having count(*) > 1
order by row_count desc, stripe_customer_id;
```

Duplicate Stripe subscription IDs:

```sql
select stripe_subscription_id, count(*) as row_count, array_agg(id order by id) as user_ids
from users
where stripe_subscription_id is not null and stripe_subscription_id <> ''
group by stripe_subscription_id
having count(*) > 1
order by row_count desc, stripe_subscription_id;
```

Duplicate Checkout Session IDs:

```sql
select stripe_checkout_session_id, count(*) as row_count, array_agg(id order by id) as purchase_ids
from ai_credit_purchases
where stripe_checkout_session_id is not null and stripe_checkout_session_id <> ''
group by stripe_checkout_session_id
having count(*) > 1
order by row_count desc, stripe_checkout_session_id;
```

Duplicate PaymentIntent IDs:

```sql
select stripe_payment_intent_id, count(*) as row_count, array_agg(id order by id) as purchase_ids
from ai_credit_purchases
where stripe_payment_intent_id is not null and stripe_payment_intent_id <> ''
group by stripe_payment_intent_id
having count(*) > 1
order by row_count desc, stripe_payment_intent_id;
```

Duplicate Charge IDs:

```sql
select stripe_charge_id, count(*) as row_count, array_agg(id order by id) as purchase_ids
from ai_credit_purchases
where stripe_charge_id is not null and stripe_charge_id <> ''
group by stripe_charge_id
having count(*) > 1
order by row_count desc, stripe_charge_id;
```

Malformed or empty Stripe IDs:

```sql
select id, email, stripe_customer_id, stripe_subscription_id
from users
where stripe_customer_id = ''
   or stripe_subscription_id = ''
   or (stripe_customer_id is not null and stripe_customer_id <> '' and stripe_customer_id not like 'cus_%')
   or (stripe_subscription_id is not null and stripe_subscription_id <> '' and stripe_subscription_id not like 'sub_%')
order by id;
```

Malformed purchase ledger Stripe references:

```sql
select id, user_id, stripe_checkout_session_id, stripe_payment_intent_id, stripe_charge_id, status
from ai_credit_purchases
where stripe_checkout_session_id is null
   or stripe_checkout_session_id = ''
   or stripe_checkout_session_id not like 'cs_%'
   or (stripe_payment_intent_id is not null and stripe_payment_intent_id <> '' and stripe_payment_intent_id not like 'pi_%')
   or (stripe_charge_id is not null and stripe_charge_id <> '' and stripe_charge_id not like 'ch_%')
order by id;
```

Orphaned purchase ledger rows:

```sql
select p.id, p.user_id, p.stripe_customer_id, p.stripe_checkout_session_id, p.status
from ai_credit_purchases p
left join users u on u.id = p.user_id
where u.id is null
order by p.id;
```

Purchase rows whose stored Stripe customer disagrees with the user mapping:

```sql
select p.id, p.user_id, u.email, u.stripe_customer_id as user_customer_id, p.stripe_customer_id as purchase_customer_id, p.stripe_checkout_session_id
from ai_credit_purchases p
join users u on u.id = p.user_id
where p.stripe_customer_id is not null
  and p.stripe_customer_id <> ''
  and u.stripe_customer_id is not null
  and u.stripe_customer_id <> ''
  and p.stripe_customer_id <> u.stripe_customer_id
order by p.id;
```

Inconsistent subscription states:

```sql
select id, email, plan, subscription_status, stripe_customer_id, stripe_subscription_id, current_period_end
from users
where (plan = 'PRO' and subscription_status in ('NONE', 'CANCELED') and stripe_subscription_id is null)
   or (plan = 'FREE' and subscription_status in ('ACTIVE', 'TRIALING', 'PAST_DUE'))
   or (stripe_subscription_id is not null and stripe_subscription_id <> '' and subscription_status = 'NONE')
order by id;
```

Duplicate local active/past-due/trialing subscriptions by customer:

```sql
select stripe_customer_id, count(*) as active_like_users, array_agg(id order by id) as user_ids
from users
where stripe_customer_id is not null and stripe_customer_id <> ''
  and subscription_status in ('ACTIVE', 'TRIALING', 'PAST_DUE')
group by stripe_customer_id
having count(*) > 1
order by active_like_users desc, stripe_customer_id;
```

Negative purchased-credit balances. These can be valid after refunds/disputes, but before rollout each row should be understood:

```sql
select id, email, purchased_ai_credits_balance, plan, subscription_status
from users
where purchased_ai_credits_balance < 0
order by purchased_ai_credits_balance asc, id;
```

Refund/dispute accounting inconsistencies:

```sql
select id, user_id, stripe_checkout_session_id, credits_purchased, reversed_credits,
       refund_reversal_credits, dispute_reversal_credits, dispute_restored_credits, status, dispute_status
from ai_credit_purchases
where coalesce(refund_reversal_credits, 0) < 0
   or coalesce(dispute_reversal_credits, 0) < 0
   or coalesce(dispute_restored_credits, 0) < 0
   or coalesce(dispute_restored_credits, 0) > coalesce(dispute_reversal_credits, 0)
   or coalesce(reversed_credits, 0) <> greatest(
        coalesce(refund_reversal_credits, 0),
        greatest(0, coalesce(dispute_reversal_credits, 0) - coalesce(dispute_restored_credits, 0))
      )
order by id;
```

Legacy reversed rows needing reason review before automated refund/dispute trust:

```sql
select id, user_id, stripe_checkout_session_id, stripe_payment_intent_id, stripe_charge_id,
       pack_id, credits_purchased, amount_cents, currency, status, dispute_status,
       refunded_amount_cents, reversed_credits, refunded_at, disputed_at, reversed_at
from ai_credit_purchases
where coalesce(reversed_credits, 0) > 0
order by id;
```

## Post-Migration Verification

Verify columns:

```sql
select table_name, column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name in ('processed_stripe_events', 'stripe_subscription_states', 'stripe_checkout_attempts', 'ai_credit_purchases', 'users')
  and column_name in (
    'stripe_event_created',
    'subscription_id',
    'last_event_id',
    'last_event_type',
    'last_event_created',
    'stripe_customer_id',
    'stripe_subscription_id',
    'stripe_checkout_session_id',
    'stripe_payment_intent_id',
    'stripe_charge_id',
    'refund_reversal_credits',
    'dispute_reversal_credits',
    'dispute_restored_credits'
  )
order by table_name, column_name;
```

Verify unique indexes and check constraints:

```sql
select indexname, indexdef
from pg_indexes
where schemaname = 'public'
  and indexname in (
    'ux_users_stripe_customer_id_not_null',
    'ux_users_stripe_subscription_id_not_null',
    'uk_stripe_checkout_attempts_user',
    'ux_ai_credit_purchases_payment_intent_not_null',
    'ux_ai_credit_purchases_charge_not_null'
  )
order by indexname;

select conname, pg_get_constraintdef(oid) as definition
from pg_constraint
where conrelid = 'public.ai_credit_purchases'::regclass
  and conname in (
    'chk_ai_credit_purchases_refund_reversal_nonnegative',
    'chk_ai_credit_purchases_dispute_reversal_nonnegative',
    'chk_ai_credit_purchases_dispute_restored_nonnegative',
    'chk_ai_credit_purchases_dispute_restored_lte_reversal'
  )
order by conname;
```

Verify the obsolete purchased-credit nonnegative check is absent:

```sql
select conname, pg_get_constraintdef(oid) as definition
from pg_constraint
where conrelid = 'public.users'::regclass
  and conname = 'users_purchased_ai_credits_balance_nonnegative';
```

Expected: zero rows.

Verify no post-migration accounting violations:

```sql
select count(*) as refund_dispute_accounting_violations
from ai_credit_purchases
where refund_reversal_credits < 0
   or dispute_reversal_credits < 0
   or dispute_restored_credits < 0
   or dispute_restored_credits > dispute_reversal_credits
   or reversed_credits <> greatest(refund_reversal_credits, greatest(0, dispute_reversal_credits - dispute_restored_credits));
```

## Rollback and Recovery Considerations

- If `supabase_stripe_production_safety.sql` fails, resolve duplicate `users.stripe_customer_id` or `users.stripe_subscription_id` values, then rerun. Do not drop historical user Stripe IDs without reconciling against Stripe.
- If `supabase_stripe_live_blockers.sql` fails, resolve duplicate PaymentIntent or Charge IDs before rerun. Do not fabricate missing Stripe IDs.
- If `supabase_stripe_reversal_reason_accounting.sql` fails, inspect negative or inconsistent reversal counters, repair from source records, then rerun.
- If `supabase_stripe_allow_negative_purchased_credit_balance.sql` fails, inspect the exact constraint name on `users`; the intended production state is no nonnegative check on `purchased_ai_credits_balance`.
- Added columns are non-secret and safe to keep during rollback. Dropping them after live traffic could lose audit data; prefer forward recovery.
- Added unique indexes are safety controls. Do not remove them during a live incident unless a reviewed recovery plan replaces the guarantee.
- The removed purchased-credit nonnegative check is intentionally obsolete because refund/dispute reversals may make purchased balance negative.

## Historical Data Risks

- Older credit-pack rows may be missing Checkout Session, PaymentIntent, Charge, pack type, amount, currency, customer, refund, or dispute details.
- Missing Stripe IDs must not be fabricated. Use Stripe Dashboard exports or mark rows as legacy/manual-reconciliation.
- Automated refund/dispute handling is trustworthy only for purchase rows with reliable PaymentIntent or Charge IDs and customer ownership.
- Existing nonzero `reversed_credits` is migrated conservatively into reason-aware counters; review these rows before enabling automated live refund/dispute reliance for legacy purchases.
