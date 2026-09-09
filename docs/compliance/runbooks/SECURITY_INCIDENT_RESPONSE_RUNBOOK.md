# Security Incident Response Runbook

## Severity Levels

| Severity | Definition | Examples |
| --- | --- | --- |
| SEV-1 Critical | Active compromise, confirmed sensitive data exposure, major billing integrity failure, or widespread production outage. | Production DB compromise, leaked live secret with evidence of use, Stripe double-charge systemic issue. |
| SEV-2 High | Serious security event with limited confirmed impact or high likelihood of escalation. | Admin account compromise suspected, repeated webhook failures, vendor security incident affecting TradePilot data. |
| SEV-3 Medium | Security weakness or incident with limited impact and available workaround. | Failed access review item, isolated suspicious login pattern, dependency vulnerability without known exploit. |
| SEV-4 Low | Low-risk event, near miss, or policy exception. | Missing evidence artifact, non-sensitive misconfiguration in staging. |

## Response Flow

1. Detection: identify signal from logs, monitoring, support report, vendor notice, GitHub alert, Stripe alert, or manual discovery.
2. Triage: assign severity, Incident Lead, affected systems, and immediate next action.
3. Containment: stop active harm, revoke credentials, disable access, pause billing flows, or roll back deploys when appropriate.
4. Investigation: preserve logs, request IDs, event IDs, database records, provider audit trails, and timeline.
5. Eradication: remove root cause such as exposed credentials, vulnerable dependency, misconfiguration, or compromised account.
6. Recovery: restore safe service, verify application behavior, reconcile data/billing, and monitor for recurrence.
7. Customer impact analysis: determine affected users, data categories, billing impact, and duration.
8. Notification decision: TradePilot Management decides whether customer, vendor, legal, or regulatory notification is required.
9. Post-incident review: document root cause, lessons learned, corrective actions, owners, and due dates.
10. Evidence retention: store incident record and related redacted evidence.

## Evidence

Use `docs/compliance/evidence/templates/INCIDENT_RECORD_TEMPLATE.md`. Do not include secrets, full tokens, passwords, raw card data, or unredacted customer data unless stored in an approved restricted evidence location.

## Current Status

Runbook drafted. No incident or tabletop evidence is created by this document.
