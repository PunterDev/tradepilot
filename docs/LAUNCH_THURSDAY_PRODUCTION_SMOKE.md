# Thursday Production Smoke

Date: 2026-09-09, America/Chicago

## Deployment

- Repository: `master` at `dcd13b6e6949f9e0cb86f5d37c0a2feb6af1d0bc` (`fix(deploy): remove browser dependency from prerender`).
- Working tree: clean before this report.
- Frontend: reachable at `https://www.tradepilotcoach.com`.
- Backend configured by the deployed frontend: `https://tradepilot-l4py.onrender.com/api`.

## Completed Production Checks

| Area | Status | Evidence |
| --- | --- | --- |
| HTTPS and canonical host | PASS | `https://www.tradepilotcoach.com` returned 200; `https://tradepilotcoach.com` returned 308 to the www HTTPS URL; HTTP returned 308 to HTTPS. |
| Robots and sitemap | PASS | `robots.txt` names the canonical www sitemap and disallows private application paths. `sitemap.xml` contains only the 18 intended public routes with canonical www URLs. |
| Live prerender | PASS | Homepage, futures journal, behavioral page, and trading expectancy calculator returned server-rendered title, description, canonical, H1, meaningful content, internal links, and expected JSON-LD before JavaScript. No route returned only an empty root. |
| Frontend security headers | PASS | Homepage returned HSTS, CSP with `frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, strict-origin referrer policy, and permissions policy. |
| Production API headers and anonymous protection | PASS | Recovered API returned uncached 401 responses in 0.09-0.24 seconds for `/auth/me`, `/admin/users`, `/dev/seed-demo-data`, and `/e2e/prepare`; responses included HSTS, CSP/frame protection, nosniff, referrer policy, permissions policy, and request IDs. |
| Registration safety | PASS | Controlled non-customer test registration returned 201. A repeat returned a readable 400, malformed input returned field validation errors without a stack trace, and login was correctly blocked until email verification. |
| Password-reset enumeration protection | PASS | Existing and non-existent test addresses both returned the same generic 200 response. |
| Public marketing | NEEDS MANUAL REVIEW | Public content correctly states no signals or profit promises and calls the Pro CTA a waitlist. The static homepage also contains the phrase `eligible V2 memory features`; confirm that this intentional wording accurately matches the launch offer. |

## Production Backend Availability Observation

The configured backend was initially unavailable during this smoke run:

- `GET https://tradepilot-l4py.onrender.com/actuator/health`: timed out after 90 seconds with zero response bytes.
- `GET https://tradepilot-l4py.onrender.com/api/auth/me`: timed out after 30 seconds with zero response bytes.
- `GET https://tradepilot-l4py.onrender.com/api/admin/users`: timed out after 30 seconds with zero response bytes.
- `GET https://tradepilot-l4py.onrender.com/api/dev/seed-demo-data`: timed out after 30 seconds with zero response bytes.

This was not a valid 401/403 authorization response. A later retry recovered with a valid 401 from `/api/auth/me` after 8.36 seconds, and subsequent protected-route checks completed in less than 0.25 seconds. Treat the initial 90-second zero-byte timeout as a production cold-start/availability risk and verify it against the Render service configuration before launch.

## Blocked Checks

The following were not executed because this environment has no controlled inbox to complete production email verification, no second verified normal user, and no production observability/admin access. No results are inferred from local tests.

- Verified-user login/logout, refresh, and expired-session behavior.
- Password-reset delivery, production reset-link domain, reset completion, and one-time reset-token behavior.
- Free plan/credit enforcement, founder Pre-Analysis access control, admin/dev endpoint authorization, and IDOR checks.
- Trading account creation, Tradovate and Topstep imports, duplicate prevention, analytics, and journal persistence.
- Trade Review, strategy-neutral ICT/non-ICT AI checks, Session Review, Daily Coach, Weekly Coach, credit accounting, and feature-usage events.
- Execution Engine, authenticated API cache behavior, production observability, and authenticated mobile workflows.

## Defects and Follow-up

- P0 gate incomplete: use controlled inbox-backed User A and User B accounts to finish all authenticated customer-journey, entitlement, import, AI, IDOR, and billing-credit checks before declaring launch GO.
- Investigate the initial 90-second zero-byte backend timeout as a potential Render cold-start/availability risk.
- No application code was changed during this smoke run.
- Stripe remains intentionally out of scope until Friday.

## Result

**THURSDAY PRODUCTION SMOKE - NOT GO**
