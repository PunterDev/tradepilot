# Account Survival Coach V1

Account Survival Coach records observable trading-risk patterns, lets a trader accept a personal protection commitment, and evaluates later imported sessions against that immutable commitment. It does not infer motivation, diagnose emotion, or make a profitability claim.

## Commitment lifecycle

1. A commitment begins as `DRAFT` and may be edited by its owner.
2. Acceptance records `acceptedAt` once and changes the record to `ACCEPTED`.
3. Accepted values are immutable through the API. A changed rule requires a separate commitment for a future session.
4. Evaluations never overwrite the accepted commitment. Each eligible session has an auditable current evaluation; corrected imports create a new version for that session and retain the prior version.
5. Draft saves and acceptance serialize on the same commitment row. A save that reaches the row after acceptance is rejected rather than returning it to `DRAFT`.

An eligible evaluation session must be strictly later than both the effective session date and the calendar date on which the commitment was accepted. This prevents a newly accepted or backdated record from being represented as compliance with already completed trading.

## Observable evidence

The feature evaluates completed broker-imported round trips only. It can measure attempt count, normalized exposure, an allowed entry window, a post-loss pause, and maximum realized session loss. The post-loss pause is an every-loss rule: each realized losing attempt is checked against the next completed-attempt entry. It is not a "two consecutive losses" rule. Fresh setup confirmation remains `NOT_EVALUABLE` without contemporaneous setup evidence.

Cross-account continuation is an observation across the authenticated trader's own accounts. It never changes a commitment globally, does not create a unified cross-account compliance score, and is not presented as proof of intent or a prop-firm rule violation.

## Realized session-loss convention

Maximum session loss is evaluated from cumulative **realized** P&L in chronological completed-trade order, not the final session P&L. The worst cumulative P&L and the first trade that reaches the configured stop boundary are retained as evidence. Reaching the boundary exactly counts as a breach. Later completed trades are reported as continued trading after the stop condition.

When imported trade rows lack essential timestamps, realized P&L, quantity, or symbol, the affected rules return `NOT_EVALUABLE` instead of assuming zero losses or compliant exposure. Absent evidence never counts toward fully evaluated adherence.

This is not an unrealized-equity, trailing-drawdown, or prop-firm-rule calculation. If a completed-trade CSV has missing timestamps, overlapping completed positions, or tied close timestamps that prevent a reliable realized-P&L sequence, this rule is `NOT_EVALUABLE`.

## Privacy and limits

Every survival resource is scoped to its owner and account. Evaluations use only imported trade facts and configured user rules. The product should be read as an accountability aid, not financial, psychological, or compliance advice.

## Adherence history

The review shows the latest 30 current evaluation sessions for an accepted commitment. All displayed history counts refer only to that window. Adherence is `FOLLOWED / (FOLLOWED + BROKEN)` and is shown only when at least one session is fully evaluable; `PARTIALLY_OBSERVED` and `NOT_EVALUABLE` are excluded from that denominator. Corrected imports retain prior evaluation versions while exactly one current version exists per commitment and evaluated session.

Migration `V031__account_survival_evaluation_history.sql` enforces that current-version invariant with a partial unique index on `(commitment_id, evaluated_session_date)` where `is_current` is true. It is forward-only and compatible with existing rows because prior releases maintained at most one current evaluation per commitment; it does not delete historical versions.

## V1 release acceptance status

- **Implemented and reviewed in source:** immutable accepted commitments; account-owned serialized save/accept; idempotent acceptance; future-session eligibility; worst cumulative realized session-loss with ambiguous-sequence safeguards; durable per-session evaluation history/versioning; and evidence-limited same-user cross-account continuation.
- **Release gate:** require green hosted backend, PostgreSQL/Flyway, mandatory OWASP, Log4j negative control, Gitleaks, Semgrep, and frontend audit; independently execute and verify Account Survival Playwright tests against the latest branch head and review the user-visible flow. Hosted backend/security gates alone do **not** establish full release GO.
- **Security expiry:** the temporary exact-scope OWASP risk acceptance expires at `2026-11-30T00:00:00Z`. Future patches or configuration changes require reassessment before release.
- **Deployment:** PR #1 is not authorized to merge or deploy based solely on this document.
