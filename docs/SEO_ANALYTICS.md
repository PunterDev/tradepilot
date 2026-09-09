# SEO Acquisition & Activation Analytics

TradePilot records privacy-safe SEO acquisition telemetry to connect public landing pages and free tools to aggregate activation outcomes.

## Attribution Model

- First-touch attribution is captured in the browser on the first SEO landing page or tool visit.
- Anonymous visitors receive a random local browser UUID. TradePilot does not fingerprint visitors.
- When a visitor registers, the optional first-touch payload is stored once in `user_acquisition_attribution`.
- Anonymous page, tool, and CTA events are stored in `seo_acquisition_events`.
- Product activation milestones continue to use feature usage telemetry. SEO attribution stores only milestone timestamps for attributed users.

## Dashboard

Admin route: `/admin/analytics/seo`

API: `GET /api/admin/analytics/seo?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`

Default range is the last 30 days.

## Metric Definitions

- Organic landing views: `seo_landing_page_view` plus `tool_page_view` events in the selected range.
- SEO CTA clicks: `seo_cta_click` and tool-specific `*_cta_click` events in the selected range.
- Signup started: `signup_started_from_seo` events in the selected range.
- SEO signups: users with `user_acquisition_attribution.signup_completed_at` in the selected range.
- First imports: attributed users with `first_import_completed_at` in the selected range.
- First insights: attributed users with `first_insight_generated_at` in the selected range. This is the activation definition.
- Pro conversions: attributed users with `pro_converted_at` in the selected range.
- Signup rate: signups divided by landing views.
- Activation rate: first insights divided by signups.
- Paid conversion rate: Pro conversions divided by signups.

## Tables

- Landing Page Performance groups by first-touch landing page and sorts by first insights.
- Free Tool Performance groups the three SEO tools: trading expectancy, overtrading, and revenge trading.
- Source / UTM Performance groups by `utm_source` and `utm_medium`, using `direct/unknown` and `unknown` when missing.
- Campaign Performance groups by `utm_campaign`, using `uncategorized` when missing.

## Latency

Median signup-to-first-import and signup-to-first-insight latency are calculated for attributed users whose signup occurred in the selected range.

## Privacy Safeguards

The SEO analytics tables do not store calculator inputs, trade P&L, strategy names, journal content, IP address, user agent, or browser fingerprint. Retention should be managed at the database policy level for `seo_acquisition_events`; a 13-month retention window is recommended for marketing trend analysis.

## Security & Integrity

Public endpoint threat model: `POST /api/v1/seo/events` is intentionally anonymous, so synthetic traffic cannot be eliminated. Practical defenses are strict event-name allowlisting, a narrow DTO, server-side validation, anonymous IP-based rate limiting, event idempotency, server-authoritative timestamps, and bounded batch size.

Allowed events are defined in `SeoAcquisitionEventName`. Unknown event names return `400` and are not persisted.

The public event DTO accepts only event name, event id, anonymous visitor id, landing path, tool, CTA labels/destination, referrer, UTM fields, and a legacy client timestamp field that is ignored for reporting. Unexpected fields such as P&L, expectancy, win rate, email, strategy, calculator inputs, arbitrary metadata, or milestone timestamps return `400`.

Landing pages and CTA destinations must be path-only values beginning with `/`, with no query string, fragment, control characters, `javascript:` scheme, or oversized value. Referrers and UTM fields are trimmed, length-limited, stored as plain data, and rendered by React text output only.

Rate limiting and request-size checks reuse TradePilot's existing `RateLimitInterceptor`: `seo-events` defaults to 120 requests per 60 seconds per anonymous request subject, and requests with `Content-Length` above `RATE_LIMIT_SEO_EVENTS_MAX_CONTENT_BYTES` default to `413` at 16 KB. Rate-limit responses use the existing JSON error style with `429` and `Retry-After`.

Idempotency uses a random frontend-generated UUID `eventId`. The database keeps a nullable unique index on non-empty event IDs, so duplicate retransmissions are harmless while legitimate repeated user actions can still be counted with new event IDs.

Event `occurred_at` is server-authoritative for public ingestion. Client timestamps are accepted only for frontend compatibility and do not affect reports.

First-touch attribution is immutable per user because `user_acquisition_attribution.user_id` is unique and registration recording returns when attribution already exists. Milestone timestamps can update later, but only if the specific milestone is empty.

Milestones are server-controlled. Signup comes from successful registration, first import from successful CSV import with saved rows, first insight from activation/insight generation, and Pro conversion only after the user has actual Pro entitlement state. Public analytics events and registration payloads cannot set first import, first insight, or Pro timestamps.

Admin reporting is protected by Spring Security's `/api/admin/**` and `/api/v1/admin/**` ADMIN matcher. The frontend route is not the security boundary.

## Google Search Console Integration

Admin API: `GET /api/admin/analytics/seo/search-console?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`

The Search Console integration is disabled by default. When disabled, the backend starts normally and the dashboard shows "Google Search Console is not connected." Internal TradePilot SEO analytics continue to work without Google credentials.

Search Console data is aggregated. TradePilot does not identify an individual Google click or join a Search Console click to a user. The dashboard joins Search Console page/query/page-date aggregates to TradePilot conversion aggregates by normalized landing page path.

Implemented Search Console query types:

- Page performance: `page`
- Query performance: `query`
- Query plus page performance: `query,page`
- Daily trend: `date`

All Search Console requests use web search data, finalized data, and the admin-selected date range. Google reports Search Console dates using Search Console date semantics, not the admin browser timezone, and recent data can lag or be incomplete.

Search Console page URLs are normalized only when they belong to the configured canonical host or its `www` form:

- `https://tradepilotcoach.com/revenge-trading` -> `/revenge-trading`
- `https://www.tradepilotcoach.com/revenge-trading/` -> `/revenge-trading`
- `https://tradepilotcoach.com/revenge-trading?utm_source=x#top` -> `/revenge-trading`
- `https://tradepilotcoach.com/` -> `/`

External URLs are ignored and are not mapped into TradePilot landing pages.

Derived ratios are aggregate comparisons:

- Signup / GSC Click = landing-page signups divided by Search Console clicks for that page.
- Activation / GSC Click = landing-page first insights divided by Search Console clicks for that page.
- Pro / GSC Click = landing-page Pro conversions divided by Search Console clicks for that page.
- Activated / 100 Search Clicks and Pro / 100 Search Clicks use the same aggregate page-level model.

Search Console query strings are treated as untrusted text and rendered by React text output only.

The backend caches Search Console responses in memory by property and date range. Default TTL is 60 minutes. Default page size and maximum rows per query type are 25,000, with a hard safety cap of 50,000 rows per query type. Multi-instance Render deployments may have one cache per instance.

Search Console can omit anonymized or low-volume queries. Its totals are not a raw event stream and will not always match TradePilot internal SEO event counts.

## Google Search Console Setup

1. Create or select a Google Cloud project.
2. Enable the Google Search Console API.
3. Create a service account.
4. Add the service account email as a user on the TradePilot Search Console property with read-only access.
5. Configure Render backend environment variables:
   - `GSC_ENABLED=true`
   - `GSC_PROPERTY=sc-domain:tradepilotcoach.com` for a domain property, or `GSC_PROPERTY=https://tradepilotcoach.com/` for a URL-prefix property.
   - `GSC_SERVICE_ACCOUNT_JSON_BASE64=<base64 encoded service account JSON>`
   - Optional: `GSC_MAX_ROWS_PER_QUERY=25000`
   - Optional: `GSC_CACHE_TTL_MINUTES=60`
   - Optional: `GSC_BRAND_TERMS=tradepilot,trade pilot,tradepilotcoach`
6. Redeploy the backend.
7. Visit `/admin/analytics/seo` and confirm the Search Console status shows connected.

Do not commit service account JSON files. Do not expose Search Console credentials through frontend or `VITE_` variables.

## Historical Limitations

Attribution starts after the SEO analytics migration and frontend telemetry are deployed. Existing users and earlier SEO visits cannot be reliably backfilled into first-touch attribution.
