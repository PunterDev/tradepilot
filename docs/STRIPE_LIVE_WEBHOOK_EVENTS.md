# Stripe Live Webhook Events

Configure a Live-mode Stripe webhook endpoint pointing to the production backend:

```text
https://<render-backend-host>/api/stripe/webhook
```

Do not use test-mode webhook signing secrets in production. Production webhook processing rejects signed events where `livemode=false`.

## Required Events

These event names come from the actual `StripeBillingService` handlers.

| Event | Handler | Purpose | Impact if omitted |
| --- | --- | --- | --- |
| `checkout.session.completed` | `handleCheckoutCompleted` | Activates PRO after subscription Checkout and fulfills paid credit-pack purchases. | Paid users may not receive PRO entitlement or purchased credits. |
| `customer.subscription.updated` | `handleSubscriptionUpdated` | Reconciles active, trialing, past-due, cancel-at-period-end, and billing period subscription state. | Entitlements and cancellation metadata can drift from Stripe. |
| `customer.subscription.deleted` | `handleSubscriptionDeleted` | Applies terminal subscription cancellation. | Canceled users may retain PRO. |
| `invoice.payment_succeeded` | `handleInvoicePaymentSucceeded` | Confirms paid/recovered subscription invoices and billing period state. | Recovered subscriptions and paid-through state may not update. |
| `invoice.payment_failed` | `handleInvoicePaymentFailed` | Records payment failure/grace state without immediate downgrade. | Payment failures may not be visible and stale status may persist. |
| `charge.refunded` | `handleChargeRefunded` | Records partial refunds and applies reason-aware full refund credit reversal for one-time credit packs. | Refunded credit packs may remain usable. |
| `charge.dispute.created` | `handleChargeDispute` | Applies dispute-based reversal for credit-pack purchases. | Disputed credit packs may remain usable during disputes. |
| `charge.dispute.closed` | `handleChargeDispute` | Restores dispute-reversed credits for won disputes or preserves reversal for lost disputes. | Dispute outcomes may leave purchased credits wrong. |
| `invoice_payment.paid` | `handleInvoicePaymentSucceeded` | Compatibility handler for invoice payment paid events when emitted by the Stripe account/API version. | If emitted instead of invoice success and omitted, recovery/paid state may not update. |
| `invoice_payment.payment_failed` | `handleInvoicePaymentFailed` | Compatibility handler for invoice payment failure events when emitted by the Stripe account/API version. | If emitted instead of invoice failure and omitted, failure/grace state may not update. |

If `invoice_payment.*` events are not selectable in the Live Dashboard for the account/API version, use the standard `invoice.payment_succeeded` and `invoice.payment_failed` events; the code handles both families.

## Dashboard Setup Checklist

- [ ] Create a Live endpoint for `/api/stripe/webhook`.
- [ ] Select only the handled events above.
- [ ] Copy the Live endpoint signing secret into Render as `STRIPE_WEBHOOK_SECRET`.
- [ ] Confirm the endpoint is Live mode, not test mode.
- [ ] Send no test-mode event to production except as a negative validation after billing is disabled; production should reject `livemode=false`.

## Products, Prices, and URLs

Required Live Stripe objects:

- PRO product.
- Monthly recurring PRO price assigned to `STRIPE_PRO_MONTHLY_PRICE_ID`.
- Yearly recurring PRO price assigned to `STRIPE_PRO_YEARLY_PRICE_ID`.
- One-time credit-pack Checkout prices are created dynamically by the backend from the server-controlled catalog:
  - Starter: 25 credits, USD 999 cents.
  - Power: 75 credits, USD 2499 cents.
  - Elite: 200 credits, USD 5999 cents.

Application-generated redirect URLs:

- Subscription success: `${APP_FRONTEND_URL}/dashboard?billing=success`
- Subscription cancel: `${APP_FRONTEND_URL}/dashboard?billing=cancelled`
- Credit-pack success: `${APP_FRONTEND_URL}/billing?credits=success`
- Credit-pack cancel: `${APP_FRONTEND_URL}/billing?credits=cancelled`
- Customer Portal return: `${APP_FRONTEND_URL}/dashboard`
