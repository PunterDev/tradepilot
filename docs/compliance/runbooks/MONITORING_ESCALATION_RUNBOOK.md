# Monitoring Escalation Runbook

Automated alerting is IMPLEMENTATION REQUIRED where only logs or database metrics exist. This runbook defines desired escalation behavior and uses repository observability evidence where available.

| Signal | Severity | Escalation | Investigation | Resolution | Evidence |
| --- | --- | --- | --- | --- | --- |
| Application 5xx error-rate spike | SEV-2 or SEV-1 if widespread | Production Administrator and Application Developer | Admin observability, request IDs, deployment history, logs | Rollback or fix root cause | Alert, logs, change record |
| Authentication anomalies | SEV-2 | Production Administrator | Auth logs, rate-limit records, user reports | Block abuse source, revoke sessions/secrets if needed | Logs, incident record |
| Rate-limit spikes | SEV-3 or SEV-2 | Application Developer | `RateLimitInterceptor` logs, source pattern, endpoint | Tune controls or add edge/distributed protection | Logs, risk/action ticket |
| Authorization failures | SEV-3 or SEV-2 if suspicious | Application Developer | 401/403/404 patterns, request IDs, user IDs | Investigate possible enumeration or bug | Logs, incident/action record |
| Stripe webhook failures | SEV-2; SEV-1 for billing integrity | Production Administrator and Service Owner | Stripe dashboard, `processed_stripe_events`, Stripe metrics | Replay/fix webhook, reconcile ledger | Stripe events, DB queries |
| Database errors | SEV-2 or SEV-1 | Production Administrator | DB logs, Flyway state, query metrics, deployment changes | Rollback/recover/restore as needed | Logs, migration output |
| External dependency failures | SEV-3 to SEV-1 | Service Owner | Vendor status, app metrics, provider logs | Degrade gracefully, retry, communicate impact | Vendor status, logs |
| Slow requests | SEV-3; SEV-2 if broad | Application Developer | Slow request/query metrics, endpoint P95 | Optimize or roll back | Observability dashboard |
| Critical job failures | SEV-3 or SEV-2 | Service Owner | Async worker logs, feature-specific tables | Retry/recover and verify outputs | Logs, job records |
| Email delivery failures | SEV-3 or SEV-2 for auth impact | Service Owner | Email metrics, Resend dashboard | Fix provider/config, resend | Email metrics, provider logs |
| AI cost threshold exceeded | SEV-3 | TradePilot Management and Application Developer | AI metrics, feature usage, rate limits | Pause feature/tune limits | Metrics, change record |

## Evidence

Retain alert notification, acknowledgement, investigation notes, request IDs, related change/incident records, and final resolution. Do not store secret values in evidence.
