# AI Trade Replay V3

## Product Behavior

AI Trade Replay reconstructs one-minute futures candles around an imported trade and adds factual entry, exit, excursion, and trade-management insights to Trade Detail. The first version supports `MES`, `MNQ`, `ES`, and `NQ` futures contracts imported from Tradovate or Topstep CSV workflows.

Replay windows are derived on the backend from owned trade data:

- 30 minutes before entry by default.
- Trade duration through final exit.
- 30 minutes after final exit by default.
- Six hours maximum total window by default.
- T+1 historical data only.

When the final exit is newer than the configured replay delay, the replay stays `WAITING_FOR_MARKET_DATA` and returns `eligibleAt`.

## Feature Flags

Backend:

```properties
tradepilot.features.v3-enabled=${V3_FEATURES_ENABLED:false}
```

Frontend:

```env
VITE_V3_FEATURES_ENABLED=false
```

`GET /api/features` and `GET /api/v1/features` include:

```json
{
  "v2FeaturesEnabled": true,
  "v3FeaturesEnabled": false,
  "features": {
    "tradeReplay": false
  }
}
```

Replay APIs enforce `Feature.TRADE_REPLAY` on the backend. The frontend also hides the Trade Detail card when V3 discovery disables `tradeReplay`.

## Market Data

Configuration:

```properties
tradepilot.market-data.provider=${MARKET_DATA_PROVIDER:disabled}
tradepilot.market-data.databento.api-key=${DATABENTO_API_KEY:}
tradepilot.market-data.databento.dataset=${DATABENTO_DATASET:GLBX.MDP3}
tradepilot.market-data.databento.schema=${DATABENTO_SCHEMA:ohlcv-1m}
tradepilot.market-data.databento.cost-check-enabled=${DATABENTO_COST_CHECK_ENABLED:true}
tradepilot.market-data.databento.max-request-cost-usd=${DATABENTO_MAX_REQUEST_COST_USD:0.05}
tradepilot.market-data.replay-delay-hours=${REPLAY_DELAY_HOURS:24}
tradepilot.market-data.pre-trade-minutes=${REPLAY_PRE_TRADE_MINUTES:15}
tradepilot.market-data.post-trade-minutes=${REPLAY_POST_TRADE_MINUTES:30}
tradepilot.market-data.max-window-hours=${REPLAY_MAX_WINDOW_HOURS:2}
tradepilot.market-data.worker-enabled=${REPLAY_WORKER_ENABLED:false}
tradepilot.market-data.cache-enabled=${REPLAY_CACHE_ENABLED:true}
```

The provider abstraction is `HistoricalMarketDataProvider`. The Databento implementation uses the official historical HTTP API:

- `POST https://hist.databento.com/v0/timeseries.get_range`
- `dataset=GLBX.MDP3`
- `schema=ohlcv-1m`
- `encoding=json`
- `stype_in=raw_symbol`
- `stype_out=raw_symbol`
- `pretty_px=true`
- `pretty_ts=true`
- `map_symbols=true`

The Databento API key is only read server-side and is never returned to clients or logged.

If the provider is disabled or the API key is missing, replay generation returns `PROVIDER_NOT_CONFIGURED`; app startup and imports continue normally.

## Contract Resolution

The resolver accepts actual supported contracts such as `MNQU6`, `MNQ-U-2026`, `MNQ 09-26`, `/MNQU6`, `ESM26`, or `MESU6`. It rejects ambiguous root or continuous symbols such as `NQ`, `ES`, `MNQ`, or `MES` because replay must not silently choose the wrong expiry or spend market-data requests on a guessed contract.

Imports now preserve both forms on `trades`:

- `symbol`: analytics root instrument, for example `MES`.
- `original_broker_symbol`: broker/export value, for example `MESU6`.
- `contract_symbol`: normalized exact contract, for example `MESU6`.
- `contract_symbol_source`: `IMPORTED`, `NORMALIZED`, `USER_CONFIRMED`, or `ADMIN_BACKFILL`.

For legacy root-only trades, Trade Detail exposes a contract confirmation workflow. The backend returns nearby quarterly candidates from the trade timestamp, validates the selected contract against the trade root, persists it as `USER_CONFIRMED`, and regenerates the replay.

## Database

Migrations:

- `backend/supabase_trade_contract_symbols.sql`
- `backend/supabase_trade_replay_v3.sql`

Tables:

- `trade_replays`: status, ownership, resolved symbol, replay window, eligibility, generated timestamp, failure fields, metrics JSON, AI summary fallback text.
- `market_candles`: cached provider candles keyed by provider, contract, interval, and timestamp.
- `trade_replay_insights`: ranked deterministic insights and optional AI explanation fields.

Persisted prices use `numeric`, not floating-point types.

## Statuses

`PENDING`, `WAITING_FOR_MARKET_DATA`, `FETCHING_DATA`, `ANALYZING`, `READY`, `PROVIDER_NOT_CONFIGURED`, `UNSUPPORTED_INSTRUMENT`, `CONTRACT_RESOLUTION_FAILED`, `NOT_ENOUGH_DATA`, and `FAILED`.

## Deterministic Formulas

For long trades:

- `MFE = max(candle.high - entryPrice)`
- `MAE = max(entryPrice - candle.low)`

For short trades:

- `MFE = max(entryPrice - candle.low)`
- `MAE = max(candle.high - entryPrice)`

Dollars are `points * futuresPointValue * quantity`. R values are only calculated when a valid stop-derived or stored risk denominator exists. The implementation does not infer emotions, moved stops, targets, or plan violations from price action alone.

## APIs

- `GET /api/v1/trades/{tradeId}/replay`
- `POST /api/v1/trades/{tradeId}/replay/generate`
- `POST /api/v1/trades/{tradeId}/replay/regenerate`
- `GET /api/v1/trades/{tradeId}/replay/candles`
- `GET /api/v1/trades/{tradeId}/replay/contract-candidates`
- `PUT /api/v1/trades/{tradeId}/replay/contract`

Compatibility aliases also exist under `/api/trades/{tradeId}/replay`.

All endpoints authenticate the current user, derive trade ownership from the JWT principal, enforce V3, and never accept a user id from the client.

## Frontend

`AITradeReplayCard` appears in Trade Detail when `tradeReplay` is enabled. It supports unrequested, waiting, processing, ready, provider unavailable, unsupported, resolution failure, not-enough-data, and failed states.

The ready state includes a responsive SVG candlestick chart, entry/final-exit markers, up to five primary coaching pins, deterministic metrics, a summary, reflection question, one-minute-resolution disclosure, and playback controls.

## Trading Memory

Replay stores semantic summaries and metrics, not raw candles. The current implementation prepares deterministic replay summaries but does not yet push a replay-specific Qdrant document. Future Qdrant integration should index only concise user-scoped replay facts and preserve existing trade-exclusion protections.

## Telemetry

New telemetry feature/action values are added for replay request, waiting, provider request/failure, generated, viewed, played, insight opened, reflection saved, regenerated, and similar-pattern events. Telemetry remains fail-open and must not include provider responses, credentials, or raw exception messages.

## Backfill

`TradeReplayService.backfill(...)` exists as a guarded hook that requires explicit `userId` and bounded `maxBatchSize`. A production admin controller or CLI should add dry-run output, date-range scanning, and progress summaries before broad use.

## Security And Reliability

- Backend derives symbols and windows from owned trades.
- Maximum window and returned-candle caps are enforced.
- Timestamps are stored in UTC instants.
- Provider errors are sanitized.
- Candle caching avoids repeated provider requests for the same contract/window.
- Import replay initialization runs after commit and fails open.

## Current Limitations

The existing `trades` table stores aggregate entry/exit data, not normalized execution rows. The V3 card therefore marks aggregate entry and final exit. True multiple-entry and partial-exit markers require persisting execution-level import rows in a future migration.

AI explanation generation is currently deterministic-summary fallback only. A future layer can reuse the existing OpenAI patterns with structured deterministic facts and validation.

## Licensing Warning

Deploying this feature to customers requires confirmation that TradePilot's Databento/CME agreement permits the intended historical-data display, caching, and derived analytics. This implementation does not resolve market-data licensing obligations.

## Local Development

1. Set `V3_FEATURES_ENABLED=true` and `VITE_V3_FEATURES_ENABLED=true`.
2. Leave `MARKET_DATA_PROVIDER=disabled` for UI/state testing.
3. Set `MARKET_DATA_PROVIDER=databento` and `DATABENTO_API_KEY` only when testing with an entitled Databento account.
4. Run `backend/supabase_trade_contract_symbols.sql`, then `backend/supabase_trade_replay_v3.sql`, after existing schema migrations.
5. Import an eligible supported futures trade and open Trade Detail.
