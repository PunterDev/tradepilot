# TradePilot Final Stripe Live Production Readiness Audit

Audit date: 2026-08-29

Scope: backend Stripe/billing implementation, frontend billing integration, Supabase/PostgreSQL migrations, production configuration, deployment order, smoke testing, rollback, and security search.

Constraints honored: no Stripe Live interaction, no live keys used, no production SQL executed.

## 1. Fresh Test Results

Command:

```bash
cd backend
mvn -Dtest=StripeBillingTests,StripeWebhookLiveModeTests,com.tradepilot.billing.StripeProductionConfigValidatorTests test
```

Result:

| Suite | Tests run | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Stripe/billing-focused tests | 81 | 0 | 0 | 0 |

Command:

```bash
cd backend
mvn test
```

Result:

| Suite | Tests run | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Full backend tests | 402 | 0 | 0 | 0 |

## 2. Final Findings

### CRITICAL: Credit-pack fulfillment is not concurrency-safe at the business idempotency boundary

- File: `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- Lines: 488-536
- Problem: `handleCreditPackCheckoutCompleted` checks `existsByStripeCheckoutSessionId(sessionId)` before acquiring the user row lock, then increments `purchasedAiCreditsBalance` before inserting/flushing the unique purchase ledger row. If two distinct Stripe events, retries, or app instances concurrently process the same Checkout Session, both can pass the pre-check. The second transaction can wait on the user lock, increment credits, then hit the unique checkout-session constraint. The catch at lines 527-530 treats the duplicate as already applied and returns, but the user's second credit increment has already occurred inside the same transaction.
- Production impact: A single paid credit-pack session can grant credits more than once under a concurrent same-session race. This violates business-level idempotency for paid credit fulfillment.
- Recommended fix: Make the purchase ledger insert the authoritative first mutation inside the user lock, or re-check `existsByStripeCheckoutSessionId` after acquiring the user lock and before changing credits. On duplicate-key, ensure the transaction rolls back all user balance changes. Add a true concurrency test for two different event IDs referencing the same Checkout Session.

### CRITICAL: Refund/dispute reversal state is not reason-aware

- File: `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- Lines: 539-659
- Problem: Refunds and disputes share one `reversedCredits` field. `restoreDisputedCredits` restores all reversed credits when a dispute is won, regardless of whether the reversal came from a dispute or from a full refund. A sequence such as full refund first, then `charge.dispute.closed` with `status=won`, can restore credits for a refunded purchase.
- Production impact: A refunded customer can regain purchased credits if refund and dispute events arrive in an unfavorable order for the same purchase.
- Recommended fix: Use ledger-aware reversal records or separate counters such as `refund_reversed_credits` and `dispute_reversed_credits`. Only restore dispute-created reversals on dispute won; never restore refund reversals unless an explicit refund reversal/charge recovery event is supported and verified. Add tests for refund-then-dispute-won and refund-then-dispute-lost.

### HIGH: Sensitive reconciliation can fall back to webhook payload if Stripe retrieval returns null

- File: `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- Lines: 258-284, 672-749
- Problem: `retrieveCurrentSubscriptionForSensitiveEvent` throws on Stripe API failure, which is correct, but if the API client returns `null`, the code logs a warning and falls back to the signed webhook payload. The real Stripe SDK normally returns an object or throws, so this is unlikely in production, but the fail-open branch remains in sensitive entitlement code.
- Production impact: A client bug, wrapper bug, or unexpected null path can allow stale destructive payload state to apply.
- Recommended fix: Treat null subscription retrieval for sensitive events as retryable failure, at least for destructive transitions and payment failures.

### MEDIUM: Subscription Checkout Session creation does not use Stripe API idempotency keys

- File: `backend/src/main/java/com/tradepilot/billing/DefaultStripeApiClient.java`
- Lines: 46-60
- Problem: Duplicate Pro checkout is blocked locally by user row locking and `stripe_checkout_attempts`, but the Stripe Checkout Session API call itself is not idempotency-keyed.
- Production impact: If Stripe creates a Checkout Session and the backend transaction fails before persisting the returned session ID, a retry can create another viable Stripe-hosted subscription Checkout Session.
- Recommended fix: Use a short-lived, deterministic Stripe idempotency key per logical Pro checkout attempt, tied to the local `stripe_checkout_attempts` row.

## 3. Protection Verification Matrix

| Protection | Status | Evidence |
|---|---|---|
| Stripe signature validation | VERIFIED BY BOTH | `Webhook.constructEvent` in `StripeBillingService.java:170`; webhook tests exercise signed payloads. |
| Production livemode enforcement | VERIFIED BY BOTH | `isWebhookLiveModeAllowed` in `StripeBillingService.java:251-256`; `StripeWebhookLiveModeTests` cover production false/true and test false. |
| Stripe event idempotency by event ID | VERIFIED BY BOTH | `processed_stripe_events.event_id` unique JPA field and save/flush before side effects; duplicate tests pass. |
| Concurrent duplicate event ID handling | VERIFIED BY CODE | `DataIntegrityViolationException` around processed event insert is treated as already processed at `StripeBillingService.java:214-219`. |
| Business-level credit-pack idempotency | NOT VERIFIED | Sequential duplicate session tests exist, but the code mutates credits before ledger insert under a concurrent same-session race. |
| Stale/out-of-order subscription event protection | VERIFIED BY BOTH | `stripe_subscription_states` and `last_event_created` guard at `StripeBillingService.java:301-334`; stale tests pass. |
| Equal-second event handling | VERIFIED BY BOTH | Equal-created tests exist and sensitive events reconcile against current Stripe subscription state. |
| Source-of-truth subscription reconciliation | VERIFIED BY BOTH | `retrieveCurrentSubscriptionForSensitiveEvent` is called for subscription and invoice state events; tests cover active/canceled/API failure/customer mismatch. |
| Payment failure grace behavior | VERIFIED BY BOTH | `PAST_DUE` maps to PRO entitlement at `StripeBillingService.java:971-987`; tests cover failure, recovery, cancellation. |
| Customer ownership | VERIFIED BY BOTH | Credit pack requires stored customer match; subscription reconciliation verifies customer mapping. |
| Subscription ownership | VERIFIED BY BOTH | Source-of-truth subscription lookup maps subscription/customer to one TradePilot user. |
| Duplicate active subscription checkout prevention | VERIFIED BY BOTH | User row lock, local entitlement check, Stripe reconciliation, and checkout-attempt table block normal double-click. |
| Credit-pack catalog validation | VERIFIED BY BOTH | Mode, paid status, currency, amount, pack ID, user/customer checked at `StripeBillingService.java:488-536,665-670`; negative tests pass. |
| Credit-pack purchase ledger | VERIFIED BY BOTH | `AiCreditPurchase` records session, customer, PaymentIntent, Charge, pack, amount, currency, status; tests cover valid packs. |
| Refund handling | VERIFIED BY BOTH for basic full/partial/duplicate cases; NOT VERIFIED for refund/dispute interleavings | Basic refund tests pass; reason-aware reversal is missing. |
| Dispute handling | VERIFIED BY BOTH for created/won/lost basics; NOT VERIFIED for refund/dispute interleavings | Basic dispute tests pass; shared reversal counter is unsafe. |
| Transaction boundaries | VERIFIED BY CODE, partially by tests | Webhook processing uses `TransactionTemplate`; credit-pack concurrent same-session boundary is unsafe. |
| Production Stripe config validation | VERIFIED BY BOTH | `StripeProductionConfigValidator.java` rejects test secret keys, missing webhook secret, missing monthly/yearly prices, invalid credit pack catalog. |
| Logging/observability | VERIFIED BY CODE | Stripe events are recorded through `ObservabilityService`; logs avoid raw secrets and summarize errors. |
| Frontend cannot pass arbitrary Stripe price/customer | VERIFIED BY CODE | Frontend calls backend endpoints only; backend accepts billing cycle or enum pack ID, not price/customer IDs. |

## 4. Database Migration Review

Reviewed:

- `backend/supabase_stripe_production_safety.sql`
- `backend/supabase_stripe_live_blockers.sql`

Findings:

- SQL is PostgreSQL/Supabase-compatible.
- Application order is clear: apply `supabase_stripe_production_safety.sql` before `supabase_stripe_live_blockers.sql`.
- `processed_stripe_events.stripe_event_created` and `stripe_subscription_states` match the current JPA fields.
- `timestamptz` with Hibernate UTC configuration is acceptable for Stripe event timestamps.
- User `stripe_customer_id` and `stripe_subscription_id` indexes are partial and match non-null/non-empty semantics.
- PaymentIntent and Charge indexes are partial and appropriate because historical rows may be null.
- Checkout Session uniqueness comes from the earlier `backend/supabase_ai_credit_packs.sql` constraint `ai_credit_purchases_session_unique`, not from the live-blocker migration.
- Both migrations are mostly rerunnable through `IF NOT EXISTS` and `ADD COLUMN IF NOT EXISTS`.
- Partial failure can leave added nullable columns before preflight failures; rerun is safe after fixing data, but operators must not assume all indexes were created unless schema verification passes.

### Required Preflight SQL Before Applying Migrations

Do not execute these against production until the deployment window is ready. Run before applying either migration.

```sql
-- 1. Duplicate stripe_customer_id
select stripe_customer_id, count(*) as row_count, array_agg(id order by id) as user_ids
from users
where stripe_customer_id is not null and btrim(stripe_customer_id) <> ''
group by stripe_customer_id
having count(*) > 1;

-- 2. Duplicate stripe_subscription_id
select stripe_subscription_id, count(*) as row_count, array_agg(id order by id) as user_ids
from users
where stripe_subscription_id is not null and btrim(stripe_subscription_id) <> ''
group by stripe_subscription_id
having count(*) > 1;

-- 3. Duplicate checkout_session_id
select stripe_checkout_session_id, count(*) as row_count, array_agg(id order by id) as purchase_ids
from ai_credit_purchases
where stripe_checkout_session_id is not null and btrim(stripe_checkout_session_id) <> ''
group by stripe_checkout_session_id
having count(*) > 1;

-- 4. Duplicate payment_intent_id
select stripe_payment_intent_id, count(*) as row_count, array_agg(id order by id) as purchase_ids
from ai_credit_purchases
where stripe_payment_intent_id is not null and btrim(stripe_payment_intent_id) <> ''
group by stripe_payment_intent_id
having count(*) > 1;

-- 4b. Duplicate charge_id
select stripe_charge_id, count(*) as row_count, array_agg(id order by id) as purchase_ids
from ai_credit_purchases
where stripe_charge_id is not null and btrim(stripe_charge_id) <> ''
group by stripe_charge_id
having count(*) > 1;

-- 5. Malformed/empty Stripe IDs on users
select id, email, stripe_customer_id, stripe_subscription_id
from users
where (stripe_customer_id is not null and (btrim(stripe_customer_id) = '' or stripe_customer_id not like 'cus_%'))
   or (stripe_subscription_id is not null and (btrim(stripe_subscription_id) = '' or stripe_subscription_id not like 'sub_%'));

-- 5b. Malformed/empty Stripe IDs on credit-pack purchases
select id, user_id, stripe_checkout_session_id, stripe_payment_intent_id, stripe_charge_id, stripe_customer_id
from ai_credit_purchases
where (stripe_checkout_session_id is not null and (btrim(stripe_checkout_session_id) = '' or stripe_checkout_session_id not like 'cs_%'))
   or (stripe_payment_intent_id is not null and (btrim(stripe_payment_intent_id) = '' or stripe_payment_intent_id not like 'pi_%'))
   or (stripe_charge_id is not null and (btrim(stripe_charge_id) = '' or stripe_charge_id not like 'ch_%'))
   or (stripe_customer_id is not null and (btrim(stripe_customer_id) = '' or stripe_customer_id not like 'cus_%'));

-- 6. Orphaned credit-pack purchases
select p.*
from ai_credit_purchases p
left join users u on u.id = p.user_id
where u.id is null;

-- 6b. Credit-pack purchases missing durable Stripe references needed for refund/dispute automation
select id, user_id, stripe_checkout_session_id, stripe_payment_intent_id, stripe_charge_id, stripe_customer_id,
       pack_id, credits_purchased, amount_cents, currency, status
from ai_credit_purchases
where stripe_checkout_session_id is null
   or btrim(stripe_checkout_session_id) = ''
   or stripe_payment_intent_id is null
   or btrim(stripe_payment_intent_id) = ''
   or stripe_customer_id is null
   or btrim(stripe_customer_id) = ''
   or pack_id is null
   or btrim(pack_id) = ''
   or amount_cents is null
   or currency is null
   or btrim(currency) = '';

-- 7. Users with inconsistent subscription state
select id, email, plan, subscription_status, stripe_customer_id, stripe_subscription_id, current_period_end
from users
where (plan = 'PRO' and subscription_status in ('NONE', 'CANCELED'))
   or (plan = 'FREE' and subscription_status in ('ACTIVE', 'TRIALING', 'PAST_DUE'))
   or (subscription_status in ('ACTIVE', 'TRIALING', 'PAST_DUE')
       and (stripe_customer_id is null or btrim(stripe_customer_id) = ''
            or stripe_subscription_id is null or btrim(stripe_subscription_id) = ''));
```

### Schema Verification SQL After Migrations

```sql
select indexname, indexdef
from pg_indexes
where schemaname = 'public'
  and indexname in (
    'ux_users_stripe_customer_id_not_null',
    'ux_users_stripe_subscription_id_not_null',
    'ux_ai_credit_purchases_payment_intent_not_null',
    'ux_ai_credit_purchases_charge_not_null',
    'uk_stripe_checkout_attempts_user',
    'idx_processed_stripe_events_stripe_created',
    'idx_stripe_subscription_states_last_event_created'
  )
order by indexname;

select table_name, column_name, data_type, is_nullable
from information_schema.columns
where table_schema = 'public'
  and table_name in ('processed_stripe_events', 'stripe_subscription_states', 'stripe_checkout_attempts', 'ai_credit_purchases')
order by table_name, ordinal_position;
```

## 5. Historical Data Backfill Review

Refund/dispute automation is trustworthy only for one-time credit-pack rows that can be tied to Stripe payment identity.

### SAFE

Rows are safe for automated refund/dispute handling when all are present and consistent:

- TradePilot `user_id`
- `stripe_customer_id`
- `stripe_checkout_session_id`
- `stripe_payment_intent_id`
- `pack_id`
- `credits_purchased`
- `amount_cents`
- `currency`
- `status`
- Optional but preferred: `stripe_charge_id`

### BACKFILL REQUIRED

Rows require backfill when the Checkout Session or PaymentIntent is known from Stripe webhook logs, Stripe Dashboard export, or support records but columns are null:

- `stripe_customer_id`
- `stripe_payment_intent_id`
- `stripe_charge_id`
- `pack_id`
- `amount_cents`
- `currency`

Backfill only from authoritative Stripe objects or previously captured webhook payloads. Do not infer Stripe IDs from amount, email, or timestamps alone.

### MANUAL REVIEW REQUIRED

Rows require manual reconciliation when any of the following are true:

- Missing Checkout Session ID.
- Missing PaymentIntent ID and no authoritative source can provide it.
- Customer ID conflicts with the user's current Stripe customer.
- Amount/currency does not match the server catalog.
- Multiple rows point at the same payment identity.
- Legacy rows predate ledger-backed fulfillment.

Recommended strategy: mark unreliable historical rows as legacy/manual-reconciliation records by keeping unknown Stripe references null, preserving the current credit balance, and excluding them from automatic refund/dispute reversal unless Stripe identity is proven.

## 6. Render Environment Variable Checklist

Property names were verified in `backend/src/main/resources/application.yml` and `StripeProperties`.

| Variable | Secret | Required | Notes |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | No | Required | Must include `production` or `prod` for production validators. |
| `DB_URL` or `DATABASE_URL` | Yes | Required | PostgreSQL JDBC URL. |
| `DB_USERNAME` or `DATABASE_USERNAME` | Yes | Required | Database user. |
| `DB_PASSWORD` or `DATABASE_PASSWORD` | Yes | Required | Database password. |
| `DB_DRIVER` | No | Optional | Defaults to `org.postgresql.Driver`. |
| `JPA_DDL_AUTO` | No | Required operationally | Use `validate` in production. |
| `SERVER_PORT` | No | Optional | Render usually provides this. |
| `JWT_SECRET` | Yes | Required | Must satisfy JWT validator; use strong Base64 secret. |
| `JWT_EXPIRATION_MINUTES` | No | Optional | Defaults to 30. |
| `JWT_EXPIRATION_MS` | No | Optional | Defaults to 1800000. |
| `APP_ALLOWED_ORIGINS` | No | Required | Production rejects localhost/127.0.0.1 origins. |
| `APP_FRONTEND_URL` | No | Required | Production rejects localhost/127.0.0.1. Controls Stripe redirects and portal return. |
| `STRIPE_SECRET_KEY` | Yes | Required | Production validator requires live secret shape and rejects test-looking values. |
| `STRIPE_WEBHOOK_SECRET` | Yes | Required | Must be the Live webhook endpoint signing secret. Code cannot prove live from prefix; livemode enforcement mitigates signed test events. |
| `STRIPE_PRO_MONTHLY_PRICE_ID` | No | Required | Production validator requires a `price_` value and rejects obvious test values. |
| `STRIPE_PRO_YEARLY_PRICE_ID` | No | Required | Required by current validator even if yearly is not publicly promoted. |
| `STRIPE_PRO_PRICE_ID` | No | Optional legacy fallback | Do not rely on this in production; set `STRIPE_PRO_MONTHLY_PRICE_ID`. |
| `OPENAI_API_KEY` | Yes | Optional for billing | Required for AI features, not Stripe readiness. |
| `RESEND_API_KEY` | Yes | Optional for billing | Required for production email delivery, not Stripe readiness. |
| `EMAIL_FROM` | No | Optional for billing | Required for email delivery. |

No frontend Stripe publishable key is required by the current checkout implementation; the frontend redirects to backend-created Checkout/Portal URLs.

## 7. Live Stripe Dashboard Checklist

Configure only events the application handles:

| Event | Why required | Handler | Customer impact if missing |
|---|---|---|---|
| `checkout.session.completed` | Activates Pro subscriptions and fulfills credit packs. | `StripeBillingService.handleCheckoutCompleted` | Paid users may not receive PRO/credits. |
| `customer.subscription.updated` | Reconciles active/trialing/past_due/cancel-at-period-end state. | `handleSubscriptionUpdated` | Entitlement and cancellation state can drift. |
| `customer.subscription.deleted` | Applies terminal cancellation when Stripe says subscription ended. | `handleSubscriptionDeleted` | Canceled users may retain PRO. |
| `invoice.payment_succeeded` | Confirms paid/recovered subscription and billing period. | `handleInvoicePaymentSucceeded` | Recovery/monthly allowance period may not update. |
| `invoice.payment_failed` | Records grace/PAST_DUE state without premature downgrade. | `handleInvoicePaymentFailed` | Payment failure state may be invisible and stale. |
| `charge.refunded` | Reverses/refunds one-time credit-pack purchases. | `handleChargeRefunded` | Refunded credits may remain usable. |
| `charge.dispute.created` | Reverses disputed credit-pack purchases. | `handleChargeDispute` | Disputed credits may remain usable. |
| `charge.dispute.closed` | Records dispute won/lost outcome. | `handleChargeDispute` | Credits may stay reversed or active incorrectly after dispute close. |

The code also accepts `invoice_payment.paid` and `invoice_payment.payment_failed`; enable only if the Stripe account/API version emits those event types.

Required Live Stripe objects:

- Product: TradePilot PRO subscription product.
- Recurring price: monthly PRO price assigned to `STRIPE_PRO_MONTHLY_PRICE_ID`.
- Recurring price: yearly PRO price assigned to `STRIPE_PRO_YEARLY_PRICE_ID`.
- Credit packs: no pre-created Stripe Price IDs are required by current code because Checkout uses server-created one-time inline `PriceData` for Starter 25, Power 75, and Elite 200.

Required URL behavior:

- Subscription success URL: `${APP_FRONTEND_URL}/dashboard?billing=success`
- Subscription cancel URL: `${APP_FRONTEND_URL}/dashboard?billing=cancelled`
- Credit-pack success URL: `${APP_FRONTEND_URL}/billing?credits=success`
- Credit-pack cancel URL: `${APP_FRONTEND_URL}/billing?credits=cancelled`
- Customer Portal return URL: `${APP_FRONTEND_URL}/dashboard`

Customer Portal:

- Enable payment method management.
- Enable subscription cancellation/update behavior according to business policy.
- Confirm it is tied to the Live account and not a test-mode portal configuration.

## 8. Recommended Deployment Order

The proposed order is close, but Stripe Live products/prices can be created before DB migration because they do not affect the app until env vars/deploy. The safest sequence:

1. Freeze billing-affecting deploys.
2. Run DB preflight queries.
3. Resolve duplicate, malformed, orphaned, and inconsistent data.
4. Apply `backend/supabase_stripe_production_safety.sql`.
5. Apply `backend/supabase_stripe_live_blockers.sql`.
6. Verify schema/indexes.
7. Backfill trustworthy historical credit-pack purchase references.
8. Mark non-backfillable purchases as legacy/manual-reconciliation.
9. Create/verify Stripe Live Products and recurring Prices.
10. Create Live webhook endpoint with the exact required events.
11. Obtain Live webhook signing secret.
12. Configure Render production env vars.
13. Deploy backend.
14. Verify backend startup validators fail closed/pass with correct config.
15. Configure frontend only for backend API URL/origins; no publishable Stripe key is required by current code.
16. Deploy frontend.
17. Run real-money Starter credit-pack smoke test.
18. Run real-money Pro monthly subscription smoke test.
19. Test Customer Portal.
20. Cancel at period end and verify entitlement remains through paid-through date.
21. Verify duplicate subscription checkout is blocked.
22. Reconcile Stripe Dashboard customers/subscriptions/payments against TradePilot DB.

Because this audit found code blockers, do not proceed past staging validation until the blockers are fixed and re-audited.

## 9. Failure and Rollback Review

### A. Migration fails

- What not to do: do not force-create unique indexes or delete rows blindly.
- Immediate containment: stop rollout; keep current backend if schema is incomplete.
- Source of truth: existing database plus Stripe Dashboard for Stripe IDs.
- Recovery: inspect failed statement, resolve duplicates/invalid rows, rerun migration.
- Reconciliation: run schema verification and preflight queries again.

### B. Backend will not start because validator rejects config

- What not to do: do not disable validators in production.
- Immediate containment: keep previous deployment active.
- Source of truth: Render env vars and Stripe Live Dashboard.
- Recovery: correct env vars, especially live secret key, webhook secret, monthly/yearly price IDs, frontend URL, and CORS origins.
- Reconciliation: confirm startup logs show validators passed without printing secret values.

### C. Stripe payment succeeds but webhook fails

- What not to do: do not manually add credits/PRO without recording a ledger/audit entry.
- Immediate containment: leave webhook retries enabled; do not delete the event endpoint.
- Source of truth: Stripe Checkout Session, PaymentIntent, Subscription, and event logs.
- Recovery: fix webhook error, replay event from Stripe Dashboard if needed.
- Reconciliation: verify processed event row, purchase ledger/subscription fields, user balance/plan.

### D. Webhook succeeds but app response fails

- What not to do: do not replay manually without checking `processed_stripe_events`.
- Immediate containment: inspect app logs and DB transaction result.
- Source of truth: TradePilot DB for applied side effects; Stripe for payment state.
- Recovery: if DB committed, replay should be idempotent by event ID.
- Reconciliation: compare event ID, ledger/subscription/user rows.

### E. Credits not granted after successful purchase

- What not to do: do not directly edit user balance alone.
- Immediate containment: pause credit-pack promotion if repeated.
- Source of truth: Stripe Checkout Session and PaymentIntent.
- Recovery: replay `checkout.session.completed` after code/config issue is fixed.
- Reconciliation: exactly one purchase ledger row and exact credit delta.

### F. User charged twice

- What not to do: do not delete one subscription/payment record from DB without Stripe action.
- Immediate containment: block further checkout for the user.
- Source of truth: Stripe charges/subscriptions.
- Recovery: refund duplicate one-time purchase or cancel/refund duplicate subscription per policy.
- Reconciliation: ensure TradePilot stores the intended active subscription and any refund reversal is ledger-correct.

### G. Duplicate Pro subscription exists

- What not to do: do not just overwrite `stripe_subscription_id`.
- Immediate containment: prevent further checkout for the affected user.
- Source of truth: Stripe subscriptions under the customer.
- Recovery: choose canonical subscription, cancel/refund duplicate per support policy.
- Reconciliation: update TradePilot user to canonical subscription and document manual action.

### H. Refund occurs but credits are not reversed

- What not to do: do not manually set balance without preserving audit trail.
- Immediate containment: mark purchase for manual reconciliation.
- Source of truth: Stripe Charge/Refund and `ai_credit_purchases`.
- Recovery: replay event after handler fix or add a reviewed ledger adjustment.
- Reconciliation: check `refunded_amount_cents`, `reversed_credits`, status, user purchased-credit balance.

### I. Stripe API unavailable

- What not to do: do not downgrade based only on uncertain stale subscription payloads.
- Immediate containment: rely on Stripe webhook retry and application 500 for reconciliation failures.
- Source of truth: Stripe Subscription once API recovers.
- Recovery: retry/replay failed events.
- Reconciliation: verify subscription state, period, plan, and processed event rows.

### J. Render deployment rollback required

- What not to do: do not rollback DB migrations unless a reviewed down plan exists.
- Immediate containment: rollback service image/env to last known good version.
- Source of truth: Stripe Dashboard for payments, DB for side effects.
- Recovery: keep webhook endpoint pointed at healthy backend or temporarily disable payment entry points.
- Reconciliation: replay missed webhooks and audit ledgers after service recovery.

## 10. Production Smoke Test Plan

Use real low-value Live payments only after code blockers are fixed, migrations/backfill completed, and env/dashboard gates pass.

### Test 1: Starter credit pack

Verify:

- Real charge succeeds.
- Exactly 25 purchased credits are granted.
- Exactly one `ai_credit_purchases` row exists.
- Checkout Session ID is captured.
- PaymentIntent ID is captured.
- Processed event is stored.
- Stripe event ID is traceable in logs/DB.

### Test 2: Pro monthly subscription

Verify:

- Exactly one active subscription exists for the Stripe customer.
- User becomes PRO.
- Monthly allowance is exactly 100.
- Stripe customer is reused.
- Subscription ID is stored.
- Duplicate checkout/subscription is blocked.

### Test 3: Customer Portal

Verify:

- Portal opens for the authenticated user's Stripe customer only.
- Correct subscription is shown.
- Return URL reaches `${APP_FRONTEND_URL}/dashboard`.

### Test 4: Cancel at period end

Verify:

- User remains PRO until paid-through date.
- `cancel_at_period_end` is reflected.
- No premature downgrade occurs.

Do not perform a destructive dispute test in production. Use Stripe test mode/staging for dispute lifecycle tests.

## 11. Security Search

Command:

```bash
rg -n "sk_test_|sk_live_|rk_live_|pk_test_|pk_live_|whsec_|price_|prod_|localhost|127\.0\.0\.1" .
```

Result summary:

- No committed real live secrets were found.
- Secret-like matches are validators, redacted/dummy tests, docs, and local development examples.
- No backend secret is exposed to frontend code.
- No frontend Stripe publishable key is currently used.
- Stripe Price IDs are configuration-driven.
- Production URL/CORS validators reject localhost/127.0.0.1 in production.
- User cannot submit arbitrary Stripe Price ID.
- User cannot submit arbitrary Stripe Customer ID.
- User cannot directly grant credits or change plan through billing endpoints.

Relevant notes:

- `frontend/src/api/axios.ts` has a localhost API default for development. Production must set `VITE_API_BASE_URL`.
- `backend/src/main/resources/application.yml` defaults `APP_ALLOWED_ORIGINS` and `APP_FRONTEND_URL` to localhost, but production validators reject those under `prod`/`production`.

## 12. Manual Go-Live Gates

### CODE GATES

- [ ] Fix credit-pack same-session concurrent double-grant race.
- [ ] Add concurrency test for two different event IDs referencing one Checkout Session.
- [ ] Split refund and dispute reversal accounting or add reason-aware reversal ledger.
- [ ] Add tests for refund-then-dispute-won and refund-then-dispute-lost.
- [ ] Treat null current subscription retrieval as retryable failure for sensitive destructive transitions.
- [ ] Re-run Stripe/billing tests and full `mvn test`.
- [ ] Run a fresh final audit after fixes.

### DATABASE GATES

- [ ] Run all preflight queries.
- [ ] Resolve duplicate customer/subscription/session/payment/charge IDs.
- [ ] Resolve malformed Stripe IDs.
- [ ] Resolve orphaned purchases.
- [ ] Resolve inconsistent subscription states.
- [ ] Apply `backend/supabase_stripe_production_safety.sql`.
- [ ] Apply `backend/supabase_stripe_live_blockers.sql`.
- [ ] Verify schema/indexes.
- [ ] Backfill only authoritative historical credit-pack references.
- [ ] Mark unreliable legacy purchases for manual reconciliation.

### STRIPE DASHBOARD GATES

- [ ] Create/verify Live PRO Product.
- [ ] Create/verify Live monthly recurring Price.
- [ ] Create/verify Live yearly recurring Price.
- [ ] Create Live webhook endpoint.
- [ ] Enable only handled required events.
- [ ] Copy Live webhook signing secret into Render.
- [ ] Configure Customer Portal for payment method and subscription management.
- [ ] Verify success/cancel/return URLs point to production frontend.

### RENDER GATES

- [ ] Set `SPRING_PROFILES_ACTIVE=production`.
- [ ] Set production DB env vars.
- [ ] Set strong `JWT_SECRET`.
- [ ] Set production `APP_ALLOWED_ORIGINS`.
- [ ] Set production `APP_FRONTEND_URL`.
- [ ] Set Live `STRIPE_SECRET_KEY`.
- [ ] Set Live `STRIPE_WEBHOOK_SECRET`.
- [ ] Set Live `STRIPE_PRO_MONTHLY_PRICE_ID`.
- [ ] Set Live `STRIPE_PRO_YEARLY_PRICE_ID`.
- [ ] Set `JPA_DDL_AUTO=validate`.
- [ ] Confirm backend startup validator passes without logging secrets.

### VERCEL GATES

- [ ] Set `VITE_API_BASE_URL` to production backend API URL.
- [ ] Confirm frontend origin is included in backend `APP_ALLOWED_ORIGINS`.
- [ ] Verify no publishable Stripe key is required by current frontend.

### REAL-PAYMENT GATES

- [ ] Starter credit-pack smoke test passes.
- [ ] Pro monthly subscription smoke test passes.
- [ ] Customer Portal smoke test passes.
- [ ] Cancel-at-period-end smoke test passes.
- [ ] Stripe Dashboard and TradePilot DB reconcile exactly.

### ROLLBACK GATES

- [ ] Render rollback target identified.
- [ ] Webhook replay procedure documented.
- [ ] Manual ledger adjustment procedure documented.
- [ ] Duplicate charge/subscription support policy documented.
- [ ] DB migration recovery plan documented.

## 13. Final Decision

The implementation is materially stronger than the original state and all automated tests pass. However, this final audit found unresolved billing-integrity blockers in credit-pack fulfillment concurrency and refund/dispute reversal accounting. These are code-level blockers, not merely operational launch tasks.

Required fixes before customer payments:

1. Make credit-pack fulfillment business idempotency concurrency-safe so a duplicate same-session race cannot double-grant credits.
2. Make refund/dispute reversals reason-aware so dispute-won handling cannot restore credits reversed by a full refund.
3. Add tests for both failure modes.
4. Re-run focused billing tests, full backend tests, and a follow-up final audit.

STRIPE LIVE READINESS: NO-GO
