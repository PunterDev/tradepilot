# Account Survival V1 Production Readiness

**Branch / audited commit:** `feature/v1-account-survival-coach` /
`853ba3e362d8fbd34885b27777127d2b2d7205f7`, plus the scoped
post-import entitlement correction documented below.
**Audit scope:** read-only repository, Render, Supabase, and Vercel review.
**Decision:** **NO-GO pending the release conditions in this record.**

This is an operational-readiness record, not authorization to merge or deploy.
It does not modify production data, run migrations, change feature access, or
activate security exceptions.

## Verified release evidence

| Control | Evidence | Result |
| --- | --- | --- |
| Full-stack acceptance | `scripts/run-account-survival-fullstack.sh` used React, real Spring Boot, a fresh PostgreSQL 16 container, Flyway V001--V031, actual authentication, and real CSV import APIs. It covers Free own-account 403, Pro own-account success, different Pro user's 404, and unauthenticated 401. | PASS locally, 1 test |
| Backend regression | Full Surefire reports after the Pro-only regression correction: 725 tests, 0 failures, 0 errors, 2 skipped | PASS locally |
| Fresh migration baseline | `mvn -Ppostgres-integration test` applied V001--V031 and initialized Hibernate validation | PASS locally |
| Frontend acceptance | Vitest: 12 passed; mocked Account Survival Playwright: 7 passed; real-backend Playwright: 1 passed; production build prerendered 25 routes | PASS locally |
| Hosted CI | Owner confirmed all seven required hosted jobs passed for `853ba3e` before the scoped correction | Owner-attested; rerun is required for the final candidate SHA |
| Production Supabase | `tradepilotcoach-prod` is `ACTIVE_HEALTHY`, PostgreSQL 17.6 | PASS, connector verified |
| Production Render | Service `tradepilot` is live, Docker runtime, branch `master`, root directory `backend` | PASS, connector verified |
| Production Vercel | Feature-branch preview for `1ac1ef6` is `READY`; production domains are verified | PASS, connector verified |

## Flyway V027--V031 review

| Migration | Production safety and existing-data effect | Locking / release result |
| --- | --- | --- |
| V027 `account_survival_review` | Creates two new Account Survival tables, foreign-keyed to existing `users` and `trading_accounts`. It does not alter, delete, or rewrite existing accounts, imports, trades, journals, or AI records. Cascades apply only if a parent user or account is later deleted. | Already successful in production. Creation occurred while tables were new. |
| V028 `next_session_commitment_evaluations` | Creates evaluation and rule-result tables plus indexes. It does not change existing trading data. | Already successful in production. |
| V029 `allow_overnight_protection_windows` | Drops only the prior same-day window check on the new commitment table, allowing an intentional overnight interval. It does not update rows. | Already successful in production. |
| V030 `enable_rls_on_account_survival_tables` | Enables RLS on the four Account Survival tables. It creates no policy and does not change row data. | Already successful in production. Direct Data API roles have no policy; server access depends on the JDBC role. |
| V031 `account_survival_evaluation_history` | Adds a partial unique index for one current evaluation per commitment/session and a supporting history index. It preserves prior versions and does not update or delete evaluation data. | **Pending in production.** The index statements are idempotent but not `CONCURRENTLY`; ordinary writes to the evaluation table can be briefly blocked while they build. Production currently reports zero rows in the two evaluation tables, so expected lock duration is short. Verify migration completion before smoke testing. |

The production Flyway history shows V027, V028, V029, and V030 as successful.
V031 is correctly absent before this branch is merged. The source migration chain and
the fresh PostgreSQL gate validate all migrations through V031.

## Production database, RLS, and ownership

Read-only catalog checks found RLS enabled on `account_risk_profiles`,
`next_session_protection_commitments`, `next_session_protection_evaluations`,
and `next_session_protection_rule_results`. They have no `pg_policies` entries.
The Supabase security advisor reports the same server-only RLS pattern across the
public schema. The Supabase catalog session used `postgres`, which has `BYPASSRLS`;
that is **not proof** of the Render JDBC runtime role.

Application ownership is independently enforced in the service layer: every
Account Survival operation receives the authenticated `User`, resolves the owned
account through `TradingAccountService`, and uses user-scoped repositories. The
real full-stack test proves a second user receives a 404 for another user's
Account Survival assessment. Commitment save/accept uses the owned account lock;
accepted commitments are immutable and repeated acceptance preserves the original
acceptance timestamp. PostgreSQL concurrency tests cover the lock and current
evaluation uniqueness.

Existing production tables remain present and nonempty where expected, including
trading accounts, import batches, trades, and journal entries. The Account
Survival tables exist and were empty at audit time. Nothing in V027--V031
modifies the existing-account, import, trade, or journal schemas.

### Required database release confirmations

1. Confirm the Render JDBC role used through the Supabase pooler can read and
   write the RLS-enabled tables after V031. Record the role name and whether it
   has `BYPASSRLS`; do not record credentials.
2. If it is a non-bypass role, create no policy during this release. Stop and
   investigate the server-only access design before enabling the feature.
3. Confirm an active Supabase backup/PITR plan, its retention period, and a
   documented restore owner. The connected read-only APIs do not expose backup
   retention or restore readiness, so this is **UNKNOWN** until dashboard or
   provider evidence is attached.
4. After deployment, verify Flyway V031 is successful and Hibernate starts with
   `ddl-auto=validate` before authenticated smoke testing.

## Render and application configuration

Render confirms a Docker web service on `master`, with automatic deploy on new
commits, `backend` as root directory, and no pull-request previews. The current
live deploy is `7a8c2442ff44779aeff06dcb682dcf96a8527b4f`; its startup log
confirms the `production` profile and `DEV TOOLS ENABLED: false`.

The source defaults are fail-closed for production: `DEV_AUTH_ENABLED=false`,
`DEV_TOOLS_ENABLED=false`, `V2_FEATURES_ENABLED=false`,
`V3_FEATURES_ENABLED=false`, `MARKET_DATA_PROVIDER=disabled`, and
`REPLAY_WORKER_ENABLED=false`. The `AccountSurvivalTimeSource` can be advanced
only when the `e2e` Spring profile is active; production uses the host clock.

The Render connector does not reveal environment-variable values or a complete
name-only inventory. Owner-attested production configuration must therefore be
reconfirmed before merge: active profile is `production`; dev authentication,
dev tools, V2/V3, and replay worker are disabled. This is a release condition,
not a claim derived from redacted values.

The current production startup log also warns that its Flyway version has not
tested PostgreSQL 17.6 support. Fresh PostgreSQL integration passed locally, but
the post-deploy startup log must be reviewed for Flyway validation/migration
errors.

## Vercel and deployment trigger review

Vercel project `tradepilot` uses Vite. The exact feature commit has a READY
preview deployment. Verified production domains are `www.tradepilotcoach.com`
and `tradepilotcoach.com`, with the apex redirecting to `www`. Production
deployments are created from `master`; the latest observed production frontend
revision is `c767b87e439f5bc95cfd8ff9f9bb58617b581163`. Preview deployments are
created for feature-branch commits.

Merging PR #1 into `master` will trigger:

1. Render automatic Docker deployment for its `master` branch, which runs the
   packaged Spring Boot application and Flyway at startup.
2. Vercel Git production deployment for `master`, which assigns the verified
   production domains after it is READY.

This conclusion is based on current connector configuration and observed commit
history. Do not assume both providers are on the same revision until their deploy
records show the merge SHA.

## Account Survival access-policy decision

**Owner decision recorded 2026-11-01: Account Survival Coach V1 is Pro-only.**
All nine Account Survival HTTP routes call the existing
`FeatureAccessService.requireFeature(user, Feature.SESSION_REVIEW)` before
reaching the user-scoped service layer:

| Caller / target | Expected and verified result |
| --- | --- |
| Unauthenticated caller | `401` from the application security chain |
| Free user / own account | `403 SUBSCRIPTION_REQUIRED`, `requiredPlan=PRO`, `feature=SESSION_REVIEW` |
| Pro user / own account | Authorized, subject to ordinary request validation and lifecycle rules |
| Different Pro user / another Pro user's account | Privacy-safe `404`, with no Account Survival evidence disclosed |

The backend ownership suite covers every read and mutation endpoint for Free
and cross-owner callers. The real-browser acceptance test independently covers
the same matrix with actual authentication, React, Spring Boot, PostgreSQL, and
the CSV import pipeline. The frontend uses the existing Session Review feature
gate, but the API boundary is authoritative.

On downgrade, historical commitments and evaluations remain preserved, while
all Account Survival HTTP access is denied. The after-commit import callback
now uses the same `FeatureAccessService.hasFeature` decision before evaluating
existing commitments. It returns without error for Free users, so CSV import
completion is not interrupted and no new Pro-only evaluation is created.
The PostgreSQL concurrency suite verifies this downgrade path.

## Authenticated production smoke checklist

Use controlled accounts and supported sample data. Do not use a production admin
account for normal-user ownership checks, do not expose tokens, and do not run a
payment transaction.

1. Log in as a controlled normal user and confirm the authenticated identity.
2. Select an owned trading account; verify no other account is listed.
3. Import a supported CSV and verify the existing import summary and journal
   continue to render correctly.
4. Confirm dashboard and journal behavior remains unchanged.
5. As a controlled Free user, confirm direct Account Survival API access returns
   structured `403 SUBSCRIPTION_REQUIRED`; then, as a controlled Pro user, open
   Session Review / Account Survival, save a valid risk profile, and verify
   assessment and evidence wording is deterministic and does not claim
   psychology, rule violations, or profitability.
6. Save then accept a protection commitment. Retry acceptance and confirm its
   timestamp is preserved; confirm editing is locked.
7. Import a later eligible session and verify the evaluation/history appears.
   A later recovery must not hide the worst cumulative realized session loss.
8. Confirm missing contemporaneous evidence is `NOT_EVALUABLE`, never counted
   as followed.
9. With a different controlled Pro user, request the first account's Account
   Survival endpoints and require 404/no data disclosure. Confirm an
   unauthenticated request returns 401.
10. Verify normal billing plan UI, upgrade CTA, credit-pack UI, and existing
    import/journal features without completing payment.
11. Review Render logs, health endpoint, Flyway V031 state, and Vercel/Render
    SHA provenance before declaring the release complete.

## Deployment and rollback procedure

1. Confirm PR checks, this document's blockers, backup/PITR evidence, runtime
   role, and the Account Survival access-policy decision.
2. Merge the reviewed SHA to `master`; do not manually apply Flyway SQL.
3. Wait for the single Render auto-deploy and confirm it uses the merge SHA.
   Require Flyway V031 success, Hibernate validation, and health `UP`.
4. Wait for Vercel production deployment on the same merge SHA and perform the
   smoke checklist above.
5. If application behavior fails, roll application code back using the
   provider's prior verified deployment or a normal forward code commit. Do not
   delete `next_session_protection_*` rows, do not rewrite Flyway history, and
   do not reverse V027--V031. Use a forward migration for a necessary data fix.

## Security status

The approved Dependency-Check configuration remains unchanged: CVSS 8 gate,
unrelated-vulnerability negative control, and exact approved temporary
exceptions remain in force. The documented exception expiry is
`2026-11-30T00:00:00Z`; it must not be extended by this release. Gitleaks,
Semgrep, frontend audit, and the hosted security checks remain mandatory.

## Pre-merge gates

**NO-GO** until all of the following are recorded for the final merge-candidate
SHA:

1. The seven hosted CI jobs are green for that exact SHA, including the CVSS 8
   negative control and the approved exception expiry check.
2. Supabase backup/PITR retention and a named restore owner are confirmed from
   provider/dashboard evidence. Connected read-only tooling does not expose
   those settings.
3. An authorized operator verifies the Render JDBC role over the actual runtime
   connection: record role name, `current_user`, `session_user`, `BYPASSRLS`,
   and table privileges for the four RLS-enabled Account Survival tables. The
   Supabase management session is `postgres` with `BYPASSRLS`; it is not proof
   of the runtime pooler role.
4. Owner-attested production values are reconfirmed without recording values:
   active profile `production`; dev auth and dev tools disabled; V2/V3 disabled;
   replay worker disabled; market-data provider disabled. Source defaults are
   fail-closed, and startup validation rejects enabled dev auth/tools in a
   production profile, but environment overrides remain runtime evidence.

## Post-merge deployment gates

After owner-approved merge, do not declare rollout successful until:

1. Render deploys the merge SHA and Flyway V031 succeeds. V031 creates two
   idempotent indexes, preserves all historical evaluation versions, and does
   not alter existing trading data; because the indexes are not `CONCURRENTLY`,
   brief evaluation-table write blocking is possible while they build.
2. Hibernate starts with `ddl-auto=validate` and the backend health endpoint is
   `UP`.
3. Vercel's production deployment is READY on the same merge SHA. Feature-branch
   deployments are previews only; current production provenance must not be
   inferred from the preview.
4. The controlled authenticated smoke checklist above passes, including the
   Free/Pro/unauthenticated/ownership matrix.

The approved OWASP exception set remains unchanged, with CVSS 8 enforcement and
an expiry of `2026-11-30T00:00:00Z`.
