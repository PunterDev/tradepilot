# E2E Baseline

Date: 2026-09-07

## Current Status

- Suite: `frontend` Playwright E2E
- Command: `npm run e2e`
- Total tests: 74
- Passing: 72
- Skipped: 2
- Unexpected failures: 0
- Retries required: 0

The suite was run twice after fixes. Both full runs completed with `73 passed`, `2 skipped`, and `0 failed`.

## Intentional Skips

- `e2e/auth-and-dashboard.spec.ts` - `login form authenticates a local dev user`
  - Requires `E2E_LOGIN_EMAIL` and `E2E_LOGIN_PASSWORD`.
  - This is a real login smoke test and is intentionally skipped when credentials are not configured.
- `e2e/auth-and-dashboard.spec.ts` - `analytics heatmap renders seeded demo data`
  - Requires a local dev backend with dev auth and demo reseeding enabled.
  - This is intentionally skipped when the dev backend/session or seeding endpoint is unavailable.

## Fixed Baseline Failures

| Test | Classification | Root Cause | Resolution |
| --- | --- | --- | --- |
| `pre-analysis result collapses inputs and prioritizes trader posture` | `TEST_ENVIRONMENT_ISSUE` | The test navigated through `/dashboard`, which fired unmocked dashboard/execution/stats API calls. Real 401 responses cleared the mocked auth session before the gated `AI Pre-Analysis` nav link appeared. | Mocked the dashboard bootstrap calls and waited for the gated nav link before clicking. |
| `pre-analysis mobile layout avoids horizontal overflow` | `TEST_ENVIRONMENT_ISSUE` | Same Pre-Analysis fixture issue as above. | Same bootstrap mocks; no product gate changes. |
| `admin SEO acquisition dashboard renders KPI, funnel, tool, source, and campaign sections` | `TEST_ENVIRONMENT_ISSUE` | The mocked admin auth/session setup did not cover all admin shell bootstrap calls, allowing real 401 responses to expire the test session. | Added deterministic auth, account, billing, feature, pre-analysis access, admin dashboard, and dashboard-stat mocks. |

## Admin Feature Usage Coverage

Added focused mocked E2E coverage for the Admin Feature Usage page:

- route loads for an admin user
- admin header shows admin workspace context
- normal user account/credit header text is absent
- six product KPI labels render
- filters and sorting render
- feature card drilldown opens
- no browser console errors are emitted

## Timeout And Retry Policy

- No global timeout increase was made.
- No blanket skips were added.
- No retries were added or required for the green baseline.
- Fixes use deterministic API mocks and visible-state assertions instead of arbitrary sleeps.

## Commands

- `npx playwright test`
- `npx playwright test e2e/pre-analysis-ui.spec.ts --grep "pre-analysis result collapses inputs and prioritizes trader posture" --repeat-each=3`
- `npx playwright test e2e/pre-analysis-ui.spec.ts --grep "pre-analysis mobile layout avoids horizontal overflow" --repeat-each=3`
- `npx playwright test e2e/seo-analytics-admin.spec.ts e2e/admin-feature-usage.spec.ts --repeat-each=3`
- `npm run e2e`
