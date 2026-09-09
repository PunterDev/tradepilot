# TradePilot Backend Authentication And Authorization Audit

Audit date: 2026-08-29

Scope: actual Spring Boot backend endpoints under `backend/src/main/java/com/tradepilot`. Both canonical `/api/v1/**` routes and legacy `/api/**` routes are in scope because `ApiVersioningFilter` rewrites `/api/v1/**` to `/api/**`.

Production systems and external services were not accessed.

## Executive Summary

Result after fixes: no remaining CRITICAL, HIGH, or MEDIUM authorization findings were identified in the reviewed backend code.

One HIGH issue was found and fixed: production could previously start with `DEV_AUTH_ENABLED=true` or `DEV_TOOLS_ENABLED=true`, exposing local-development controls in a production profile if misconfigured. A production startup validator now rejects those settings.

The remaining MEDIUM findings were fixed during final hardening: `/api/auth/me` now requires authentication at the HTTP filter layer, and playbook/trading-memory ownership misses now normalize to `404` instead of exposing distinguishable `400` responses.

Most user data APIs derive ownership from `@AuthenticationPrincipal User user` and enforce object-level ownership through user-scoped repository methods such as `findByIdAndUser`, `findByIdAndUser_Id`, `findByIdAndUserId`, `OwnershipGuard`, or `ResourceNotFoundException`.

## Security Boundary

- API versioning: `/api/v1/**` is internally rewritten to `/api/**` by `backend/src/main/java/com/tradepilot/config/ApiVersioningFilter.java`.
- Authentication: all non-public backend routes require JWT authentication via `backend/src/main/java/com/tradepilot/security/SecurityConfig.java`.
- Admin authorization: `/api/admin/**` and `/api/v1/admin/**` require `ROLE_ADMIN`.
- Public endpoints: auth entrypoints, feature availability, contact form, health, and Stripe webhook are intentionally public at the HTTP filter layer.
- Stripe webhook: public transport endpoint; billing authorization depends on Stripe signature verification and event validation, audited separately in Stripe readiness work.

## Endpoint Inventory And Verdicts

| Endpoint | Methods | Controller | Authorization verdict |
|---|---:|---|---|
| `/api/auth/register` | POST | `AuthController` | Public by design; rate limited; creates only caller account |
| `/api/auth/login` | POST | `AuthController` | Public by design; rate limited |
| `/api/auth/me` | GET | `AuthController` | Authenticated at SecurityConfig filter layer; controller retains principal guard |
| `/api/auth/verify-email` | POST | `AuthController` | Public token flow; tokens are hashed/single-purpose |
| `/api/auth/resend-verification` | POST | `AuthController` | Public email flow; rate limited |
| `/api/auth/forgot-password` | POST | `AuthController` | Public token flow; rate limited |
| `/api/auth/reset-password` | POST | `AuthController` | Public token flow; tokens are hashed/single-use |
| `/api/accounts` | GET, POST | `TradingAccountController` | Authenticated; user-scoped |
| `/api/accounts/{id}/risk` | PATCH | `TradingAccountController` | Authenticated; account ownership checked |
| `/api/accounts/{id}` | DELETE | `TradingAccountController` | Authenticated; account ownership checked |
| `/api/trades` | GET, POST | `TradeController` | Authenticated; user-scoped list/create |
| `/api/trades/{id}` | GET, PUT, DELETE | `TradeController` | Authenticated; trade ownership checked |
| `/api/trades/{id}/detail` | GET | `TradeController` | Authenticated; trade ownership checked before detail hydration |
| `/api/trades/{id}/journal` | PATCH | `TradeController` | Authenticated; trade ownership checked |
| `/api/trades/import` | POST | `TradeController` | Authenticated; target account ownership checked |
| `/api/trades/import/preview` | POST multipart | `TradeController` | Authenticated; target account ownership checked |
| `/api/trades/import/csv` | POST multipart | `TradeController` | Authenticated; target account ownership checked |
| `/api/trades/repair-tradovate-directions` | POST | `TradeController` | Authenticated; account filter ownership checked when supplied |
| `/api/stats/summary` | GET | `StatsController` | Authenticated; account filter ownership checked |
| `/api/stats/by-setup` | GET | `StatsController` | Authenticated; account filter ownership checked |
| `/api/stats/by-session` | GET | `StatsController` | Authenticated; account filter ownership checked |
| `/api/stats/by-symbol` | GET | `StatsController` | Authenticated; account filter ownership checked |
| `/api/stats/by-weekday` | GET | `StatsController` | Authenticated; account filter ownership checked |
| `/api/analytics/friday-opex` | GET | `TradeAnalyticsController` | Authenticated; account filter ownership checked |
| `/api/analytics/sessions` | GET | `TradeAnalyticsController` | Authenticated; account filter ownership checked |
| `/api/analytics/heatmap` | GET | `TradeAnalyticsController` | Authenticated; account filter ownership checked |
| `/api/analytics/calendar-summary` | GET | `TradeAnalyticsController` | Authenticated; account filter ownership checked |
| `/api/analytics/behavioral-insights` | GET | `TradeAnalyticsController` | Authenticated; account filter ownership checked |
| `/api/journals` | GET, POST | `JournalController` | Authenticated; user-scoped |
| `/api/journals/by-date` | GET | `JournalController` | Authenticated; account/date access scoped to principal |
| `/api/discipline/trades/{tradeId}/violations` | GET | `DisciplineController` | Authenticated; trade ownership checked first |
| `/api/sessions/review` | GET | `SessionReviewController` | Authenticated; account ownership checked |
| `/api/sessions/review/generate` | POST | `SessionReviewController` | Authenticated; account ownership checked before credit usage |
| `/api/coach/daily` | GET | `AiCoachController` | Authenticated; account ownership checked |
| `/api/coach/weekly` | GET | `AiCoachController` | Authenticated; account ownership checked |
| `/api/coach/daily/generate` | POST | `AiCoachController` | Authenticated; account ownership checked before credit usage |
| `/api/coach/weekly/generate` | POST | `AiCoachController` | Authenticated; account ownership checked before credit usage |
| `/api/coach/snapshots` | GET | `AiCoachController` | Authenticated; account ownership checked |
| `/api/ai/trade-review` | POST | `AiController` | Authenticated; trade ownership checked before AI/credit usage |
| `/api/ai/trade-review/async` | POST | `AiController` | Authenticated; trade ownership checked before job creation |
| `/api/ai/trade-review/status/{reviewId}` | GET | `AiController` | Authenticated; `reviewId` scoped by `userId` |
| `/api/ai/weekly-review` | POST | `AiReflectionController` | Authenticated; trade query scoped to principal |
| `/api/ai/chart-review` | POST multipart | `AiChartReviewController` | Authenticated; optional `tradeId` ownership checked |
| `/api/ai/trade-analysis/async` | POST multipart | `AiTradeAnalysisController` | Authenticated; trade ownership checked before job creation |
| `/api/ai/trade-analysis/status/{analysisId}` | GET | `AiTradeAnalysisController` | Authenticated; `analysisId` scoped by `userId` |
| `/api/ai/leak-detector` | GET | `LeakDetectorController` | Authenticated; account ownership checked |
| `/api/ai/leak-detector/regenerate` | POST | `LeakDetectorController` | Authenticated; account ownership checked before credit usage |
| `/api/ai/trading-dna` | GET | `TradingDnaController` | Authenticated; account scope checked; invalid account returns empty response |
| `/api/ai/performance-timeline` | GET | `PerformanceTimelineController` | Authenticated; account ownership checked |
| `/api/ai/performance-timeline/regenerate` | POST | `PerformanceTimelineController` | Authenticated; account ownership checked before credit usage |
| `/api/trades/{tradeId}/replay` | GET | `TradeReplayController` | Authenticated; trade ownership checked |
| `/api/trades/{tradeId}/replay/generate` | POST | `TradeReplayController` | Authenticated; trade ownership checked |
| `/api/trades/{tradeId}/replay/regenerate` | POST | `TradeReplayController` | Authenticated; trade ownership checked |
| `/api/trades/{tradeId}/replay/candles` | GET | `TradeReplayController` | Authenticated; trade ownership checked |
| `/api/trades/{tradeId}/replay/contract-candidates` | GET | `TradeReplayController` | Authenticated; trade ownership checked |
| `/api/trades/{tradeId}/replay/contract` | PUT | `TradeReplayController` | Authenticated; trade ownership checked |
| `/api/trades/{tradeId}/trading-memory` | GET | `TradingMemoryController` | Authenticated; trade ownership checked before vector hydration |
| `/api/trades/{tradeId}/trading-memory/regenerate` | POST | `TradingMemoryController` | Authenticated; indexer checks trade/user ownership |
| `/api/trades/{tradeId}/similar-trades` | GET | `TradingMemoryController` | Authenticated; trade ownership checked; vector hits rehydrated by `userId` |
| `/api/playbook` | GET, POST | `PlaybookController` | Authenticated; user-scoped |
| `/api/playbook/overview` | GET | `PlaybookController` | Authenticated; user-scoped |
| `/api/playbook/{setupId}` | GET, PUT, DELETE | `PlaybookController` | Authenticated; setup ownership checked |
| `/api/playbook/{setupId}/restore` | POST | `PlaybookController` | Authenticated; setup ownership checked |
| `/api/playbook/{setupId}/duplicate` | POST | `PlaybookController` | Authenticated; setup ownership checked |
| `/api/playbook/{setupId}/trades/{tradeId}` | POST, DELETE | `PlaybookController` | Authenticated; setup and trade ownership checked |
| `/api/playbook/{setupId}/trades/bulk-link` | POST | `PlaybookController` | Authenticated; each trade ownership checked |
| `/api/playbook/{setupId}/trades` | GET | `PlaybookController` | Authenticated; setup ownership checked before assignment list |
| `/api/playbook/assignments/{assignmentId}` | PATCH | `PlaybookController` | Authenticated; assignment ownership checked |
| `/api/playbook/{setupId}/analysis` | GET | `PlaybookController` | Authenticated; setup ownership checked |
| `/api/ai/playbook/draft-from-description` | POST | `PlaybookAiController` | Authenticated; feature/credit check against principal |
| `/api/ai/playbook/draft-from-trades` | POST | `PlaybookAiController` | Authenticated; selected trades filtered to current user |
| `/api/ai/playbook/{setupId}/regenerate` | POST | `PlaybookAiController` | Authenticated; setup ownership checked |
| `/api/billing/usage` | GET | `BillingController` | Authenticated; principal-only |
| `/api/billing/create-checkout-session` | POST | `BillingController` | Authenticated; principal-derived Stripe customer/subscription |
| `/api/billing/create-credit-pack-checkout-session` | POST | `BillingController` | Authenticated; server-side pack catalog |
| `/api/billing/create-portal-session` | POST | `BillingController` | Authenticated; customer derived from principal |
| `/api/stripe/webhook` | POST | `StripeWebhookController` | Public transport; Stripe signature/event validation required |
| `/api/feature-usage/track` | POST | `FeatureUsageController` | Authenticated; telemetry attributed to current principal |
| `/api/features` | GET | `FeatureAvailabilityController` | Public by design; no user data |
| `/api/contact` | POST | `ContactController` | Public by design; rate limited |
| `/api/dev/token` | GET | `DevAuthController` | Conditional dev-only endpoint; now rejected at startup in prod/production |
| `/api/dev/seed-demo-data` | POST | `DevToolsController` | Authenticated and dev-tools gated; now rejected at startup in prod/production |
| `/api/dev/clear-demo-data` | POST | `DevToolsController` | Authenticated and dev-tools gated; now rejected at startup in prod/production |
| `/api/dev/demo-data/reseed` | POST | `DevToolsController` | Authenticated and dev-tools gated; now rejected at startup in prod/production |
| `/api/dev/trades/{tradeId}/replay` | DELETE | `DevToolsController` | Authenticated, dev-tools gated, trade ownership checked |
| `/api/dev/mock-upgrade-pro` | POST | `DevToolsController` | Authenticated and dev-tools gated; now rejected at startup in prod/production |
| `/api/dev/mock-downgrade-free` | POST | `DevToolsController` | Authenticated and dev-tools gated; now rejected at startup in prod/production |
| `/api/dev/feature-usage/backfill` | POST | `DevToolsController` | Authenticated and dev-tools gated; now rejected at startup in prod/production |
| `/api/admin/users` | GET | `AdminUserController` | Admin only |
| `/api/admin/users/search` | GET | `AdminUserController` | Admin only |
| `/api/admin/users/{userId}` | GET | `AdminUserController` | Admin only |
| `/api/admin/users/{userId}/upgrade-pro` | POST | `AdminUserController` | Admin only |
| `/api/admin/users/{userId}/downgrade-free` | POST | `AdminUserController` | Admin only |
| `/api/admin/users/{userId}/make-admin` | POST | `AdminUserController` | Admin only |
| `/api/admin/users/{userId}/remove-admin` | POST | `AdminUserController` | Admin only |
| `/api/admin/dashboard/*` | GET | `AdminDashboardController` | Admin only |
| `/api/admin/contact-messages/*` | GET/PATCH/POST | `AdminContactController` | Admin only |
| `/api/admin/billing/summary` | GET | `AdminBillingController` | Admin only |
| `/api/admin/feature-usage/*` | GET | `AdminFeatureUsageController` | Admin only |
| `/api/admin/retention/*` | GET | `AdminRetentionAnalyticsController` | Admin only |
| `/api/admin/feature-funnels/**` | GET | `AdminFeatureFunnelController` | Admin only |
| `/api/admin/observability/**` | GET/PATCH | `AdminObservabilityController` | Admin only plus method-level `@PreAuthorize` |
| `/api/admin/trading-memory/**` | GET/POST | `AdminTradingMemoryController` | Admin only plus method-level `@PreAuthorize` |
| `/api/admin/trades/cleanup-range` | DELETE | `AdminTradeCleanupController` | Admin only; target account must belong to target user |
| `/actuator/health` | GET | Actuator | Public health only |

## Findings

### CRITICAL

No CRITICAL authorization findings remain after this audit.

### HIGH

#### HIGH-1: Production Could Be Started With Dev Auth/Dev Tools Enabled

- File: `backend/src/main/java/com/tradepilot/security/SecurityConfig.java:70`
- File: `backend/src/main/java/com/tradepilot/controller/DevAuthController.java:13`
- File: `backend/src/main/java/com/tradepilot/dev/DevToolsAvailability.java:17`
- Problem: `DEV_AUTH_ENABLED=true` makes `/api/dev/token` anonymous, and `DEV_TOOLS_ENABLED=true` enables demo mutation/mock billing endpoints for authenticated users. Before this fix, production startup did not reject these settings.
- Production impact: A Render/environment misconfiguration could expose token minting or dev mutation endpoints in production.
- Recommended fix: Fail startup in `prod`/`production` when either `DEV_AUTH_ENABLED` or `DEV_TOOLS_ENABLED` is true.
- Fix implemented: `backend/src/main/java/com/tradepilot/security/SecurityProductionConfigValidator.java:22` rejects both settings in production profiles.
- Verification: `backend/src/test/java/com/tradepilot/security/SecurityProductionConfigValidatorTests.java` covers production rejection and non-production allowance.

### MEDIUM

No MEDIUM authorization findings remain after final hardening.

Previously open MEDIUM items were fixed:

- `/api/auth/me` now requires authentication in `SecurityConfig` for both `/api/auth/me` and `/api/v1/auth/me`, while the controller-level principal guard remains as defense in depth.
- Playbook and trading-memory ownership misses now use `ResourceNotFoundException`/existing ownership behavior so cross-user and nonexistent resources both return `404`.

### LOW

#### LOW-1: Legacy `/api/**` And Canonical `/api/v1/**` Paths Both Remain Active

- File: `backend/src/main/java/com/tradepilot/config/ApiVersioningFilter.java:30`
- Problem: `/api/v1/**` requests are rewritten to legacy `/api/**`; legacy paths also remain directly usable.
- Production impact: Larger route surface to monitor, though both paths enter the same controller/security rules.
- Recommended fix: Continue monitoring both path families in tests and logs; eventually retire legacy paths when clients have migrated.

## Authorization Controls Verified

- JWT auth required for non-public API routes: verified by code and tests.
- Admin-only routes: verified by code and tests.
- Cross-user account access: verified by code and tests.
- Cross-user trade read/update/delete: verified by code and tests.
- Cross-user import into account: verified by code and tests.
- Cross-user analytics/stats account filters: verified by code and tests.
- Cross-user session/coach/leak detector account access: verified by code and tests.
- Cross-user AI trade review/chart review by trade ID: verified by code and tests.
- Cross-user async AI status access: verified by code; existing status repositories scope by `userId`.
- Cross-user replay and trading-memory trade IDs: verified by code and tests.
- Cross-user playbook setup/trade/assignment IDs: verified by code and tests.
- Dev tooling disabled outside enabled dev modes: verified by code and tests.
- Production rejection of dev tooling/dev auth: verified by code and tests.
- `/api/auth/me` authentication at the HTTP filter layer: verified by code and tests.
- Cross-user playbook and trading-memory ownership failures return indistinguishable `404` responses: verified by code and tests.
- Billing customer/portal/session creation derives from principal: verified by code; Stripe-specific integrity audited separately.
- Public webhook transport is intentionally unauthenticated at HTTP layer: verified by code; Stripe signature validation audited separately.

## Tests Added Or Updated

- Added `SecurityProductionConfigValidatorTests`:
  - production rejects dev auth token endpoint
  - production rejects dev tools
  - production accepts dev features disabled
  - non-production allows dev features
- Updated `RbacSecurityTests`:
  - anonymous and invalid-JWT `/api/auth/me` requests return `401`
  - valid USER and ADMIN `/api/auth/me` requests return `200`
  - playbook setup/trade/assignment cross-user access is rejected
  - replay and trading-memory cross-user trade IDs are rejected

## Follow-Up Recommendations

- Keep the Stripe webhook security posture tied to Stripe signature validation, livemode enforcement, and event idempotency tests.
