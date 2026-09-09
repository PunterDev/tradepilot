# First Insight Activation Implementation

## Audit Summary

TradePilot already had the main ingredients for a low-friction first-insight journey: authenticated signup/login, one-account Free limits, Tradovate/Topstep CSV import, account-scoped trade ownership, billing usage counters, feature access checks, AI credit accounting, feature usage telemetry, AI Trade Review, Leak Detector, and Daily/Weekly Coach.

The missing product layer was an authoritative activation state that connects those pieces into a guided first session:

1. Signup
2. Trading account creation
3. CSV import
4. First meaningful AI insight
5. Second AI action / contextual upgrade
6. Visible credit allowance
7. One-time Pro preview

## Implemented Backend Changes

- Added `/api/activation/status`.
- Added server-derived activation status from real account, import, trade, billing, preview, and insight state.
- Added durable `user_feature_previews` storage for a one-time Weekly Coach Pro preview.
- Added server-side preview reservation, release, and consumption states:
  - `AVAILABLE`
  - `RESERVED`
  - `CONSUMED`
  - `EXPIRED`
- Added `/api/coach/weekly/preview` for eligible Free users with an unlocked preview.
- Kept normal Weekly Coach behind existing Pro entitlement.
- Preserved billing/credit accounting: preview generation does not deduct monthly or purchased AI credits.
- Preview is consumed only after the async Weekly Coach generation succeeds.
- Preview is released if generation fails or if no AI generation is needed.
- Added activation telemetry events using the existing `feature_usage_events` table.
- Added signup and first-insight generation-start telemetry without changing auth or AI entitlement behavior.
- Added admin feature-usage summary metrics for activation funnel counts and median Time to First Insight intervals.
- Exposed AI feature credit costs in the existing billing usage response.

## Implemented Frontend Changes

- Added activation status API/hook.
- Added a first-insight dashboard panel driven by backend state.
- Added persistent AI credit visibility in the app shell.
- Added contextual CTAs for:
  - account creation
  - import trades
  - first trade review
  - Leak Detector when available
  - one-time Weekly Coach Pro preview
  - credit purchase when allowance is low
- Allowed `/coach` for Free users only when a server-side Weekly Coach preview is available.
- Kept locked feature pages and Pro entitlement checks intact.

## Funnel Events

New activation-level telemetry actions:

- `USER_SIGNED_UP`
- `TRADING_ACCOUNT_CREATED`
- `CSV_IMPORT_STARTED`
- `CSV_IMPORT_COMPLETED`
- `CSV_IMPORT_FAILED`
- `FIRST_IMPORT_COMPLETED`
- `FIRST_INSIGHT_AVAILABLE`
- `FIRST_INSIGHT_GENERATION_STARTED`
- `FIRST_INSIGHT_GENERATED`
- `SECOND_AI_FEATURE_USED`
- `AI_ALLOWANCE_LOW`
- `PRO_PREVIEW_UNLOCKED`
- `PRO_PREVIEW_GENERATION_STARTED`

The events are stored under `FeatureKey.ACTIVATION` so activation funnel analytics do not pollute normal feature usage counts.

## Database Changes

New Flyway migration:

- `V002__first_insight_activation.sql`

New historical Supabase migration for existing deployments:

- `supabase_first_insight_activation.sql`

New table:

- `user_feature_previews`

Key constraints:

- unique `(user_id, feature_key)`
- status check constraint
- foreign key to `users(id)` with cascade delete

## Security And Authorization

- Activation status is authenticated through existing `/api/**` security.
- Preview unlock and consumption are server-side only.
- The client cannot grant itself preview entitlement.
- Weekly Coach preview validates account ownership before generation.
- Normal Pro Weekly Coach access still requires existing `FeatureAccessService`.
- Import telemetry stores sanitized metadata only and never stores raw CSV rows.

## Remaining Recommendations

- Consider an admin/support activation timeline view so support can inspect why a user received a preview.
- Consider surfacing the first-insight panel in more empty states after more product telemetry accumulates.
- Consider moving Leak Detector minimum-trade readiness into a shared public constant to avoid duplicated value drift.
