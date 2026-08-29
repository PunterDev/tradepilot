# Stripe Production Rollback and Recovery

Use this when production rollout fails or a billing incident occurs. Prefer forward recovery for billing data; do not remove safety constraints casually.

## A. Migration Failure

What not to do:

- Do not drop Stripe IDs or edit billing rows without reconciliation.
- Do not proceed to webhook enablement if uniqueness constraints failed.

Immediate containment:

- Stop the rollout.
- Keep public billing disabled.
- Preserve the failing migration output.

Source of truth:

- Production database current schema and Stripe Dashboard.

Recovery:

- Run the relevant duplicate/preflight query from `docs/STRIPE_PROD_DB_PREFLIGHT.md`.
- Reconcile duplicates against Stripe Dashboard.
- Fix only the disputed historical rows.
- Rerun the failed migration.
- If the negative purchased-credit migration fails, inspect the actual constraint name on `users`; the intended forward state is that no check constraint forbids negative `purchased_ai_credits_balance`.

## B. Backend Startup Failure

What not to do:

- Do not disable production validators to force startup.
- Do not replace live Stripe values with test values.

Immediate containment:

- Roll back to the previous backend deployment if traffic is affected.
- Keep Stripe webhook endpoint disabled or pointed at the last known healthy backend.

Source of truth:

- Render environment variables and backend startup logs.

Recovery:

- Correct the rejected env value.
- Redeploy.
- Confirm validator passes without logging secret values.

## C. Payment Succeeds But Webhook Fails

What not to do:

- Do not manually add credits or PRO without checking whether Stripe will retry.
- Do not mark events processed by hand unless the business effect is already applied.

Immediate containment:

- Leave Stripe webhook retries enabled.
- Pause public purchase links if failures are widespread.

Source of truth:

- Stripe Dashboard payment/subscription state.
- `processed_stripe_events`, `stripe_events`, `users`, `ai_credit_purchases`.

Recovery:

- Fix the webhook error.
- Replay the failed Stripe event from Dashboard if needed.
- Confirm exactly one processed event and exactly one business effect.

## D. Duplicate Charge

What not to do:

- Do not delete one database purchase row to hide the issue.
- Do not issue a refund before identifying which charge should remain.

Immediate containment:

- Pause public checkout if duplicate charge is systemic.
- Open a support case for the affected user.

Source of truth:

- Stripe Charges, Checkout Sessions, PaymentIntents, and customer record.

Recovery:

- Identify the valid intended purchase.
- Refund the unintended duplicate charge through Stripe Dashboard if appropriate.
- Verify `charge.refunded` webhook reverses only the refunded credit pack.
- Reconcile `ai_credit_purchases` and user purchased balance.

## E. Duplicate Subscription

What not to do:

- Do not locally downgrade the user until the Stripe subscription state is clear.
- Do not delete the duplicate subscription ID from the user row without cancelling the extra subscription in Stripe.

Immediate containment:

- Block further checkout for the user.
- Pause public subscription checkout if duplicate creation appears systemic.

Source of truth:

- Stripe active subscriptions for the Stripe customer.

Recovery:

- Decide which subscription remains active.
- Cancel/refund the duplicate subscription in Stripe Dashboard as policy requires.
- Replay or wait for webhooks.
- Ensure the user has one `stripe_subscription_id` and correct entitlement.

## F. Credits Missing

What not to do:

- Do not grant credits before checking if webhook retry is pending.
- Do not create a fake ledger row with fabricated Stripe IDs.

Immediate containment:

- Keep user's support case open.
- Query by Checkout Session and PaymentIntent.

Source of truth:

- Stripe Checkout Session payment status and `ai_credit_purchases`.

Recovery:

- Replay `checkout.session.completed` if payment is paid and ledger row is missing.
- If historical data cannot be replayed, create a documented manual reconciliation entry according to support policy.
- Verify final purchased balance and ledger row.

## G. Credits Double-Granted

What not to do:

- Do not delete processed Stripe events.
- Do not silently reduce total credits without support notification.

Immediate containment:

- Pause credit-pack checkout if systemic.
- Query duplicate Checkout Session, PaymentIntent, and Charge references.

Source of truth:

- `ai_credit_purchases` unique claim, Stripe Checkout Session, and user balance.

Recovery:

- Identify the extra grant.
- Apply a documented manual purchased-credit correction if policy permits.
- Preserve audit trail.

## H. Refund/Dispute Mismatch

What not to do:

- Do not mix monthly PRO allowance with purchased-credit corrections.
- Do not restore credits for a refund-based reversal after a dispute is won.

Immediate containment:

- Pause automated support promises for the affected purchase.
- Inspect reason-aware counters.

Source of truth:

- Stripe refund/dispute object, `ai_credit_purchases`, and event history.

Recovery:

- Verify `refund_reversal_credits`, `dispute_reversal_credits`, `dispute_restored_credits`, and `reversed_credits`.
- Correct only purchased-credit balance if the economic state is wrong.
- Keep a written manual reconciliation note.

## I. Stripe API Outage

What not to do:

- Do not apply destructive subscription downgrades from stale webhook payloads when reconciliation fails.
- Do not disable signature verification.

Immediate containment:

- Let webhook requests fail retryably where reconciliation is required.
- Monitor Stripe status and retry queues.

Source of truth:

- Stripe once API recovers.

Recovery:

- Allow retries or replay events after recovery.
- Run subscription reconciliation checks.

## J. Render Rollback

What not to do:

- Do not roll back to code that does not understand the migrated schema if live webhooks may arrive.
- Do not point Live webhooks to a local or staging endpoint.

Immediate containment:

- Roll back to the latest backend build that includes Stripe production safety migrations and handlers.
- Disable public billing links if no safe backend is available.

Source of truth:

- Render deployment history, Git commit, and database schema.

Recovery:

- Redeploy a safe build.
- Confirm startup validators pass.
- Replay failed webhooks and reconcile.
