# Stripe Final Delta Audit

Date: 2026-08-29

Scope: latest Stripe Live blocker fixes plus production go-live prerequisites. No Stripe Live calls were made and no live keys were used.

## Executive Decision

The two recent code blockers are closed by code review and automated tests. The implementation now uses Checkout Session ID as the business-level credit-pack fulfillment claim before granting purchased credits, and refund/dispute accounting is reason-aware.

This is not a public-payments enablement approval yet. Production database migrations, live Stripe Dashboard configuration, Render/Vercel environment configuration, and real-money smoke tests remain required before public Stripe payments are enabled.

## Test Results

Command:

```bash
cd backend
mvn -Dtest=StripeBillingTests,StripeWebhookLiveModeTests,com.tradepilot.billing.StripeProductionConfigValidatorTests test
```

Result:

- Tests run: 92
- Failures: 0
- Errors: 0
- Skipped: 0

Command:

```bash
cd backend
mvn test
```

Result:

- Tests run: 413
- Failures: 0
- Errors: 0
- Skipped: 0

Command:

```bash
git diff --check
```

Result: clean.

## Blocker 1: Concurrent Same-Checkout Credit Double Grant

Status: VERIFIED CLOSED

Relevant code:

- `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- `backend/src/main/java/com/tradepilot/billing/AiCreditPurchase.java`
- `backend/src/main/java/com/tradepilot/billing/AiCreditPurchaseRepository.java`
- `backend/src/test/java/com/tradepilot/StripeBillingTests.java`
- `backend/supabase_stripe_live_blockers.sql`

Proof:

- `handleWebhook` wraps event persistence, business handling, and credit-pack fulfillment in one `TransactionTemplate` transaction.
- `handleCreditPackCheckoutCompleted` validates the Checkout Session, locks the mapped user, checks for an existing `stripe_checkout_session_id`, then calls `aiCreditPurchaseRepository.saveAndFlush(purchase)` before incrementing `purchasedAiCreditsBalance`.
- `stripe_checkout_session_id` is non-null in JPA and has a unique constraint/index. The Checkout Session ID is therefore the authoritative fulfillment identity.
- If the unique claim already exists, the handler returns successfully and grants zero credits.
- If a duplicate-key race occurs during `saveAndFlush`, the handler rechecks by Checkout Session ID and treats the purchase as already fulfilled instead of surfacing a webhook 500.
- Credit increment happens only after the ledger claim flush succeeds.
- PaymentIntent uniqueness remains enforced by the partial unique index from `supabase_stripe_live_blockers.sql` where `stripe_payment_intent_id` is non-null/non-empty.

Rollback/atomicity proof:

- Failure after insert/flush but before credit increment rolls back the transaction. The claim row is not committed, the processed event is not committed, and Stripe retry can safely grant once.
- Failure after credit increment but before commit rolls back both user balance and purchase row. Stripe retry can safely grant once.
- The user pessimistic lock serializes same-user local delivery, but it is not the primary guarantee. Database uniqueness on Checkout Session ID remains authoritative across multiple JVMs and Render instances.
- No `REQUIRES_NEW` or independent transaction is used in the credit-pack path; the ledger claim and balance mutation share the webhook transaction.

Tests reviewed:

- Different Stripe event IDs for the same Checkout Session grant once.
- Concurrent different Stripe event IDs for the same Checkout Session grant once.
- Repeated same Stripe event ID grants once.
- Preexisting ledger claim grants zero additional credits.
- Failure after fulfillment claim rolls back and retries safely.
- Failure after credit increment rolls back and retries safely.
- Starter, Power, and Elite credit packs use the same fulfillment path.

Remaining concern: none identified in code. PostgreSQL production behavior depends on applying the unique indexes/migrations before enabling live webhooks.

## Blocker 2: Reversal Reason Accounting

Status: VERIFIED CLOSED

Relevant code:

- `backend/src/main/java/com/tradepilot/billing/StripeBillingService.java`
- `backend/src/main/java/com/tradepilot/billing/AiCreditPurchase.java`
- `backend/src/test/java/com/tradepilot/StripeBillingTests.java`
- `backend/supabase_stripe_reversal_reason_accounting.sql`

Implementation:

- Each credit-pack purchase now stores `refundReversalCredits`, `disputeReversalCredits`, and `disputeRestoredCredits`.
- Economic reversal is calculated as:

```text
max(refundReversalCredits, disputeReversalCredits - disputeRestoredCredits)
```

- Full refunds set refund reversal credits and do not depend on dispute state.
- Dispute created/lost sets dispute reversal credits.
- Dispute won restores only credits previously reversed for the dispute.
- Won disputes do not restore refund-based reversals.
- Monthly PRO allowance is untouched; handlers mutate only `purchasedAiCreditsBalance`.

Scenario review:

| Scenario | Expected net purchased credit effect | Verified behavior |
| --- | ---: | --- |
| Purchase +25, full refund, dispute won | 0 | Refund reversal remains authoritative; no restoration occurs without prior dispute reversal. |
| Purchase +25, dispute created, dispute won | +25 | Dispute reversal is restored exactly once. |
| Purchase +25, dispute created, dispute lost | 0 | Dispute reversal remains active. |
| Purchase +25, dispute created, dispute won, full refund | 0 | Refund reversal applies after dispute restoration. |
| Purchase +25, full refund, dispute created, dispute won | 0 | Refund reversal remains authoritative across later dispute events. |
| Duplicate refund events | 0 | One economic refund reversal only. |
| Duplicate dispute events | Deterministic | One dispute reversal/restoration only. |
| Refund/dispute concurrently | Deterministic | Row locks plus max-based economic reversal prevent double subtraction. |
| Purchased credits already spent | May go negative | Purchased balance may become negative; monthly PRO credits are not modified. |

Safety proof:

- Refund/dispute lookups use pessimistic purchase-row locking by PaymentIntent or Charge.
- Reversal delta is computed from previous economic reversal to new economic reversal, so replayed events have zero delta.
- `disputeRestoredCredits` is clamped not to exceed `disputeReversalCredits`.
- The migration adds a PostgreSQL check constraint enforcing `dispute_restored_credits <= dispute_reversal_credits`.
- Because refund and active dispute reversal are combined with `max`, refund plus dispute events cannot double-reverse the same purchase.

Remaining concern: historical rows with nonzero `reversed_credits` need review before automated live refund/dispute handling is trusted for legacy purchases.

## New Migration Review

Migration: `backend/supabase_stripe_reversal_reason_accounting.sql`

Status: ACCEPTABLE WITH PREFLIGHT

Findings:

- Column types are PostgreSQL `integer`, matching JPA `Integer`.
- Defaults of `0` are safe for new rows and existing rows.
- `NOT NULL` is appropriate after the migration backfills null values.
- Check constraints are valid PostgreSQL.
- `dispute_restored_credits <= dispute_reversal_credits` is correct because restoration must never exceed prior dispute reversal.
- The migration is idempotent for column creation and constraints.
- It must run after `backend/supabase_stripe_live_blockers.sql`, because it depends on `ai_credit_purchases.reversed_credits`, refund/dispute status fields, and the live-blocker purchase schema.
- If it partially fails after columns are added but before constraints, rerunning is safe after correcting any data that violates the checks.
- Historical rows can migrate safely for net accounting, but rows with legacy `reversed_credits > 0` require review because the migration infers reversal reason from status.

Production preflight queries before applying this migration:

```sql
-- 1. Confirm the migration is being applied after the live-blockers schema.
select column_name, data_type, is_nullable
from information_schema.columns
where table_schema = 'public'
  and table_name = 'ai_credit_purchases'
  and column_name in (
    'id',
    'user_id',
    'stripe_checkout_session_id',
    'stripe_payment_intent_id',
    'stripe_charge_id',
    'credits_purchased',
    'amount_cents',
    'refunded_amount_cents',
    'reversed_credits',
    'status',
    'dispute_status'
  )
order by column_name;

-- 2. Review every legacy row whose reversal reason will be inferred.
select
  id,
  user_id,
  stripe_checkout_session_id,
  stripe_payment_intent_id,
  stripe_charge_id,
  pack_id,
  credits_purchased,
  amount_cents,
  currency,
  status,
  dispute_status,
  refunded_amount_cents,
  reversed_credits,
  refunded_at,
  disputed_at,
  reversed_at
from ai_credit_purchases
where coalesce(reversed_credits, 0) > 0
order by id;

-- 3. Rows that would violate non-negative accounting expectations.
select
  id,
  user_id,
  stripe_checkout_session_id,
  status,
  credits_purchased,
  refunded_amount_cents,
  reversed_credits
from ai_credit_purchases
where coalesce(reversed_credits, 0) < 0
   or coalesce(refunded_amount_cents, 0) < 0
   or coalesce(credits_purchased, 0) < 0;

-- 4. Status values that the migration's conservative split may not classify correctly.
select status, dispute_status, count(*) as row_count, sum(coalesce(reversed_credits, 0)) as reversed_sum
from ai_credit_purchases
group by status, dispute_status
order by status, dispute_status;

-- 5. Full-refund-looking rows where reversal does not match purchased credits.
select
  id,
  user_id,
  stripe_checkout_session_id,
  credits_purchased,
  amount_cents,
  refunded_amount_cents,
  reversed_credits,
  status
from ai_credit_purchases
where status = 'REFUNDED'
  and coalesce(reversed_credits, 0) <> coalesce(credits_purchased, 0);

-- 6. Active/lost dispute rows where reversal does not match purchased credits.
select
  id,
  user_id,
  stripe_checkout_session_id,
  credits_purchased,
  reversed_credits,
  status,
  dispute_status
from ai_credit_purchases
where status in ('DISPUTED', 'DISPUTE_LOST')
  and coalesce(reversed_credits, 0) <> coalesce(credits_purchased, 0);

-- 7. Won-dispute rows that still show active reversed credits.
select
  id,
  user_id,
  stripe_checkout_session_id,
  credits_purchased,
  reversed_credits,
  status,
  dispute_status
from ai_credit_purchases
where status = 'DISPUTE_WON'
  and coalesce(reversed_credits, 0) <> 0;

-- 8. If this migration was partially applied before, validate restored <= reversal dynamically.
do $$
declare
  violation_count bigint := 0;
begin
  if exists (
      select 1 from information_schema.columns
      where table_schema = 'public'
        and table_name = 'ai_credit_purchases'
        and column_name = 'dispute_restored_credits'
  ) and exists (
      select 1 from information_schema.columns
      where table_schema = 'public'
        and table_name = 'ai_credit_purchases'
        and column_name = 'dispute_reversal_credits'
  ) then
    execute 'select count(*) from ai_credit_purchases where coalesce(dispute_restored_credits, 0) > coalesce(dispute_reversal_credits, 0)'
      into violation_count;
    raise notice 'rows where dispute_restored_credits exceeds dispute_reversal_credits: %', violation_count;
  else
    raise notice 'reason-accounting columns do not exist yet; restored<=reversal validation will be enforced by the migration.';
  end if;
end $$;
```

## Regression Check

| Area | Delta result |
| --- | --- |
| Starter +25 | Verified by credit-pack catalog and tests. |
| Power +75 | Verified by credit-pack catalog and tests. |
| Elite +200 | Verified by credit-pack catalog and tests. |
| PRO monthly 100 credits | Unchanged; `UsageLimitService` still defines PRO monthly allowance as 100. |
| Purchased credit preservation | Verified; reversal handlers modify purchased credits only. |
| Payment failure grace behavior | Unchanged; covered by billing tests. |
| Subscription reconciliation | Unchanged; sensitive events still retrieve current Stripe subscription when applicable. |
| Stale webhook suppression | Unchanged; subscription state table tracks last event timestamp. Equal-second safety comes from source-of-truth reconciliation for sensitive changes. |
| Livemode enforcement | Unchanged; production rejects `livemode=false` before processing state. |
| Duplicate active subscription prevention | Unchanged; checkout attempt table and local/Stripe checks block duplicate Pro checkouts. |
| Refund handling | Improved with reason-aware reversal. |
| Dispute handling | Improved with reason-aware reversal/restoration. |
| Customer Portal ownership | Unchanged; portal session uses authenticated user's stored Stripe customer. |
| Event idempotency | Unchanged and still separate from business-level purchase idempotency. |

## Production Operational Gates

No remaining code blockers were identified in this delta audit. The remaining work is operational and data-migration gated.

### Database

- [ ] Run all duplicate and historical-data preflight queries from `docs/STRIPE_FINAL_LIVE_AUDIT.md`.
- [ ] Run the reason-accounting preflight queries in this document.
- [ ] Resolve duplicate `stripe_customer_id`, `stripe_subscription_id`, `checkout_session_id`, `payment_intent_id`, and `charge_id` rows before constraints.
- [ ] Backfill only verifiable historical credit-pack purchase fields.
- [ ] Mark unverifiable historical credit-pack rows as legacy/manual-reconciliation records.
- [ ] Apply migrations in order: `backend/supabase_stripe_production_safety.sql`, `backend/supabase_stripe_live_blockers.sql`, `backend/supabase_stripe_reversal_reason_accounting.sql`.
- [ ] Verify unique indexes and check constraints after migration.

### Stripe Dashboard

- [ ] Create/verify Live PRO product.
- [ ] Create/verify Live monthly recurring PRO price and map it to `STRIPE_PRO_MONTHLY_PRICE_ID`.
- [ ] Create/verify Live yearly recurring PRO price and map it to `STRIPE_PRO_YEARLY_PRICE_ID`.
- [ ] Confirm one-time credit packs use the server-controlled in-app catalog amounts: Starter 25, Power 75, Elite 200.
- [ ] Create the Live webhook endpoint pointing to the production backend `/api/stripe/webhook`.
- [ ] Enable exactly the webhook events listed below.
- [ ] Copy the Live endpoint signing secret into `STRIPE_WEBHOOK_SECRET`.
- [ ] Configure Customer Portal with allowed subscription management behavior.
- [ ] Verify success/cancel/return URLs use the deployed frontend.

### Render

- [ ] Set `SPRING_PROFILES_ACTIVE=production` or `prod`.
- [ ] Set production database credentials.
- [ ] Set `APP_FRONTEND_URL` to the deployed frontend origin.
- [ ] Set `APP_ALLOWED_ORIGINS` to deployed frontend origins only.
- [ ] Set `JWT_SECRET` to a strong production secret.
- [ ] Set Live `STRIPE_SECRET_KEY`.
- [ ] Set Live `STRIPE_WEBHOOK_SECRET`.
- [ ] Set Live `STRIPE_PRO_MONTHLY_PRICE_ID`.
- [ ] Set Live `STRIPE_PRO_YEARLY_PRICE_ID`.
- [ ] Confirm startup fails for test-looking Stripe config before allowing traffic.

### Vercel

- [ ] Configure frontend API base URL to the deployed Render backend.
- [ ] Confirm no production frontend route points to localhost or 127.0.0.1.
- [ ] Deploy frontend after backend health and CORS are verified.

### Real-Payment Smoke Test

- [ ] Buy Starter credit pack with real low-value payment and verify exactly 25 purchased credits.
- [ ] Verify exactly one purchase ledger row with Checkout Session ID and PaymentIntent ID.
- [ ] Subscribe to PRO monthly and verify exactly one active subscription.
- [ ] Verify PRO monthly allowance is exactly 100 and purchased credits are preserved.
- [ ] Open Customer Portal and verify it maps to the correct Stripe customer/subscription.
- [ ] Cancel at period end and verify no premature downgrade.
- [ ] Reconcile Stripe Dashboard objects to TradePilot database rows.

### Rollback

- [ ] Prepare Render rollback to the last known backend image.
- [ ] Keep Stripe webhook retries enabled; do not manually grant credits until reconciliation is complete.
- [ ] Prepare SQL reconciliation queries for purchases, processed events, users, and subscription states.
- [ ] Document manual user remediation for double charge, missing credit grant, duplicate subscription, and refund/dispute mismatches.

## Required Live Webhook Events

These are the event types currently handled by `StripeBillingService`.

| Event | Purpose | What breaks if missing |
| --- | --- | --- |
| `checkout.session.completed` | Activates PRO subscriptions and fulfills paid credit-pack purchases. | Paid users may not receive PRO entitlement or purchased credits. |
| `customer.subscription.updated` | Reconciles active/trialing/past_due/cancel-at-period-end subscription state. | Subscription entitlement and cancellation metadata can drift. |
| `customer.subscription.deleted` | Applies terminal cancellation. | Canceled users may retain PRO. |
| `invoice.payment_succeeded` | Handles successful/recovered subscription invoices and billing period state. | Recovery and paid-through state can lag or remain stale. |
| `invoice.payment_failed` | Records payment failure/grace state without immediate downgrade. | Payment failure state may be invisible and reconciliation may lag. |
| `charge.refunded` | Applies reason-aware full refund reversal for credit-pack purchases and records partial refunds for manual reconciliation. | Refunded purchased credits may remain usable. |
| `charge.dispute.created` | Applies dispute-based reversal for credit-pack purchases. | Disputed purchased credits may remain usable. |
| `charge.dispute.closed` | Restores dispute-reversed credits on won disputes or keeps reversal on lost disputes. | Dispute outcomes may leave purchased credits wrong. |
| `invoice_payment.paid` | Compatibility handler for invoice payment paid events. | If the Stripe account/API version emits this instead of invoice success, paid/recovery state may not update. |
| `invoice_payment.payment_failed` | Compatibility handler for invoice payment failure events. | If the Stripe account/API version emits this instead of invoice failure, failure/grace state may not update. |

If `invoice_payment.*` events are not selectable for the account/API version, use `invoice.payment_succeeded` and `invoice.payment_failed`; the code handles both families.

## Required Production Environment Variables

Exact variable names from `backend/src/main/resources/application.yml` and validators:

| Variable | Required | Secret | Notes |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Required | No | Set to `production` or `prod` to enable production validators. |
| `DB_URL` or `DATABASE_URL` | Required | No/Secret-adjacent | JDBC database URL. Treat as sensitive operational config. |
| `DB_USERNAME` or `DATABASE_USERNAME` | Required | Secret | Database username. |
| `DB_PASSWORD` or `DATABASE_PASSWORD` | Required | Secret | Database password. |
| `DB_DRIVER` | Optional | No | Defaults to `org.postgresql.Driver`. |
| `JPA_DDL_AUTO` | Optional | No | Defaults to `validate`; keep `validate` in production. |
| `SERVER_PORT` | Optional | No | Render may set this. |
| `APP_ALLOWED_ORIGINS` | Required | No | Must not include localhost/127.0.0.1 in production. |
| `APP_FRONTEND_URL` | Required | No | Must be deployed frontend URL; production rejects localhost/127.0.0.1. |
| `FRONTEND_URL` | Optional fallback | No | Do not rely on this in production; set `APP_FRONTEND_URL`. |
| `JWT_SECRET` | Required | Secret | Required by app JWT config. |
| `JWT_EXPIRATION_MINUTES` | Optional | No | Defaults to 30. |
| `JWT_EXPIRATION_MS` | Optional | No | Defaults to 1800000. |
| `STRIPE_SECRET_KEY` | Required | Secret | Production validator requires `sk_live_` shape and rejects test-looking values. |
| `STRIPE_WEBHOOK_SECRET` | Required | Secret | Production validator requires `whsec_` shape and rejects obvious test values. |
| `STRIPE_PRO_MONTHLY_PRICE_ID` | Required | No | Production validator requires `price_` and rejects obvious test values. |
| `STRIPE_PRO_PRICE_ID` | Optional legacy fallback | No | Do not rely on this in production. |
| `STRIPE_PRO_YEARLY_PRICE_ID` | Required | No | Production validator requires `price_` and rejects obvious test values. |
| `OPENAI_API_KEY` | Optional for billing | Secret | Needed only for AI features. |
| `RESEND_API_KEY` | Optional for billing | Secret | Needed for production email delivery. |
| `EMAIL_FROM` | Optional for billing | No | Needed for email delivery. |
| `DEV_AUTH_ENABLED` | Optional | No | Must remain false in production. |
| `DEV_TOOLS_ENABLED` | Optional | No | Must remain false in production. |

No test-mode Stripe fallback can silently work in production if `SPRING_PROFILES_ACTIVE` is set to `production`/`prod`: the validator rejects test-looking secret keys, missing webhook secret, missing price IDs, and invalid credit-pack catalog; webhook processing also rejects signed `livemode=false` events.

## Conditions Before Public Payments

- Apply and verify all Stripe production database migrations in order.
- Resolve duplicate/invalid historical Stripe identifiers before unique indexes.
- Review/backfill/mark legacy credit-pack purchases before trusting automated refund/dispute handling for historical rows.
- Configure Live Stripe products, recurring prices, webhook endpoint, enabled events, and Customer Portal.
- Configure Render production variables with live Stripe values and production URLs.
- Configure Vercel frontend API URL and verify CORS.
- Complete real-money Starter credit-pack, PRO monthly, Customer Portal, and cancel-at-period-end smoke tests.
- Reconcile Stripe Dashboard objects to TradePilot database state after smoke tests.
- Keep rollback and manual reconciliation runbooks ready before opening payments to public users.

STRIPE LIVE READINESS: CONDITIONAL GO
