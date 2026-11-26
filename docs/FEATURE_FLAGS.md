# TradePilot Feature Flags

TradePilot uses a global V2 release flag to hide selected completed features during the V1 launch without deleting code, data, APIs, or tests.

## Current Flags

| Layer | Variable | Default | Purpose |
| --- | --- | --- | --- |
| Frontend | `VITE_V2_FEATURES_ENABLED` | `false` | Hides V2 navigation, routes, and frontend entry points at build time. |
| Backend | `V2_FEATURES_ENABLED` | `false` | Blocks V2 API endpoints before service execution, telemetry, cache reads, AI calls, or credit deduction. |

The Spring property is:

```yaml
tradepilot:
  features:
    v2-enabled: ${V2_FEATURES_ENABLED:false}
```

## Features Controlled

When V2 is disabled, these features are hidden and inaccessible:

- Playbook
- Trading DNA
- Journey / AI Performance Timeline
- Trading Memory

Coach is a V1 feature. Daily Coach and Weekly Coach remain visible and continue using their existing plan, cache, and credit rules.

## Frontend Behavior

The frontend flag is compiled into the Vite bundle. Changing `VITE_V2_FEATURES_ENABLED` requires a frontend rebuild and redeploy.

Accepted true values:

- `true`
- `1`
- `yes`
- `on`

Accepted false values:

- `false`
- `0`
- `no`
- `off`
- blank or missing

Unknown values fail closed and disable V2 features.

## Backend Behavior

Changing `V2_FEATURES_ENABLED` requires a backend restart or redeploy.

When disabled, protected V2 APIs return `404` with `FEATURE_DISABLED`. This makes V1 behave as though unreleased product surfaces do not exist yet and avoids confusing users with subscription errors.

The public release-availability endpoint is available at:

- `GET /api/features`
- `GET /api/v1/features`

Example response:

```json
{
  "v2FeaturesEnabled": false,
  "features": {
    "playbook": false,
    "tradingDna": false,
    "journey": false,
    "tradingMemory": false,
    "coach": true
  }
}
```

This endpoint exposes release availability only. It does not expose secrets and does not perform user-specific entitlement checks.

## Local Development

V1-style local run:

```bash
V2_FEATURES_ENABLED=false
VITE_V2_FEATURES_ENABLED=false
```

V2 local run:

```bash
V2_FEATURES_ENABLED=true
VITE_V2_FEATURES_ENABLED=true
```

Backend tests run with V2 enabled in `application-test.yml` so existing Playbook, Trading DNA, and Journey behavior remains covered.

## V1 Launch

1. Set `V2_FEATURES_ENABLED=false` in backend hosting.
2. Set `VITE_V2_FEATURES_ENABLED=false` in frontend hosting.
3. Deploy/restart backend.
4. Rebuild and deploy frontend.
5. Verify `GET /api/v1/features` reports `playbook=false`, `tradingDna=false`, `journey=false`, `tradingMemory=false`, and `coach=true`.
6. Verify sidebar, mobile nav, and trade review surfaces do not show Playbook, Trading DNA, Journey, or Trading Memory.
7. Verify direct routes redirect to Dashboard.
8. Verify direct API calls return `404 FEATURE_DISABLED`.
9. Verify Coach still works.

## V2 Release

1. Set `V2_FEATURES_ENABLED=true`.
2. Set `VITE_V2_FEATURES_ENABLED=true`.
3. Restart/redeploy backend.
4. Rebuild/redeploy frontend.
5. Verify `GET /api/v1/features` reports V2 features enabled.
6. Smoke test Playbook, Trading DNA, Journey, Trading Memory, and Coach.

Existing V2 data, cache rows, snapshots, feature usage history, and database tables remain intact. No migration is required merely to expose V2 again.

## Emergency Rollback

1. Set both flags back to `false`.
2. Restart/redeploy backend.
3. Rebuild/redeploy frontend.
4. Confirm V2 nav/routes/APIs are inaccessible.
5. Do not delete V2 data.

## Keeping Flags Synchronized

Keep frontend and backend values aligned:

- Backend `false` + frontend `true`: users may see V2 links, but APIs return `FEATURE_DISABLED`.
- Backend `true` + frontend `false`: V2 APIs work, but users cannot navigate to the UI.

Use `GET /api/v1/features` during deployment smoke tests to verify backend state.

## Adding Future V2 Features

To add another feature to the global V2 group:

1. Add the frontend feature key to `frontend/src/config/featureFlags.ts`.
2. Add the backend feature to `FeatureFlagService`.
3. Filter navigation and cross-links through the frontend helper.
4. Guard backend controllers before telemetry, cache, credit, or AI work.
5. Add tests for disabled and enabled behavior.

To split V2 into individual flags later, keep the existing feature-key abstraction and map each key to its own environment-backed property.

## Account Survival controlled rollout

Account Survival uses the independent backend-only variable `ACCOUNT_SURVIVAL_ROLLOUT_MODE`. Its default is `disabled`; no frontend value can enable access.

| Value | Premium mutation policy |
| --- | --- |
| `disabled` | No user can create or accept a commitment or create an evaluation. |
| `beta` | Only Pro users with an enabled `ACCOUNT_SURVIVAL_BETA` entitlement can perform premium mutations. |
| `general-availability` | Pro users can perform premium mutations. |

Values are case-insensitive and may use hyphens or underscores. Blank, missing, or unknown values fail closed as `disabled`. Beta inclusion does not override the Pro plan, account ownership, authentication, or commitment lifecycle checks. Administrator role and AI credit balance do not grant access.

Overview, risk-profile, and owner-only commitment/evaluation reads remain available when premium actions are unavailable. A downgrade, expiration, beta removal, or rollout disablement leaves an accepted active commitment and every historical evaluation unchanged and readable by its owner. It does not complete, fail, cancel, or reevaluate the commitment. Automatic post-import evaluation pauses until both rollout and Pro access are restored; a later authorized evaluation resumes from the preserved commitment and append-only history.

Post-import evaluation begins only after the trade-import transaction commits. The callback carries user and account identifiers, opens a new transaction, reloads the current user, and rechecks ownership, rollout eligibility, and subscription entitlement before evaluating. A subscription change committed before that callback begins therefore governs the callback; it does not revoke an evaluation that was already authorized and running in an earlier transaction.

Denied premium requests return the existing stable conventions:

- `404 FEATURE_DISABLED` when rollout is unavailable.
- `403 SUBSCRIPTION_REQUIRED` when rollout is available but Pro is required.
- privacy-safe `404` when the authenticated user does not own the account; ownership is checked first.

Deterministic Account Survival evaluation does not call an LLM or consume monthly or purchased AI credits. Changing the rollout mode requires a backend restart or redeploy in normal environments. Production activation is an operational decision outside code deployment.
