# Stripe Production Reconciliation

Use this after migrations, smoke tests, webhook failures, refunds, disputes, or support investigations. These queries are read-only.

## User Entitlement

Why did this user become PRO/FREE?

```sql
select id, email, plan, subscription_status,
       stripe_customer_id, stripe_subscription_id, current_period_start, current_period_end,
       stripe_cancel_at_period_end, stripe_cancel_at, stripe_canceled_at, pro_until, updated_at
from users
where id = :user_id;
```

Recent processed subscription events:

```sql
select event_id, event_type, stripe_event_created, processed_at
from processed_stripe_events
where event_type in (
  'checkout.session.completed',
  'customer.subscription.updated',
  'customer.subscription.deleted',
  'invoice.payment_succeeded',
  'invoice.payment_failed',
  'invoice_payment.paid',
  'invoice_payment.payment_failed'
)
order by processed_at desc
limit 50;
```

Subscription stale-event guard state:

```sql
select subscription_id, last_event_id, last_event_type, last_event_created, updated_at
from stripe_subscription_states
where subscription_id = :stripe_subscription_id;
```

## Credit Balance

Why does this user have this credit balance?

```sql
select id, email, plan, subscription_status, purchased_ai_credits_balance
from users
where id = :user_id;
```

Credit-pack ledger:

```sql
select id, user_id, stripe_customer_id, stripe_checkout_session_id, stripe_payment_intent_id, stripe_charge_id,
       pack_id, credits_purchased, amount_cents, currency, status,
       refunded_amount_cents, reversed_credits,
       refund_reversal_credits, dispute_reversal_credits, dispute_restored_credits,
       dispute_status, completed_at, refunded_at, disputed_at, reversed_at, created_at
from ai_credit_purchases
where user_id = :user_id
order by created_at desc, id desc;
```

AI credit usage:

```sql
select id, user_id, usage_type, credits_used, created_at
from ai_usage_events
where user_id = :user_id
order by created_at desc, id desc
limit 100;
```

## Purchase Lookup

By Checkout Session:

```sql
select *
from ai_credit_purchases
where stripe_checkout_session_id = :checkout_session_id;
```

By PaymentIntent:

```sql
select *
from ai_credit_purchases
where stripe_payment_intent_id = :payment_intent_id;
```

By Charge:

```sql
select *
from ai_credit_purchases
where stripe_charge_id = :charge_id;
```

## Event Traceability

Which Stripe event caused this change?

```sql
select event_id, event_type, stripe_event_created, processed_at
from processed_stripe_events
where event_id = :event_id;
```

Recent Stripe observability metrics:

```sql
select event_type, success, duration_ms, error_message, created_at
from stripe_events
order by created_at desc
limit 100;
```

## Refund and Dispute Checks

Was this purchase refunded or disputed?

```sql
select id, stripe_checkout_session_id, stripe_payment_intent_id, stripe_charge_id,
       status, dispute_status, refunded_amount_cents, reversed_credits,
       refund_reversal_credits, dispute_reversal_credits, dispute_restored_credits,
       refunded_at, disputed_at, reversed_at
from ai_credit_purchases
where id = :purchase_id;
```

Accounting consistency:

```sql
select id, user_id, stripe_checkout_session_id, credits_purchased, reversed_credits,
       refund_reversal_credits, dispute_reversal_credits, dispute_restored_credits,
       greatest(refund_reversal_credits, greatest(0, dispute_reversal_credits - dispute_restored_credits)) as expected_reversed_credits
from ai_credit_purchases
where id = :purchase_id;
```

Global accounting exceptions:

```sql
select id, user_id, stripe_checkout_session_id, status, dispute_status, credits_purchased,
       reversed_credits, refund_reversal_credits, dispute_reversal_credits, dispute_restored_credits
from ai_credit_purchases
where reversed_credits <> greatest(refund_reversal_credits, greatest(0, dispute_reversal_credits - dispute_restored_credits))
   or dispute_restored_credits > dispute_reversal_credits
   or refund_reversal_credits < 0
   or dispute_reversal_credits < 0
   or dispute_restored_credits < 0
order by id;
```

## Support Tooling Assessment

Support can answer with current admin UI:

- Current plan, subscription status, Stripe customer ID, and subscription ID.
- High-level billing readiness flags.
- Stripe webhook failure trends through observability.
- Recent manual admin upgrades/downgrades.

Support needs SQL or Stripe Dashboard for:

- Exact Stripe event that caused an entitlement transition.
- Full explanation of purchased-credit balance.
- Refund/dispute reason accounting for a specific purchase.
- Business-level idempotency proof for a specific Checkout Session.

Non-blocking tooling improvements:

- Add an admin per-user billing ledger view.
- Add a purchase detail view with Checkout Session, PaymentIntent, Charge, refund, and dispute fields.
- Add a plan-transition timeline that joins user state, processed events, and subscription-state guard rows.
- Add an admin reconciliation export for Stripe support cases.
