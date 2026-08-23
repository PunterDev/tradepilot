# Supported Trade Import Formats

TradePilot imports broker exports into the canonical `trades` model so Dashboard, Calendar, Analytics, Session Review, AI Coach, Trading DNA, and Leak Detector continue to work from one data shape.

Imported futures trades use the canonical session policy documented in [TRADING_SESSION_POLICY.md](./TRADING_SESSION_POLICY.md). Session classification is based on `America/New_York` market time, not browser, user, account, or JVM default timezone.

## Tradovate

- Supported through the existing TradePilot-compatible Tradovate trade CSV flow.
- Direction uses explicit side/direction fields when available and falls back to buy/sell timestamp order.
- Current Tradovate timestamps are offset-less, so TradePilot treats them explicitly as `America/New_York` market-local timestamps for session classification.
- Existing duplicate protection uses deterministic external trade keys.

## Topstep Order History

Supported file shape: Topstep `orders_export.csv` order-history export.

Required headers include:

- `Id`
- `AccountName`
- `ContractName`
- `Status`
- `Size`
- `Side`
- `FilledAt`
- `ExecutePrice`
- `PositionDisposition`
- `CreationDisposition`

Import rules:

- Only `Status = Filled` rows with `FilledAt`, `ExecutePrice`, and `PositionDisposition` of `Opening` or `Closing` become executions.
- Cancelled rows and unfilled stop-loss/take-profit bracket orders are ignored.
- `Bid` is treated as buy and `Ask` is treated as sell.
- Direction comes from the opening execution: opening `Bid` is `LONG`; opening `Ask` is `SHORT`.
- Session classification uses the opening execution `FilledAt` instant converted to `America/New_York`.
- `TradeDay` is preserved as the trade date when present, but it is not used as the session clock time.
- Completed trades are reconstructed by position accounting per source account and contract.
- Incomplete open positions are reported in preview and are not persisted.
- Multiple Topstep `AccountName` values in one file are rejected for MVP safety.
- Gross PnL is calculated from futures point values. Unsupported contracts are rejected with a clear validation error.

Topstep duplicate keys are prefixed with `TOPSTEP` so they do not collide with Tradovate imports.
