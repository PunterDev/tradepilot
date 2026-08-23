# TradePilot RBAC and User Data Isolation

## Roles

TradePilot currently uses two application roles:

| Role | Access |
| --- | --- |
| `USER` | Own profile, trading accounts, trades, imports, analytics, AI reviews, coach snapshots, billing usage, purchased credits, and feature usage events. |
| `ADMIN` | Admin portal endpoints under `/api/v1/admin/**` and legacy `/api/admin/**`. Admin data access must go through admin endpoints. |

Future roles should be added explicitly in `UserRole`, `SecurityConfig`, and tests before use.

## Public Endpoints

These endpoints are intentionally public:

| Endpoint | Purpose |
| --- | --- |
| `/api/v1/auth/register` and `/api/auth/register` | User registration |
| `/api/v1/auth/login` and `/api/auth/login` | User login |
| `/api/v1/auth/verify-email` and `/api/auth/verify-email` | Email verification |
| `/api/v1/auth/forgot-password` and `/api/auth/forgot-password` | Password reset request |
| `/api/v1/auth/reset-password` and `/api/auth/reset-password` | Password reset completion |
| `/api/v1/contact` and `/api/contact` | Public contact form |
| `/api/stripe/webhook` | Stable Stripe webhook callback; signature verification is mandatory |
| `/actuator/health` | Operational health check |

Everything else under `/api/v1/**` and legacy `/api/**` requires authentication unless explicitly documented here.

## Authentication and Session Model

TradePilot uses stateless JWT access tokens for browser sessions.

- JWT signing uses `app.jwt.secret` / `JWT_SECRET`.
- The secret must be Base64 encoded and decode to at least 32 random bytes.
- The application fails startup if the JWT secret is missing, malformed, or too weak.
- JWT expiration is finite and configured by `app.jwt.expiration-minutes`; the default is 30 minutes.
- The application fails startup if JWT expiration is zero or negative.
- Request authentication loads the user from the database on every JWT-authenticated request.
- Backend authorization uses the database user role, not stale or forged JWT role claims.
- Missing, expired, malformed, wrong-secret, missing-subject, and deleted-user tokens must return `401 Unauthorized`.

JWTs include a `tokenVersion` claim tied to `users.token_version`. Password reset increments `users.token_version`, which invalidates older access tokens even if their `exp` has not passed. Run `backend/supabase_user_token_version.sql` before deploying token-version validation to an existing database.

Logout is client-side for the current stateless model: the frontend clears the access token and user cache. A logged-out token remains cryptographically valid until expiration unless `tokenVersion` is incremented by a password reset or future session revocation flow. TradePilot does not currently issue refresh tokens. If refresh tokens are added, they should be stored server-side as hashes, rotated on use, and delivered only through `HttpOnly`, `Secure`, `SameSite` cookies.

The frontend currently stores the access token in browser storage. This keeps the SPA simple, but it means XSS prevention remains critical. Do not log JWTs, reset tokens, verification links, authorization headers, or password reset tokens.

## Password Reset Tokens

Password reset and verification tokens are stored as hashes, expire, and are single-use. Password reset completion must:

- update the password hash,
- mark the reset token as used,
- invalidate remaining active reset tokens for the user,
- increment `users.token_version` to invalidate existing access JWTs.

## Admin-Only Endpoints

Spring Security requires `ADMIN` for:

- `/api/v1/admin/**`
- `/api/admin/**`

Normal users must receive `403 Forbidden`. Unauthenticated callers must receive `401 Unauthorized`.

Admin endpoints may use aggregate/global repositories such as `findAll()` because they are role-gated and intended for admin reporting. User-facing endpoints must not do this.

## User-Owned Endpoints

The following endpoint groups are authenticated and scoped to the current JWT principal:

| Endpoint Group | Ownership Rule |
| --- | --- |
| `/api/v1/accounts/**` | Account must belong to the authenticated user. |
| `/api/v1/trades/**` | Trade must belong to the authenticated user. |
| `/api/v1/stats/**` | Optional `accountId` must belong to the authenticated user. |
| `/api/v1/analytics/**` | Optional `accountId` must belong to the authenticated user. |
| `/api/v1/journals/**` | Optional `accountId` must belong to the authenticated user. |
| `/api/v1/discipline/**` | Trade must belong to the authenticated user. |
| `/api/v1/sessions/review/**` | `accountId` must belong to the authenticated user. |
| `/api/v1/coach/**` | `accountId` must belong to the authenticated user. |
| `/api/v1/ai/trade-review/**` | `tradeId` or review id must belong to the authenticated user. |
| `/api/v1/ai/trade-analysis/**` | `tradeId` or analysis id must belong to the authenticated user. |
| `/api/v1/ai/chart-review` | Optional `tradeId` must belong to the authenticated user. |
| `/api/v1/ai/trading-dna` | `accountId` must belong to the authenticated user or return a no-data response without leaking data. |
| `/api/v1/ai/leak-detector/**` | `accountId` must belong to the authenticated user. |
| `/api/v1/billing/**` | Billing and credit data is derived from the authenticated user only. |
| `/api/v1/feature-usage/track` | Feature usage is recorded for the authenticated user only. |

Legacy `/api/**` endpoints are normalized through the API versioning filter and must enforce the same rules.

## Ownership Enforcement Rules

- Do not trust `userId` from normal user requests.
- Derive the current user from the JWT security context.
- Validate every `accountId` with an owner-scoped lookup.
- Validate every `tradeId` with an owner-scoped lookup.
- For resource ids such as AI review ids, snapshot ids, or analysis ids, query by both id and current user id.
- Prefer `404 Not Found` for ownership failures to avoid revealing whether another user's resource exists.
- Log denied ownership access at `WARN` without sensitive values.

Reusable helper:

- `OwnershipGuard.requireCurrentUserId(user)`
- `OwnershipGuard.requireAccountOwner(user, accountId)`
- `OwnershipGuard.requireTradeOwner(user, tradeId)`
- `OwnershipGuard.requireTradeOwnerForAccount(user, tradeId, accountId)`

## Repository Hardening

User-facing code should prefer owner-filtered repository methods:

- `findByIdAndUser(...)`
- `findByIdAndUser_Id(...)`
- `findByIdAndUserId(...)`
- `findByUserAndAccount_Id...`
- `findByUser_IdAndAccount_Id...`
- `existsByUserAndAccount_Id...`

Avoid raw `findById(...)` in user-facing request paths unless followed by a clear owner check. Raw `findAll(...)` is acceptable only in admin, dev, startup, or background maintenance paths.

## API Versioning Note

`/api/v1/**` is canonical for launch. Legacy `/api/**` remains available during transition. The versioning filter normalizes `/api/v1/**` to existing controllers before Spring MVC handling, while Spring Security and rate limiting also account for both prefixes.

See `backend/API_VERSIONING.md` for the route versioning plan.

## Frontend RBAC

Frontend route guards and navigation hide admin routes for non-admin users, but this is only UX. Backend security is authoritative.

Expected frontend behavior:

- Non-admin users should not see admin nav items.
- Manual navigation to `/admin` as a normal user should show an unauthorized state or redirect.
- API `403` responses should render friendly "no access" messaging.
- Unexpected authenticated `401` responses should clear local auth state and return the user to login instead of continuing with stale credentials.

## Test Coverage

Security coverage lives in:

- `AuthHardeningTests`
- `RbacSecurityTests`
- `UserDataIsolationTests`
- `ApiVersioningTests`
- `AdminPortalTests`

The tests verify:

- Missing, invalid, expired, wrong-secret, missing-subject, and deleted-user JWTs receive `401`.
- Forged JWT role claims cannot elevate a database `USER` to admin access.
- Password reset tokens expire, are single-use, and invalidate existing access JWTs.
- `USER` cannot access `/api/v1/admin/**` or legacy `/api/admin/**`.
- `ADMIN` can access admin endpoints.
- Cross-user account update/delete/import/stats attempts are blocked.
- Cross-user trade read/update/delete/journal/AI review attempts are blocked.
- Chart review validates optional `tradeId` ownership before AI processing.
- Account-scoped analytics and AI endpoints reject another user's `accountId`.
- API versioning preserves the same protections for `/api/v1/**` and legacy `/api/**`.
