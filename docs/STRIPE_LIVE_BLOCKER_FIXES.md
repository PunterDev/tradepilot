# Stripe Live Blocker Fixes

## Subscription Event Ordering

Stripe event `created` timestamps are only second-precision, so TradePilot does not treat equal-second events as last-arrival-wins. The application persists the Stripe event id, type, and created timestamp for each subscription to ignore strictly older events, but sensitive equal-second subscription changes are reconciled against Stripe's current Subscription object before mutating entitlement.

For `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.payment_succeeded`, `invoice_payment.paid`, `invoice.payment_failed`, and `invoice_payment.payment_failed`, the current Stripe Subscription is retrieved when a subscription id is available. The retrieved subscription status is the authority for PRO/FREE entitlement and billing status. Webhook payload state is only a fallback when no current subscription object can be obtained from the Stripe client.

If Stripe retrieval fails with an exception, processing fails retryably and no destructive downgrade is applied from the uncertain webhook payload.

## Livemode Enforcement

When the `prod` or `production` Spring profile is active, webhook payloads must have `livemode: true` after signature verification. Test-mode events are rejected before billing state or purchased credits are mutated. Dev and test profiles continue to allow test-mode events.

## Duplicate Subscription Checkout Policy

Before creating a PRO subscription Checkout Session, TradePilot locks the local user row and blocks checkout when local status is `ACTIVE`, `TRIALING`, or `PAST_DUE`. If local state is stale but a stored Stripe subscription id exists, the service retrieves the current Stripe subscription and blocks checkout when Stripe reports an eligible subscription.

A per-user `stripe_checkout_attempts` row with a unique `user_id` backs recent checkout attempts. A pending attempt blocks duplicate checkout creation for 30 minutes, covering double-clicks and concurrent requests. CANCELED users without an eligible current Stripe subscription can create a new checkout.

## Credit-Pack Refund And Dispute Policy

Credit-pack purchases are represented by a durable ledger row containing the TradePilot user, Stripe customer, Checkout Session, PaymentIntent, Charge, pack, credits, amount, currency, status, refunded amount, reversed credits, and timestamps.

Policies:

- Full refund: record refunded amount and apply one idempotent negative purchased-credit adjustment for the original pack credits.
- Partial refund: record the partial refund and mark the purchase partially refunded; no automatic credit adjustment is made.
- Dispute created: apply one idempotent negative purchased-credit adjustment for the original pack credits and mark disputed.
- Dispute won: restore any previously reversed dispute credits and mark dispute won.
- Dispute lost: mark dispute lost and ensure the full purchased-credit reversal has been applied once.

Purchased-credit reversals affect only `purchased_ai_credits_balance`. Monthly PRO allowance credits are not modified. If purchased credits were already consumed, the purchased-credit balance may go negative so entitlement accounting remains auditable instead of silently corrupting balances.
