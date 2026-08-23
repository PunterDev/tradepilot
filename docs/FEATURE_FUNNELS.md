# Feature Funnels & Conversion Analytics

Feature Funnels help TradePilot admins understand how users move through important product workflows, where they abandon them, and which behaviors are historically associated with upgrades or return usage.

Funnel analytics are observational. They describe historical user behavior and do not prove that a feature caused conversion, retention, or subscription outcomes.

## Funnel Definitions

The first release ships deterministic built-in funnels:

| Funnel | Purpose | Default Window |
| --- | --- | --- |
| Playbook Activation | Tracks Playbook viewing, setup creation, trade linking, AI review, and return behavior. | 14 days |
| Journey Engagement | Tracks Journey viewing, chapter expansion, AI regeneration, and return behavior. | 14 days |
| Leak Detector | Tracks leak analysis generation, detail review, recovery plan usage, and return behavior. | 14 days |
| Trade Review | Tracks trade detail viewing, AI review generation, review reading, and return behavior. | 7 days |
| Session Review | Tracks session review viewing, generation, action item review, and return behavior. | 14 days |
| Free-to-Pro Upgrade | Tracks locked preview interest through checkout and subscription activation. | 30 days |
| AI Credit Purchase | Tracks out-of-credit state through credit-pack purchase and post-purchase use. | 14 days |

## Step Definitions

Each step maps to:

- `feature`
- optional `action`
- optional `event_type`
- sequence order
- passive or active classification
- required/optional status

Historical events without `action` are still readable, but they cannot fully populate action-level steps. The dashboard reports these as data-quality warnings rather than silently treating them as complete workflows.

## Eligibility

Funnels use eligible-user denominators, not all registered users:

- Pro-only funnels count Pro users with relevant events.
- Free-to-Pro counts Free users with relevant upgrade or preview events.
- Admin users are excluded by default.
- Test/demo/sample/fake/dev/example emails are excluded unless `includeTestUsers=true`.

## Sequence Calculation

The backend calculates progression deterministically:

- Step N must occur after Step N-1.
- Events are ordered by `occurred_at`, then event `id`.
- Events outside the conversion window do not count.
- Duplicate step events on the same day are deduplicated for unique-user mode.
- Optional steps do not block completion.

Current calculation mode is optimized for unique users. Future releases may add full instance and session-level analyses.

## Metrics

The dashboard returns:

- eligible users
- entered users
- completed users
- completion rate
- completion rate among eligible users
- step-to-step conversion
- drop-off count and rate
- median and average transition time
- 7-day and 14-day repeat rate
- confidence level
- data-quality warnings

Confidence thresholds:

- `INSUFFICIENT`: fewer than 5 entrants
- `LOW`: 5-19 entrants
- `MEDIUM`: 20-99 entrants
- `HIGH`: 100+ entrants

## Conversion Association

Conversion association compares current Pro status among users who entered a workflow versus a comparison group in the same filtered event sample.

The API always includes this caveat:

> This is an observational association and does not establish causation.

Relative lift is suppressed when the comparison rate is zero or sample size is too small.

## Retention Association

Retention association measures whether funnel entrants returned with later feature events:

- returned within 7 days
- returned within 14 days
- returned within 30 days

This is also observational and should not be interpreted as causal proof.

## Cohorts

The MVP returns monthly entry cohorts with:

- entrants
- completions
- completion rate
- 7-day return
- 14-day return
- 30-day return

Future cohorts may include plan at entry, trade-count band, tenure band, and source feature.

## Privacy Rules

- User drill-down masks emails by default.
- Raw metadata JSON is not returned to the frontend.
- Payment details are not exposed.
- AI prompts and private trade notes are not exposed.
- All endpoints require admin authorization.

## Data Quality

The service reports:

- historical events missing action metadata
- events missing user IDs
- insufficient samples

Unknown action metadata is not silently ignored. Historical feature-only events can count broad feature entry, but not precise action completion.

## API Endpoints

Admin-only:

- `GET /api/v1/admin/feature-funnels`
- `GET /api/v1/admin/feature-funnels/{funnelKey}`
- `GET /api/v1/admin/feature-funnels/{funnelKey}/users`

Legacy routes under `/api/admin/feature-funnels` are also supported by the API versioning layer.

## Database Changes

Run:

- `backend/supabase_feature_funnels.sql`

It adds nullable action-level telemetry fields and indexes to `feature_usage_events`. It does not infer or backfill historical action semantics.

## Known Limitations

- Historical feature-only events cannot answer all action-level funnel questions.
- Configurable admin-created funnels are not part of the MVP.
- CSV export is deferred.
- Advanced device segmentation is deferred until device metadata is consistently captured.
