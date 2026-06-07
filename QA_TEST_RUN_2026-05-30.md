# TradePilot QA Test Run - 2026-05-30

## Feature Under Test

- Feature: Application QA smoke and risk pass
- Module: Auth, dashboard, analytics, admin access, billing/security backend coverage, observability
- Related ticket/PR: Not provided

## Build / Version

- Frontend build: `npm run build`
- Backend build: `mvn test`
- Commit SHA: `1036d81`
- Branch: `master`
- Release/version: Local workspace

## Tester

- Name: Codex
- Role: Senior QA Engineer / automation runner

## Date

- Test date: 2026-05-30
- Retest date: Not run

## Environment

- Environment: Local
- Frontend URL: `http://localhost:5173`
- Backend URL: `http://127.0.0.1:8080`
- Database: H2 in-memory `jdbc:h2:mem:tradepilot-e2e;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`
- Browser/device: Playwright Chromium desktop
- User timezone: America/Chicago
- Backend profile: `test`
- Feature flags: `app.dev-auth.enabled=true`, `app.dev-tools-enabled=true`

## Test Data Used

- Users: local dev user from `/api/dev/token`
- Roles: USER
- Plans: FREE
- Trading accounts: seeded demo account data through `/api/dev/seed-demo-data`
- CSV files: Not used in browser smoke pass
- Trade date ranges: seeded demo data
- Stripe test data: covered by backend tests only
- OpenAI mode: mocked/stubbed by backend tests where applicable
- Notes about seeded data: Playwright analytics smoke seeded demo data before visiting `/analytics`

## Checklist Results

| Area | Pass | Fail | Blocked | Not Tested | Notes |
| --- | --- | --- | --- | --- | --- |
| Critical risk assessment completed | X |  |  |  | Highest risk areas: auth/session handling, user isolation, admin authorization, billing/AI limits, config secret hygiene. |
| Happy path | X | X |  |  | Backend tests passed; frontend dashboard smoke reached the app shell but failed on ambiguous heading assertion. |
| Boundary and edge cases | X | X |  |  | Backend suite covered limits, duplicate webhooks, invalid signatures, token reuse; missing-JWT `/api/auth/me` returned 500. |
| Negative testing | X | X |  |  | Admin route denied normal user in UI and API; missing auth status bug found. |
| Concurrency and state risks | X |  |  |  | Backend tests covered duplicate Stripe webhook and AI/cache refresh behavior. |
| API tests | X | X |  |  | Maven API/service coverage passed; manual missing-JWT check failed expected status. |
| UI/UX tests | X | X |  |  | Invalid-session redirect happened but expected message was not visible. |
| Security tests | X | X |  |  | User isolation/admin denial covered; checked-in concrete dev credentials found. |
| Observability verification | X |  |  |  | Error responses/logs included `requestId`; backend tests covered metrics and alert flows. |
| Automation recommendations captured | X |  |  |  | See recommendations below. |

## Detailed Execution Notes

| Test ID | Scenario | Result | Evidence | requestId | Notes |
| --- | --- | --- | --- | --- | --- |
| QA-001 | Backend automated QA suite | Pass | `mvn test`: 42 tests, 0 failures, 0 errors, 0 skipped | Multiple request IDs in test logs | Covered billing limits, user isolation, auth verification/password reset, dev tools gating, admin portal, Stripe billing, runtime smoke, abuse protection. |
| QA-002 | Frontend production build | Pass | `npm run build`: Vite build completed | N/A | TypeScript compile and production bundle succeeded. |
| QA-003 | Playwright E2E smoke | Fail | `npm run e2e`: 2 passed, 2 failed, 1 skipped | N/A | Failures: ambiguous dashboard heading locator; invalid-session message absent. |
| QA-004 | API missing auth on `/api/auth/me` | Fail | `curl` returned HTTP 500 with generic error body | `38ca710b-e23a-4c6f-b197-e77bbf0f9aab` | Expected 401. Log showed NPE in `AuthService.me`. |
| QA-005 | API normal user denied `/api/admin/users` | Pass | `curl` returned HTTP 403 | Captured in backend logs | Confirms admin-only route denied a normal dev user. |
| QA-006 | API seeded demo data with auth | Pass | `curl` returned HTTP 200 | Captured in backend logs | Confirms dev data seed endpoint works under test/dev helper configuration. |

## Bugs Found

| Bug ID | Title | Severity | Area | Status | Evidence |
| --- | --- | --- | --- | --- | --- |
| BUG-001 | `/api/auth/me` without JWT returns 500 instead of 401 | High | Backend auth/API contract | Open | HTTP 500, requestId `38ca710b-e23a-4c6f-b197-e77bbf0f9aab`; NPE at `AuthService.me`. |
| BUG-002 | Invalid stored session redirects to login without session-expired message | Medium | Frontend auth UX | Open | Playwright failure: `/login` page rendered but message `Your session expired. Please log in again.` was absent. |
| BUG-003 | Dashboard smoke test locator is ambiguous | Low | E2E automation | Open | Locator matched both `Prop Firm Performance Dashboard` and `Welcome to TradePilot`. |
| BUG-004 | Concrete dev credentials are checked into `application-dev.yml` | Critical | Secret/config hygiene | Open | Redacted keys found in `backend/src/main/resources/application-dev.yml` lines 4, 5, 14, 15, 40, 41, 42, 43, 44. |

## Bug Details

### BUG-001: `/api/auth/me` without JWT returns 500 instead of 401

Severity: High

Area: Backend auth/API contract

Steps to Reproduce:
1. Start backend locally.
2. Request `GET /api/auth/me` without an `Authorization` header.

Expected Result:
HTTP 401 with a safe auth error response.

Actual Result:
HTTP 500 with generic response body and `requestId`.

Evidence:
- API response status: 500
- requestId: `38ca710b-e23a-4c6f-b197-e77bbf0f9aab`
- Log excerpt summary: NullPointerException because `AuthService.me` dereferences a null `User`.

Suspected Root Cause:
`AuthController.me` allows unauthenticated access to reach service code with `user == null`; `AuthService.me` logs null safely but then dereferences `user.getId()`.

Suggested Fix:
Require authentication at the controller/security layer for `/api/auth/me`, or return 401 before calling `AuthService.me` when the principal is null.

Regression Test Needed:
MockMvc/API test asserting missing, malformed, and expired JWTs return 401 for `/api/auth/me`.

### BUG-002: Invalid stored session redirects to login without session-expired message

Severity: Medium

Area: Frontend auth UX

Steps to Reproduce:
1. Store an invalid token and user object in localStorage.
2. Navigate to `/dashboard`.

Expected Result:
User is redirected to `/login` and sees `Your session expired. Please log in again.`

Actual Result:
User is redirected to `/login`, but no session-expired message appears.

Evidence:
- Playwright error context: login page rendered with `Welcome back`, no session-expired status message.

Suspected Root Cause:
`verifyStoredSession` catches invalid stored sessions and calls `logout()` only. It does not route through `finishSession(SESSION_EXPIRED_MESSAGE)` or preserve router state before the unauthenticated route fallback redirects to login.

Suggested Fix:
On invalid stored session, clear local state and navigate to `/login` with `state: { sessionExpiredMessage: SESSION_EXPIRED_MESSAGE }`.

Regression Test Needed:
Keep the existing Playwright invalid-session test and add a lower-level unit/integration test for the stored-session verification branch.

### BUG-003: Dashboard smoke test locator is ambiguous

Severity: Low

Area: E2E automation

Steps to Reproduce:
1. Run `npm run e2e`.
2. Observe `dashboard loads for an authenticated user`.

Expected Result:
The test asserts one stable dashboard heading or app shell element.

Actual Result:
Playwright strict mode fails because the regex matches both `Prop Firm Performance Dashboard` and `Welcome to TradePilot`.

Evidence:
- Playwright error context: locator resolved to two headings.

Suggested Fix:
Use a precise heading assertion such as `page.getByRole("heading", { name: "Welcome to TradePilot" })` for the no-account state, or scope the assertion to a page region.

Regression Test Needed:
Update Playwright smoke locator and rerun `npm run e2e`.

### BUG-004: Concrete dev credentials are checked into `application-dev.yml`

Severity: Critical

Area: Secret/config hygiene

Steps to Reproduce:
1. Inspect `backend/src/main/resources/application-dev.yml`.
2. Review database, mail, Stripe, admin, and contact configuration values.

Expected Result:
Dev config contains placeholders, safe examples, or environment variable references with harmless defaults.

Actual Result:
Concrete credential-like values are present in source-controlled dev configuration.

Evidence:
- Redacted sensitive-looking values found at lines 4, 5, 14, 15, 40, 41, 42, 43, and 44.

Suggested Fix:
Remove concrete values, rotate any exposed credentials, and keep only placeholder/example values in tracked files. Use `.env`, local untracked config, or secret manager-backed environment variables.

Regression Test Needed:
Add secret scanning to CI and block credential-like values in tracked config.

## Retest Status

- Retest required: Yes
- Bugs retested: None
- Regression tests added: None during this run
- Remaining risk: Manual CSV import, mobile/responsive QA, large dataset testing, real login credentials, Stripe live test mode, OpenAI live cost/rate-limit behavior, and database row inspection were not executed.

## Sign-Off Decision

- Decision: Blocked
- Sign-off owner: Not assigned
- Sign-off date: 2026-05-30
- Rationale: Backend automated tests and frontend build passed, but the QA pass found a critical checked-in secret hygiene issue, a backend auth status bug, and two frontend/E2E issues.
- Follow-up actions: Remove and rotate dev credentials, fix `/api/auth/me` unauthenticated handling, restore invalid-session login messaging, tighten Playwright dashboard locator, then rerun `mvn test`, `npm run build`, and `npm run e2e`.

## Automation Recommendations

- Add MockMvc coverage for `/api/auth/me` missing JWT, malformed JWT, expired JWT, and tampered JWT.
- Add CI secret scanning for YAML/properties/env example files.
- Add Playwright coverage for invalid stored session, logout messaging, and unauthenticated route redirects.
- Add focused Playwright selectors that target one stable element per state.
- Extend E2E with CSV import duplicate/reimport tests, two-tab account-selection behavior, and free-vs-Pro limit enforcement from the UI.
