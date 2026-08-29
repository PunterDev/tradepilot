# Stripe Production Rollout Runbook

This runbook prepares Stripe production billing without directly interacting with Stripe Live or production databases from development. Execute the production steps from approved operator terminals and dashboards only.

## Preconditions

- [ ] Latest backend commit deployed to staging/test has passing tests.
- [ ] `docs/STRIPE_PROD_DB_PREFLIGHT.md` has been reviewed.
- [ ] Live Stripe product/price IDs are known but not committed.
- [ ] Render/Vercel environment access is limited to authorized operators.
- [ ] Rollback operator and incident owner are assigned.

## Exact Deployment Order

1. Database preflight
   - Run every read-only query in `docs/STRIPE_PROD_DB_PREFLIGHT.md`.
   - Resolve duplicate Stripe customer, subscription, Checkout Session, PaymentIntent, and Charge IDs.
   - Mark unverifiable historical credit-pack purchases as legacy/manual-reconciliation records.

2. Migrations
   - Apply `backend/supabase_stripe_production_safety.sql`.
   - Apply `backend/supabase_stripe_live_blockers.sql`.
   - Apply `backend/supabase_stripe_reversal_reason_accounting.sql`.
   - Apply `backend/supabase_stripe_allow_negative_purchased_credit_balance.sql`.
   - Stop if any migration fails; use `docs/STRIPE_PROD_ROLLBACK.md`.

3. Schema verification
   - Run post-migration verification queries from `docs/STRIPE_PROD_DB_PREFLIGHT.md`.
   - Confirm all unique indexes and check constraints exist.
   - Confirm the obsolete `users_purchased_ai_credits_balance_nonnegative` constraint is absent.
   - Confirm no refund/dispute accounting violations.

4. Stripe Live products/prices
   - Create or verify PRO Live product.
   - Create or verify Live monthly recurring PRO price.
   - Create or verify Live yearly recurring PRO price.
   - Do not change application pricing during rollout.

5. Webhook setup
   - Create Live webhook endpoint for `/api/stripe/webhook`.
   - Enable only events in `docs/STRIPE_LIVE_WEBHOOK_EVENTS.md`.
   - Copy the Live endpoint signing secret to Render as `STRIPE_WEBHOOK_SECRET`.

6. Render environment configuration
   - Configure every required backend variable in `docs/STRIPE_PROD_ENV_MANIFEST.md`.
   - Ensure `SPRING_PROFILES_ACTIVE=production` or `prod`.
   - Confirm no localhost or 127.0.0.1 values are present in production URL/CORS settings.

7. Backend deploy
   - Deploy backend.
   - Watch startup logs.
   - Production validator must pass with no secret values logged.

8. Startup validation
   - Confirm health endpoint responds.
   - Confirm admin billing summary reports Stripe Checkout, webhook, monthly price, and yearly price as configured.
   - Confirm no Stripe production validator errors.

9. Vercel config/deploy
   - Set `VITE_API_BASE_URL` to the deployed backend API base.
   - Deploy frontend.
   - Confirm frontend origin is listed in backend `APP_ALLOWED_ORIGINS`.

10. Controlled smoke test
    - Execute `docs/STRIPE_PROD_SMOKE_TEST.md` with a real low-value payment path.
    - Keep public billing entry points limited until reconciliation passes.

11. Reconciliation
    - Run `docs/STRIPE_PROD_RECONCILIATION.md` checks.
    - Compare Stripe Dashboard objects with TradePilot database records.
    - Verify processed events, ledger rows, and user entitlements.

12. Public billing enablement
    - Enable public upgrade and credit-pack paths only after all smoke tests pass.
    - Keep monitoring open for at least one billing cycle edge case window.

## Historical Data Risks

- Legacy credit-pack purchases may not have enough Stripe IDs for automated refund/dispute handling.
- Duplicate user Stripe IDs must be reconciled before unique indexes are applied.
- Production rollout should not depend on reconstructing missing Checkout Session or PaymentIntent IDs from memory or logs.

## Observability and Support Readiness

Support can answer at a high level:

- Current user plan and subscription status from admin user views.
- Whether Stripe Checkout, webhook secret, monthly price, and yearly price are configured from admin billing summary.
- Recent Stripe webhook success/failure health from observability metrics.
- Recent manual admin PRO/FREE changes from admin audit logs.

Support can answer with SQL:

- Which Stripe event was processed for a webhook ID.
- Which Checkout Session, PaymentIntent, or Charge created a credit-pack purchase.
- Whether a purchase was refunded or disputed.
- Why purchased-credit balance changed, by comparing `ai_credit_purchases` and processed Stripe events.

Non-blocking support tooling gaps:

- No dedicated per-user billing ledger explorer in the admin UI.
- No UI that directly links user plan transitions to Stripe event IDs.
- No UI that explains a user's purchased-credit balance from purchase, refund, dispute, and usage components.
- No admin UI for refund/dispute/manual-reconciliation status on a credit-pack purchase.
