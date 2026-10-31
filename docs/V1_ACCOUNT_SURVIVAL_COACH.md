# Account Survival Coach V1

Account Survival Coach records observable trading-risk patterns, lets a trader accept a personal protection commitment, and evaluates later imported sessions against that immutable commitment. It does not infer motivation, diagnose emotion, or make a profitability claim.

## Commitment lifecycle

1. A commitment begins as `DRAFT` and may be edited by its owner.
2. Acceptance records `acceptedAt` once and changes the record to `ACCEPTED`.
3. Accepted values are immutable through the API. A changed rule requires a separate commitment for a future session.
4. Evaluations never overwrite the accepted commitment. Each eligible session has an auditable current evaluation; corrected imports create a new version for that session and retain the prior version.

An eligible evaluation session must be strictly later than both the effective session date and the calendar date on which the commitment was accepted. This prevents a newly accepted or backdated record from being represented as compliance with already completed trading.

## Observable evidence

The feature evaluates completed broker-imported round trips only. It can measure attempt count, normalized exposure, an allowed entry window, a post-loss pause, and maximum realized session loss. Fresh setup confirmation remains `NOT_EVALUABLE` without contemporaneous setup evidence.

Cross-account continuation is an observation across the trader's accounts. It never changes a commitment globally and is not presented as proof of intent or a prop-firm rule violation.

## Realized session-loss convention

Maximum session loss is evaluated from cumulative **realized** P&L in chronological completed-trade order, not the final session P&L. The worst cumulative P&L and the first trade that reaches the configured stop boundary are retained as evidence. Reaching the boundary exactly counts as a breach. Later completed trades are reported as continued trading after the stop condition.

This is not an unrealized-equity, trailing-drawdown, or prop-firm-rule calculation. If a completed-trade CSV has missing timestamps, overlapping completed positions, or tied close timestamps that prevent a reliable realized-P&L sequence, this rule is `NOT_EVALUABLE`.

## Privacy and limits

Every survival resource is scoped to its owner and account. Evaluations use only imported trade facts and configured user rules. The product should be read as an accountability aid, not financial, psychological, or compliance advice.
