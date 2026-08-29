# Executive Summary

TradePilot's recent Stripe fixes materially improved webhook signature handling, sequential/concurrent event idempotency, payment-failure grace behavior, and credit-pack fulfillment validation. The implementation is still not ready for Live Mode.

The main unresolved risk is subscription ordering. The new stale-event guard compares only Stripe `event.created`, which has one-second precision. Equal-second events are accepted in arrival order, so an older subscription state can still overwrite a newer one. The webhook also applies sensitive entitlement changes from webhook payloads without retrieving Stripe's current subscription state for terminal or recovery transitions. Those two issues are enough for a NO-GO.

## Critical

### Equal-second Stripe events can overwrite newer subscription state

- file: `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- line: 240
- issue: `claimSubscriptionStateEvent` only ignores events where `stripeEventCreated.isBefore(lastEventCreated)`. Equal timestamps are treated as fresh and applied in HTTP arrival order.
- exploit/failure scenario: Stripe emits `customer.subscription.updated` and `customer.subscription.deleted` for the same subscription inside the same second. If the true newer state is processed first, a delayed equal-second older event is still accepted and can flip PRO/FREE, ACTIVE/PAST_DUE/CANCELED, or cancellation metadata incorrectly.
- customer/revenue impact: Users can lose paid access or retain access after cancellation depending on retry/order timing. Support will see internally consistent audit rows but the wrong entitlement.
- recommended fix: Add deterministic ordering for equal timestamps. Store and compare a per-subscription version derived from Stripe object state where available, such as subscription `updated` if expanded/available, terminal-state precedence, or a source-of-truth Stripe retrieval before applying state. At minimum, define same-second tie behavior explicitly and test deleted/updated/payment_failed/paid ties.
- whether it blocks Live Mode: Yes.

### Entitlement changes are applied from event payload without source-of-truth verification

- file: `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- line: 408
- issue: `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.payment_succeeded`, and `invoice.payment_failed` mutate local entitlement from webhook payloads after the timestamp check. Terminal downgrades and recoveries do not retrieve the current Stripe subscription before applying.
- exploit/failure scenario: A delayed event with later `event.created` but stale object semantics, an invoice event that conflicts with a subscription event, or a retry after subscription recovery can apply the wrong state. `lookupInvoiceSubscriptionPeriod` retrieves the subscription for period data, but the returned subscription status is not used as the entitlement source of truth.
- customer/revenue impact: A paid customer can be downgraded after recovery, or a canceled/unpaid customer can remain PRO from a later-arriving invoice/subscription payload.
- recommended fix: For entitlement-sensitive transitions, retrieve the current Stripe subscription by ID and apply local state from Stripe's current subscription status. Use webhook payloads to trigger reconciliation, not as the final authority for PRO/FREE on terminal/recovery edges.
- whether it blocks Live Mode: Yes.

## High

### Production does not verify Stripe event livemode

- file: `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- line: 153
- issue: After signature verification, the webhook does not reject events whose top-level `livemode` is false when running under `prod` or `production`.
- exploit/failure scenario: A production deployment accidentally configured with a test-mode endpoint secret that does not contain the literal word `test` can process signed test events against real users. Stripe `whsec_` values do not encode live/test mode in a way the current validator can prove.
- customer/revenue impact: Test Checkout or subscription events could grant PRO or paid credits in production, or test cancellation events could downgrade live users.
- recommended fix: In production profiles, require `root.path("livemode").asBoolean(false)` to be true before any business logic or processed-event insert. Add tests for prod rejecting signed `livemode=false` events.
- whether it blocks Live Mode: Yes.

### Concurrent duplicate credit-pack sessions can throw instead of returning consumed

- file: `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- line: 364
- issue: Credit-pack fulfillment checks `existsByStripeCheckoutSessionId` before updating the user, but it does not catch a unique-key violation on `AiCreditPurchase` for two different Stripe events with the same Checkout Session processed concurrently.
- exploit/failure scenario: Two valid signed events with different event IDs but the same Checkout Session pass the pre-check on different app instances. One commits. The other increments the user in-memory and then hits the `uk_ai_credit_purchases_checkout_session` constraint. The transaction should roll back, so double credits are unlikely, but the HTTP response can be 500, inviting Stripe retries.
- customer/revenue impact: No durable duplicate credits expected if the transaction rolls back correctly, but operators can get retry noise and alarming errors for an already-fulfilled purchase.
- recommended fix: Catch `DataIntegrityViolationException` around the purchase insert and treat it as already fulfilled if the session row exists. Prefer inserting/claiming the purchase row first, then increment credits only after the session claim succeeds.
- whether it blocks Live Mode: No, but fix before scale-up.

### Subscription Checkout can create multiple active Pro subscriptions for one user

- file: `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- line: 50
- issue: `createCheckoutSession` always creates a subscription Checkout Session for an authenticated user and does not check for an existing active/trialing/past_due Stripe subscription or open checkout session.
- exploit/failure scenario: A user double-clicks, retries across tabs, or starts monthly and yearly flows before the first completes. Stripe can create multiple subscription sessions and potentially multiple active subscriptions for the same customer.
- customer/revenue impact: Duplicate customer billing, refund/support load, and confusing webhook state where one subscription cancellation can downgrade a user who still has another active subscription.
- recommended fix: Before creating a subscription Checkout Session, retrieve existing active subscriptions for the customer or persist an internal pending checkout/session guard. Prefer redirecting existing PRO users to the Customer Portal.
- whether it blocks Live Mode: Yes unless Stripe dashboard/customer portal settings or operational process already prevent it.

### Refunds, disputes, and chargebacks still do not reverse credits or entitlement

- file: `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- line: 180
- issue: The webhook switch handles checkout, subscription, and invoice events only. It ignores refund/dispute events.
- exploit/failure scenario: A user buys an Elite pack, receives 200 credits, then refunds or disputes the charge. No local reversal, hold, or audit marker is applied.
- customer/revenue impact: Revenue leakage, disputed paid access, and poor accounting reconciliation.
- recommended fix: Handle `charge.refunded`, `charge.dispute.created`, `charge.dispute.closed`, and/or `checkout.session.async_payment_failed`/payment intent events relevant to the chosen Stripe flow. Decide whether to revoke unused purchased credits, freeze account access, or flag for manual review.
- whether it blocks Live Mode: Yes for credit packs.

## Medium

### Production config validator cannot prove webhook secret mode

- file: `backend/src/main/java/com/tradepilot/billing/StripeProductionConfigValidator.java`
- line: 42
- issue: The validator accepts any `whsec_` value that does not contain `test`. Stripe webhook secrets do not have a live/test prefix like secret keys.
- exploit/failure scenario: A test endpoint secret with a live-looking random value passes validation.
- customer/revenue impact: Same class of test-event production mutation as above if paired with missing `livemode` verification.
- recommended fix: Keep the shape validation, but rely on production webhook `livemode` checks and deployment runbook verification for actual mode assurance.
- whether it blocks Live Mode: It contributes to a Live blocker until `livemode` is enforced.

### Migration creates unique indexes non-concurrently

- file: `backend/supabase_stripe_production_safety.sql`
- line: 58
- issue: `create unique index if not exists` is safe to retry but can lock writes on `users` while building indexes. Supabase/PostgreSQL production tables can be affected if the table is large or under write load.
- exploit/failure scenario: Deploying during active signups or billing webhook traffic blocks writes or causes timeouts.
- customer/revenue impact: Signup, webhook, or billing writes may stall during migration.
- recommended fix: For production, run duplicate preflight manually first, then create the partial unique indexes with `CREATE UNIQUE INDEX CONCURRENTLY` in a migration mechanism that does not wrap the command in a transaction. Schedule during low traffic.
- whether it blocks Live Mode: No if run during a controlled maintenance window; otherwise operational risk.

### Observability records are aggregate events, not a full Stripe event audit trail

- file: `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- line: 200
- issue: `ProcessedStripeEvent` stores event ID/type/created time, but not outcome, user ID, customer ID, subscription ID, session ID, stale/duplicate reason, or sanitized failure detail. Logs contain many of these values, but not in a durable queryable audit row.
- exploit/failure scenario: Support asks "why did this user become FREE?" after log retention expires or across multiple webhook retries. The database can show that an event was processed, but not the applied business decision.
- customer/revenue impact: Slower incident response and weaker financial forensics.
- recommended fix: Extend the Stripe event audit table or add a separate audit table with event ID, created, received, type, outcome, user ID, customer ID, subscription ID, session ID, and sanitized error/result reason.
- whether it blocks Live Mode: No, but high operational value before launch.

### Stripe customer validation runs synchronously at startup

- file: `backend/src/main/java/com/tradepilot/billing/StripeCustomerValidationRunner.java`
- line: 20
- issue: Startup iterates all users with Stripe customer IDs and calls Stripe synchronously.
- exploit/failure scenario: As user count grows, deployment startup depends on Stripe API latency/rate limits. A single StripeException exits the loop, but only after already delaying startup.
- customer/revenue impact: Slower deploys and potentially delayed recovery during incidents.
- recommended fix: Move this to an admin/background health job with pagination, timeout limits, and sampled validation. Do not block app boot on external Stripe reads.
- whether it blocks Live Mode: No for small launch, post-launch high priority.

### MRR/ARR remains hardcoded to monthly price

- file: `backend/src/main/java/com/tradepilot/service/AdminBillingService.java`
- line: 24
- issue: Admin MRR uses a hardcoded `29.99` monthly price and counts only ACTIVE/TRIALING PRO subscriptions. It does not reflect yearly subscriptions, discounts, taxes, refunds, or PAST_DUE grace subscriptions.
- exploit/failure scenario: Admin billing dashboard reports misleading revenue after yearly subscriptions or promotions go live.
- customer/revenue impact: Revenue analytics drift; not an entitlement integrity problem.
- recommended fix: Derive revenue from Stripe prices/subscription items or store billing interval/amount from Checkout/subscription webhooks.
- whether it blocks Live Mode: No, post-launch normal.

## Low

### Checkout metadata includes email even though ownership is customer-based

- file: `backend/src/main/java/com/tradepilot/billing/DefaultStripeApiClient.java`
- line: 57
- issue: `userEmail` is written into Checkout metadata. Current webhook ownership does not use email as authority, which is good, but unnecessary personal data increases exposure in Stripe/log exports.
- exploit/failure scenario: Metadata is visible to admins and exports; future code could accidentally trust it.
- customer/revenue impact: Low privacy/minimization concern.
- recommended fix: Remove `userEmail` metadata unless needed for support; keep `userId` plus Stripe customer mapping.
- whether it blocks Live Mode: No.

# Confirmed Protections

- Stripe webhook route is unauthenticated by JWT but isolated in a dedicated permit-all security chain for `/api/stripe/webhook`; business logic is protected by Stripe signature verification.
- Missing `Stripe-Signature` returns 400 before calling `StripeBillingService`.
- Invalid signatures and malformed payloads return 400 and do not insert processed-event records.
- Duplicate Stripe event IDs are skipped sequentially and under a concurrent race through the database unique constraint.
- Stale events with strictly older `event.created` are ignored for subscription/invoice state changes.
- `invoice.payment_failed` marks the subscription PAST_DUE while preserving PRO entitlement.
- `customer.subscription.deleted` and terminal subscription statuses downgrade to FREE.
- Credit-pack fulfillment validates mode, payment status, pack ID, currency, amount, and stored Stripe customer mapping before granting credits.
- Credit-pack grants use server-controlled catalog values, not metadata `credits`.
- Different event IDs for the same Checkout Session do not grant credits twice in the sequential path.
- `ai_credit_purchases.stripe_checkout_session_id` has a database uniqueness constraint.
- The new migration preflights duplicate non-null/non-empty Stripe customer and subscription IDs before adding partial unique indexes.
- Production URL/CORS validation already rejects localhost frontend/CORS values under `prod`/`production`.

# Tests Added

Added to `backend/src/test/java/com/tradepilot/StripeBillingTests.java`:

- `creditPackNoPaymentRequiredSessionGrantsZeroCredits`
- `creditPackWrongCheckoutModeGrantsZeroCredits`
- `creditPackMissingCustomerGrantsZeroCredits`
- `differentStripeEventsForSameCreditPackSessionGrantCreditsOnlyOnce`

Existing recent tests already covered stale strictly-older webhook events, payment failure/recovery/cancellation, production config validation, valid Starter/Power/Elite purchases, duplicate event IDs, and concurrent duplicate event IDs.

# Test Results

- `mvn -Dtest=StripeBillingTests,StripeProductionConfigValidatorTests test`
  - Tests run: 47
  - Failures: 0
  - Errors: 0
  - Skipped: 0
  - Result: BUILD SUCCESS

- `mvn test`
  - Tests run: 368
  - Failures: 0
  - Errors: 0
  - Skipped: 0
  - Result: BUILD SUCCESS

# Production Migration Risks

`backend/supabase_stripe_production_safety.sql` is logically safe and retryable for already-added columns/tables/indexes, but production execution needs care:

- The preflight duplicate checks correctly ignore null and empty-string Stripe IDs.
- The partial unique index predicates match current application semantics for non-null/non-empty customer/subscription IDs.
- `stripe_event_created timestamptz` and JPA `LocalDateTime` are acceptable if the application consistently converts Stripe epoch seconds using UTC, which it currently does.
- The unique indexes are not created concurrently and can lock writes.
- If historical duplicates exist, the migration intentionally fails before adding the partial unique indexes.

Manual SQL to run before production migration:

```sql
select stripe_customer_id, count(*), array_agg(id order by id) as user_ids
from users
where stripe_customer_id is not null and stripe_customer_id <> ''
group by stripe_customer_id
having count(*) > 1;

select stripe_subscription_id, count(*), array_agg(id order by id) as user_ids
from users
where stripe_subscription_id is not null and stripe_subscription_id <> ''
group by stripe_subscription_id
having count(*) > 1;

select count(*) as users_with_customer_id
from users
where stripe_customer_id is not null and stripe_customer_id <> '';

select count(*) as users_with_subscription_id
from users
where stripe_subscription_id is not null and stripe_subscription_id <> '';
```

# Manual Stripe Dashboard Tasks Still Required

- Confirm production webhook endpoint URL points to the deployed backend `/api/stripe/webhook`.
- Confirm webhook endpoint is Live Mode and subscribed to required events: checkout completion, subscription updated/deleted, invoice paid/succeeded, invoice payment failed, and refund/dispute events once implemented.
- Confirm webhook signing secret in production comes from the Live Mode endpoint.
- Confirm monthly and yearly PRO price IDs are Live Mode, active, and mapped to the intended product.
- Confirm Customer Portal settings allow the intended subscription cancellation/update flows and do not permit unsupported plan switches.
- Confirm no test webhook endpoints point to production.
- Confirm deployed `APP_FRONTEND_URL`, allowed origins, Stripe keys, and price IDs are production values.

# Remaining Live Blockers

- Equal-second Stripe subscription events have no deterministic tie-breaker and can overwrite newer state.
- Entitlement-sensitive transitions do not reconcile against Stripe's current subscription state before applying.
- Production webhooks do not enforce `livemode=true`.
- Subscription Checkout does not prevent duplicate active subscriptions for one user/customer.
- Refund/dispute handling for credit packs is not implemented.

# Stripe Live Readiness

NO-GO
