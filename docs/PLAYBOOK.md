# AI Trading Playbook

AI Trading Playbook helps traders define, validate, execute, and improve repeatable trading setups. It is centered on **Playbook vs. Actual Behavior**: documented rules are compared against linked historical trades so traders can see whether compliant trades behaved differently from noncompliant trades.

The Playbook summarizes historical process and execution behavior. It does not provide financial advice, guarantee future results, or prove that a setup will remain profitable.

## Setup Lifecycle

Supported setup statuses:

- `DRAFT`: incomplete or newly drafted setup.
- `TESTING`: setup is defined enough to link trades and collect evidence.
- `VALIDATED`: setup has enough documented rules for the trader to treat it as part of their process.
- `PAUSED`: setup is intentionally not in current use.
- `ARCHIVED`: setup is hidden from active workflows and cannot be linked until restored.

AI never automatically marks a setup validated. Status changes remain user-controlled.

## Setup Quality vs. Execution Quality

Setup Quality measures whether the opportunity matched the documented playbook.

Execution Quality measures how well the trader executed the opportunity.

Both use:

- `A`
- `B`
- `C`
- `D`
- `F`
- `UNGRADED`

## Compliance Score

Checklist compliance is deterministic:

- Required checklist items contribute 70% of the available score.
- Optional checklist items contribute 30% of the available score.
- `NOT_APPLICABLE` items are excluded from the denominator.
- `UNKNOWN` items reduce confidence.
- Required `NOT_MET` items add a penalty.
- Scores are clamped from 0 to 100.

Confidence levels:

- `INSUFFICIENT`
- `LOW`
- `MEDIUM`
- `HIGH`

## Playbook Health Score

Health Score is deterministic and separate from profitability:

- Definition completeness: 20%
- Historical evidence: 25%
- Rule adherence: 25%
- Performance consistency: 20%
- Recent relevance: 10%

Profit alone cannot produce a high score. Low sample size and inactivity reduce confidence or relevance.

## Playbook vs. Actual Behavior

For each required rule, TradePilot compares:

- Trades where the rule was met
- Trades where the rule was not met
- Average R and win rate for both groups
- Raw sample counts
- Confidence and interpretation

Low sample comparisons are labeled insufficient. The UI uses historical association language, not causation claims.

## AI Credit Behavior

- Passive Playbook overview/detail/analysis reads cost 0 credits.
- AI draft from description costs 1 credit.
- AI draft from selected trades costs 2 credits.
- AI review regeneration costs 3 credits.
- AI review regeneration requires at least 10 linked trades.
- Draft-from-trades requires at least 5 valid selected trades.
- Cached reads cost 0 credits.

## API Endpoints

Versioned routes:

- `GET /api/v1/playbook`
- `GET /api/v1/playbook/overview`
- `POST /api/v1/playbook`
- `GET /api/v1/playbook/{setupId}`
- `PUT /api/v1/playbook/{setupId}`
- `DELETE /api/v1/playbook/{setupId}`
- `POST /api/v1/playbook/{setupId}/restore`
- `POST /api/v1/playbook/{setupId}/duplicate`
- `POST /api/v1/playbook/{setupId}/trades/{tradeId}`
- `DELETE /api/v1/playbook/{setupId}/trades/{tradeId}`
- `POST /api/v1/playbook/{setupId}/trades/bulk-link`
- `PATCH /api/v1/playbook/assignments/{assignmentId}`
- `GET /api/v1/playbook/{setupId}/trades`
- `GET /api/v1/playbook/{setupId}/analysis`
- `POST /api/v1/ai/playbook/draft-from-description`
- `POST /api/v1/ai/playbook/draft-from-trades`
- `POST /api/v1/ai/playbook/{setupId}/regenerate`

Legacy `/api` compatibility routes are also registered.

## Database Tables

- `playbook_setup`
- `playbook_checklist_item`
- `trade_playbook_assignment`
- `playbook_analysis_snapshot`
- `playbook_milestone`

Migration:

- `backend/supabase_playbook.sql`

## Security

- Playbook is Pro-only.
- All endpoints require authentication.
- Setup access is scoped by `user_id`.
- Trade linking verifies that the trade belongs to the authenticated user.
- Account-scoped setups validate account ownership.
- Archived setups cannot be linked until restored.

## Known Limitations

- Current AI draft/review responses are structured and credit-gated, but use deterministic fallback generation until a dedicated OpenAI prompt is wired for Playbook.
- Trade Detail modal integration is not yet implemented in the MVP UI.
- Multiple setup assignments per trade are intentionally deferred; the schema uses a primary assignment model with a future-compatible assignment table.
