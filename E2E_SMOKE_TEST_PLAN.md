# TradePilot E2E Smoke Test Plan

This plan covers the highest-risk product flows for a local/dev TradePilot environment. It intentionally avoids real secrets and production services.

## Environment

Required local services:

- Backend: `http://localhost:8080`
- Frontend: Playwright starts or reuses Vite on `http://localhost:5173` by default.
- Database: local/dev Supabase or Postgres with the documented SQL migrations applied.

Recommended backend flags for E2E:

```bash
DEV_AUTH_ENABLED=true
APP_DEV_TOOLS_ENABLED=true
SPRING_PROFILES_ACTIVE=dev
```

Optional E2E variables:

```bash
E2E_API_URL=http://localhost:8080/api
E2E_BASE_URL=http://localhost:5173
E2E_FRONTEND_PORT=5173
E2E_FRONTEND_HOST=localhost
E2E_LOGIN_EMAIL=local-dev-user@example.com
E2E_LOGIN_PASSWORD=local-dev-password
```

Do not use production Stripe, OpenAI, SMTP, or user credentials for smoke tests.

## Critical Smoke Matrix

| # | Flow | Preconditions | Smoke Steps | Expected Result |
|---|---|---|---|---|
| 1 | Register / login / logout | Backend running, email in dev log mode | Register new user, attempt login before verification, verify via dev/log token or backend helper, login, click Logout | Unverified login is blocked; verified login reaches dashboard; logout returns to auth page |
| 2 | Email verification behavior | Mail dev fallback enabled | Register user, inspect dev/log link, open `/verify-email?token=...` | Success message appears; login succeeds afterward |
| 3 | Password reset request | Mail dev fallback enabled | Open `/forgot-password`, submit email, inspect dev/log link, reset password | Generic success shown; old password fails; new password works |
| 4 | Create/select trading account | Authenticated user | Create account from onboarding or account selector | New account auto-selects; import controls enable |
| 5 | CSV import | Account selected | Upload Tradovate CSV, save import | Trades persist; dashboard metrics update |
| 6 | Dashboard loads | Authenticated user | Visit `/dashboard` | App shell, KPIs, import widget, preview cards render |
| 7 | Analytics heatmap loads correctly | Seeded trades across sessions | Visit `/analytics` | Performance Heatmap shows populated weekday/session cells, not global empty state |
| 8 | Trade detail modal opens | At least one trade exists | Click trade from table/calendar/timeline | Trade detail modal opens with metadata and AI review controls |
| 9 | AI Trade Review cached/regenerate | Trade exists and AI limits available | Generate review, reopen trade, regenerate | Cached review displays without extra usage; regenerate consumes usage |
| 10 | Daily / Weekly Coach | Trades exist and account selected | Generate Daily Coach, Generate Weekly Coach | Snapshot appears; cached snapshot remains viewable |
| 11 | Trading DNA cached/force refresh | Trades exist | Generate DNA, reload page, force refresh | Reload uses cache; force refresh consumes usage or shows friendly limit state |
| 12 | Session Review | Trades exist on selected date | Open `/session-review`, select account/date, generate review | Timeline, stats, best/worst trade, AI review render |
| 13 | Billing usage and Free/Pro limits | Free user and Pro/mock Pro user | Exhaust a limited AI action; mock upgrade to Pro | Free limit blocks fresh generation; cached data remains; Pro limits expand |
| 14 | Admin access denied for normal user | Normal user token | Visit `/admin` | Not Authorized/Admin access required page appears |
| 15 | Admin dashboards load | Admin user | Visit `/admin`, `/admin/users`, `/admin/observability` | Admin KPIs/tables render; normal users receive 403 from APIs |
| 16 | Rate limiting returns 429 | Low local rate-limit values | Repeat login/contact/reset endpoint requests | Response is HTTP 429 with safe message |
| 17 | Expired JWT / idle logout | Short JWT or idle env vars | Use expired token or wait idle timeout | Token/user are cleared; user is redirected to login with session-expired message |

## Starter Playwright Coverage

The current starter suite covers:

- Login form authenticates local dev user.
- Dashboard loads for authenticated local dev user.
- Normal user admin access control.
- Analytics heatmap renders seeded demo data.
- Invalid/expired auth redirects to login.

Authenticated route tests use `/api/dev/token` and skip safely when dev auth is disabled or the returned token cannot be validated. The real login-form test is opt-in through `E2E_LOGIN_EMAIL` and `E2E_LOGIN_PASSWORD` so no credentials are stored in the repo. The heatmap test also skips if dev demo seeding is disabled.

## Run Commands

From `frontend`:

```bash
npm run e2e
```

From repository root for full validation:

```bash
cd backend && mvn clean test
cd ../frontend && npm run build
cd ../frontend && npm run e2e
```

## Expansion Backlog

- Add CSV fixture upload for real import coverage.
- Add admin seeded user flow with `APP_ADMIN_EMAIL` or a dedicated dev-only admin token.
- Add AI usage assertions against `/api/billing/usage`.
- Add session review generation coverage with cached vs force refresh assertions.
- Add visual smoke screenshots for marketing homepage and app shell.
- Add CI job that runs E2E against an ephemeral test database.
