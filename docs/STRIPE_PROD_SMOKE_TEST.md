# Stripe Production Smoke Test

Run these tests only after migrations, Render env, Vercel env, Stripe Live webhook, and Customer Portal settings are configured. Use a controlled real payment path. Do not run destructive dispute tests in production.

## Test 1: Starter Credit Pack +25

Action:

- Sign in as the smoke-test user.
- Buy the Starter credit pack.

Verify in application:

- Billing page returns from Checkout with `credits=success`.
- Purchased-credit balance increases by exactly 25.
- Monthly PRO allowance is unchanged.

Verify in database:

```sql
select id, user_id, stripe_customer_id, stripe_checkout_session_id, stripe_payment_intent_id,
       pack_id, credits_purchased, amount_cents, currency, status,
       refunded_amount_cents, reversed_credits, refund_reversal_credits,
       dispute_reversal_credits, dispute_restored_credits, completed_at
from ai_credit_purchases
where user_id = :user_id
order by completed_at desc
limit 5;
```

Expected:

- Exactly one new purchase ledger row.
- `pack_id = 'STARTER_25'`
- `credits_purchased = 25`
- `amount_cents = 999`
- `currency = 'usd'`
- `status = 'COMPLETED'`
- Checkout Session ID starts with `cs_`
- PaymentIntent ID starts with `pi_`

Verify Stripe artifacts:

- Checkout Session exists and is paid.
- PaymentIntent exists and succeeded.
- Customer matches the TradePilot user's stored `stripe_customer_id`.
- `checkout.session.completed` event is delivered successfully.

## Test 2: PRO Monthly +100

Action:

- From a FREE smoke-test user, start monthly PRO checkout.
- Complete payment.

Verify in application:

- Dashboard returns with `billing=success`.
- User becomes PRO after webhook processing.
- Monthly AI allowance is exactly 100.
- Purchased credits, if any, are preserved.

Verify in database:

```sql
select id, email, plan, subscription_status, stripe_customer_id, stripe_subscription_id,
       current_period_start, current_period_end, stripe_cancel_at_period_end, purchased_ai_credits_balance
from users
where id = :user_id;
```

Expected:

- `plan = 'PRO'`
- `subscription_status in ('ACTIVE', 'TRIALING')`
- One `stripe_customer_id`
- One `stripe_subscription_id`
- Current period dates populated when available

Verify duplicate protection:

```sql
select stripe_customer_id, count(*) as active_like_users
from users
where stripe_customer_id = :stripe_customer_id
  and subscription_status in ('ACTIVE', 'TRIALING', 'PAST_DUE')
group by stripe_customer_id;
```

Expected: one active-like TradePilot user for the customer.

Verify Stripe artifacts:

- Customer reused if user already had a customer.
- Exactly one active subscription for the customer.
- Subscription price equals configured Live monthly PRO price.
- Webhook events are delivered and visible in `processed_stripe_events`.

## Test 3: Customer Portal

Action:

- Open Customer Portal from the Billing page.

Verify in application:

- Portal opens for the authenticated user's Stripe customer.
- Return link lands on `${APP_FRONTEND_URL}/dashboard`.

Verify in database:

```sql
select id, email, stripe_customer_id, stripe_subscription_id, plan, subscription_status
from users
where id = :user_id;
```

Expected:

- Customer and subscription IDs match the Live Stripe Portal view.

Verify Stripe artifacts:

- Portal session customer equals the user's stored `stripe_customer_id`.
- Portal displays the correct active subscription.

## Test 4: Cancel at Period End

Action:

- In Customer Portal, set the PRO subscription to cancel at period end.

Verify in application:

- User remains PRO until paid-through date.
- Billing UI reflects scheduled cancellation.
- No immediate downgrade occurs.

Verify in database:

```sql
select id, email, plan, subscription_status, stripe_subscription_id,
       current_period_end, stripe_cancel_at_period_end, stripe_cancel_at, stripe_canceled_at
from users
where id = :user_id;
```

Expected:

- `plan = 'PRO'`
- `stripe_cancel_at_period_end = true`
- `current_period_end` remains the paid-through date
- No duplicate subscription created by trying Upgrade again

Verify Stripe artifacts:

- Subscription has `cancel_at_period_end = true`.
- `customer.subscription.updated` is delivered.
- TradePilot subscription state matches Stripe.

## Final Reconciliation

Run:

```sql
select event_id, event_type, stripe_event_created, processed_at
from processed_stripe_events
where processed_at >= now() - interval '2 hours'
order by processed_at desc;

select event_type, success, duration_ms, error_message, created_at
from stripe_events
where created_at >= now() - interval '2 hours'
order by created_at desc;
```

Smoke test passes only if:

- Every expected Stripe event is delivered once or safely deduplicated.
- Every database business effect is applied exactly once.
- No accounting consistency query returns rows.
- No production validator or webhook signature errors appear for valid Live events.
