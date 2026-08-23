# AI Performance Timeline

The AI Performance Timeline powers the TradePilot Journey page. It answers a coaching question that a normal P&L chart cannot: **am I becoming a better trader?**

## What It Analyzes

The backend groups account trades into the latest 12 monthly periods. Each month is classified as:

| Status | Rule | AI/Credit Behavior |
| --- | --- | --- |
| `READY` | 5 or more valid trades | Eligible for a full chapter |
| `LIMITED_DATA` | 1 to 4 trades | Deterministic summary only, no AI credits |
| `NO_ACTIVITY` | 0 trades | Deterministic no-activity message, no AI credits |

Metrics are calculated from trade data first. Narrative text is only allowed to explain facts already calculated by TradePilot.

## Evolution Score

The Trader Evolution Score is deterministic and ranges from 0 to 100. It is not a prediction and it is not based only on profitability.

Current category weights:

| Category | Weight |
| --- | ---: |
| Discipline | 25% |
| Risk Management | 25% |
| Execution | 20% |
| Consistency | 15% |
| Psychology | 15% |

Score confidence is based on monthly sample size:

| Trades | Confidence |
| ---: | --- |
| 5-9 | LOW |
| 10-29 | MEDIUM |
| 30+ | HIGH |

Inactive months are never rewarded as improvement.

## Caching

Each monthly snapshot stores a trade-data hash that includes:

- account id
- period dates
- trade ids and updated timestamps
- P&L and R values
- side/direction
- setup, confidence, emotion, and notes
- trade timestamps
- account default risk amount
- calculation and prompt versions

Unchanged historical months are reused from cache. If trade data changes, that month is recalculated.

## Credits

- Cached reads cost `0` AI credits.
- Limited/no-activity months cost `0` AI credits.
- Full regeneration costs `3` AI credits once per request when at least one eligible month exists.

## API

```http
GET /api/v1/ai/performance-timeline?accountId={accountId}
POST /api/v1/ai/performance-timeline/regenerate?accountId={accountId}
```

The legacy path is also available:

```http
/api/ai/performance-timeline
```

## Limitations

The score depends on imported trade quality and journal metadata completeness. Missing risk amount, setup, emotion, confidence, or notes reduce what TradePilot can infer. The score is a coaching signal, not a guarantee of future trading performance.
