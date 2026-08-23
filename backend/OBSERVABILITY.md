# TradePilot Observability and Error Handling

This document defines the launch baseline for tracing production issues without exposing secrets or returning false success to users.

## Request IDs

Every API request receives a request id.

- Incoming `X-Request-ID` is accepted when it is short and contains only safe characters.
- Legacy `X-Request-Id` is also accepted for compatibility.
- If no safe request id is provided, the backend generates a UUID.
- The request id is stored in MDC as `requestId`.
- Responses include both `X-Request-ID` and `X-Request-Id`.
- Error responses include `requestId`.

Use `requestId` first when investigating a user-reported failure.

## Production Monitoring Dashboard

Admins can use **Admin → Production Monitoring** to triage support issues quickly.

- **Error Rate** is based on server errors (`5xx`) over total API requests. Treat `>2%` as warning and `>5%` as critical.
- **Errors by Status** groups common support statuses: `400`, `401`, `403`, `404`, `409`, `429`, `500`, and `502`.
- **Errors by Endpoint** shows the affected endpoint, category, count, latest safe error code, latest timestamp, and latest request id.
- **Combined P95 Response** includes all API traffic. Use categorized latency before drawing conclusions.
- **Standard API P95** excludes AI generation, admin/reporting, webhook, and system traffic. Treat `>1500ms` as warning and `>3000ms` as critical.
- **AI Generation P95** is tracked separately. Treat `>30000ms` as warning.
- **Cache Hit Rate** below `50%` is warning when cache activity exists.
- **Stripe Health** and **Email Success** show `N/A` when no events were attempted in the selected range. A real `0%` appears only when attempts exist and none succeeded.
- **Stripe Failures** or **Email Failures** greater than zero should be investigated before launch changes.

The dashboard only stores safe operational metadata. It must not include secrets, raw prompts, tokens, passwords, card data, reset links, or full payloads.

## Error Response Contract

Backend errors should use this safe shape:

```json
{
  "timestamp": "2026-06-30T02:00:00Z",
  "requestId": "abc-123",
  "status": 500,
  "errorCode": "INTERNAL_ERROR",
  "userMessage": "Something went wrong. Please try again.",
  "error": "Something went wrong. Please try again.",
  "path": "/api/accounts"
}
```

`error` remains as a compatibility alias for existing frontend code. New code should prefer `userMessage`, `errorCode`, and `requestId`.

Never expose stack traces, JWTs, passwords, reset tokens, verification tokens, Stripe secrets, OpenAI prompts, database URLs, or API keys in response bodies.

## Logging Standards

Important logs should be structured and include useful identifiers:

- `requestId` from MDC, automatically included by log pattern.
- `userId` when authenticated or known.
- `endpoint` or operation name.
- `accountId`, `tradeId`, or date range when relevant.
- `feature` and credit cost for AI work.
- Stripe `eventId`, `sessionId`, `customerId`, `subscriptionId`, and `paymentIntentId` when available.
- `status`, `durationMs`, and safe error summaries.

Avoid logging full request payloads. Never log secrets, tokens, raw OpenAI prompts, or card/payment data.

## Stripe Tracing

For subscription checkout:

1. Search logs for `Stripe checkout session creation started` with `userId`.
2. Confirm `Stripe checkout session creation completed`.
3. Trace webhook logs by `eventId` and `type`.
4. Confirm user subscription logs such as `Stripe subscription updated`, `Stripe checkout completed upgraded user`, or invoice payment confirmation.
5. Check `processed_stripe_events` for the Stripe event id.

For AI credit packs:

1. Search logs for `Stripe AI credit pack checkout creation started` with `userId`, `packId`, and `credits`.
2. Trace `checkout.session.completed` webhook by `eventId`.
3. Confirm `Stripe AI credit pack applied` includes `userId`, `sessionId`, `packId`, `credits`, and `newBalance`.
4. Check `ai_credit_purchases` for `stripe_checkout_session_id`.
5. Check `users.purchased_ai_credits_balance`.
6. If a webhook was replayed, confirm duplicate handling logs and `processed_stripe_events` prevent double application.

If a user says "payment succeeded but credits did not appear", collect:

- user email/userId,
- Stripe checkout session id,
- Stripe payment intent id,
- requestId from frontend/backend error,
- matching `ai_credit_purchases` row,
- latest billing usage response.

Dashboard path:

1. Open Production Monitoring for the payment date.
2. Check **Stripe Health** for successful events, failed events, last event type/time, and credit-pack purchases completed today.
3. If failures exist, open **Last Stripe Failure** and use the safe error message plus logs around the same time.
4. Confirm `ai_credit_purchases.stripe_checkout_session_id` and `users.purchased_ai_credits_balance`.

## AI Tracing

AI features should log:

- feature name,
- `userId`,
- `accountId` / `tradeId` / date range,
- cache hit or miss,
- no-data skip reason,
- credit cost and deduction split,
- OpenAI start/end duration and token usage when available,
- snapshot/review id on save,
- failure status and safe error message.

Cached reads must not consume credits. No-data deterministic responses should not call OpenAI or consume credits.

If a user says "AI used credits but result is missing":

1. Search logs by `requestId` and `userId`.
2. Check `ai_credit_usage` for feature, credits, monthly/purchased split, request id/resource id, and created time.
3. Check the feature snapshot/review table for `PENDING`, `COMPLETED`, `FAILED`, or deterministic no-data status.
4. Confirm async worker logs show completion or failure.
5. If OpenAI succeeded but DB save failed, the API must not return success; investigate the exception under the same `requestId`.

Dashboard path:

1. Check **AI Costs** for calls, failures, tokens, average duration, most expensive feature, and most used model.
2. Check **Last AI Failure** for feature, user id, and safe error code.
3. Check **AI Generation P95** separately from Standard API latency.
4. Check cache health. Cached reads should be visible as cache hits and should not create new AI usage rows.

## CSV Import Tracing

CSV import is a database write path and should not show success until the backend returns saved trades.

Trace by:

- `requestId`,
- `userId`,
- `accountId`,
- import batch row,
- saved trade count,
- skipped duplicate count,
- validation error response.

Invalid CSV payloads should return `400` with `requestId`; duplicate/conflict write failures should return `409` or a clear validation error.

## False Success Prevention

Backend:

- Do not return `200` after a failed database write.
- Use transactions when multiple writes must succeed together.
- Do not swallow exceptions in write paths unless the operation is explicitly best-effort observability/email.
- Mark async AI jobs or snapshots `FAILED` when generation fails.
- Preserve idempotency for Stripe webhooks and AI cache races.

Frontend:

- Show success only after the awaited API call returns success and expected fields exist.
- On mutation failure, show a user-facing error.
- Include `requestId` when the backend provides one.
- Avoid optimistic updates unless there is rollback.
- Do not rely on `console.error` as the only failure signal.

## Existing Audit Tables

TradePilot currently uses these operational records:

- `processed_stripe_events` for webhook idempotency.
- `ai_credit_purchases` for one-time credit pack purchases.
- `ai_credit_usage` for pooled monthly/purchased credit deductions.
- AI review/snapshot tables with status fields for generated content.
- observability event tables for API, cache, email, Stripe, query performance, and alerts.

## Post-Beta Option

A general `operation_audit_events` table can be added later if log-based tracing is not enough. It should store only safe metadata:

- request id,
- operation type,
- user/account/trade ids,
- external id,
- status,
- error code,
- safe message.

Do not store secrets, full prompts, card data, or sensitive payloads.
