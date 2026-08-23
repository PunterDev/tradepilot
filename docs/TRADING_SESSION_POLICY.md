# TradePilot Trading Session Policy

TradePilot classifies futures trades into sessions using one canonical policy so analytics, AI coaching, and historical comparisons do not change based on a user's browser, server, or account display timezone.

## Canonical Timezone

Session classification uses the futures market classification timezone:

`America/New_York`

The execution instant is the source of truth. Offset-aware timestamps are converted to `America/New_York` before session boundaries are applied.

TradePilot does not use these timezones for session classification:

- Browser timezone
- User display timezone
- Trading account display timezone
- JVM/server default timezone
- A fixed UTC offset such as `-05:00`

## Timestamp Concepts

| Concept | Role |
| --- | --- |
| Original timestamp and offset | Preserved from the source export where the current import model supports it. |
| Execution instant | Used for offset-aware imports such as Topstep `FilledAt`. |
| Market classification timezone | `America/New_York`; used only to decide `SessionType`. |
| User timezone | UI display concern only. |
| Account timezone | Optional display/reporting context only; not used for session classification. |
| Topstep `TradeDay` | Futures trading-day metadata used as the trade date when supplied; not an execution clock time. |

## Session Boundaries

Boundaries are centralized in `TradingSessionPolicy`.

All windows are start-inclusive and end-exclusive.

| Session | Market-local window |
| --- | --- |
| `ASIA` | `[18:00, 03:00)` cross-midnight |
| `LONDON` | `[03:00, 09:30)` |
| `NY_AM` | `[09:30, 12:30)` |
| `NY_PM` | `[12:30, 16:00)` |
| `UNKNOWN` | Any time with no configured window, including `[16:00, 18:00)` |

No configured windows overlap. Gaps intentionally map to `UNKNOWN` rather than being coerced into `NY_AM`.

## Daylight Saving Time

The classifier uses `ZoneId.of("America/New_York")` and IANA timezone rules. Offset-aware timestamps are converted through their `Instant`, so spring-forward gaps and fall repeated hours are deterministic.

Do not use fixed offsets as a substitute for `America/New_York`.

## Importer Behavior

### Topstep

Topstep `FilledAt` includes an explicit offset. TradePilot classifies the trade session from the opening execution `FilledAt` instant converted to `America/New_York`.

Topstep `TradeDay` can represent the following futures trading day. It is preserved as the trade date when present, but it is not used for session classification.

Example:

- `FilledAt = 06/15/2026 18:56:13 -05:00`
- `TradeDay = 06/16/2026 00:00:00 -05:00`

The trade belongs to trade date `2026-06-16`, but its session is classified from the `FilledAt` instant.

### Tradovate

The currently supported TradePilot-compatible Tradovate CSV timestamps are offset-less strings such as `6/16/2026 9:30:00 AM`.

Because those rows do not contain enough timezone context to derive an independent instant, TradePilot treats them explicitly as market-local `America/New_York` timestamps for session classification. The importer does not use the JVM default timezone.

If a future Tradovate export includes offsets or timezone metadata, parse it as an offset-aware timestamp and classify from the execution instant.

## UNKNOWN Behavior

`UNKNOWN` is returned when:

- The timestamp is missing.
- The timestamp cannot be parsed.
- No configured session window matches.
- Required timezone context is unavailable and no explicit source policy exists.

Downstream analytics and AI features must treat `UNKNOWN` as an explicit value. It should display as `Unknown Session`, not as `NY_AM`, blank, or an application error.

## Extending the Policy

For V1, TradePilot uses one futures market policy. If new markets need different sessions later, add a named market policy and route imports explicitly. Do not add importer-local boundary literals or timezone assumptions.
