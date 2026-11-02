# Account Survival V1 Production Readiness

**Branch / audited commit:** `feature/v1-account-survival-coach` /
`1ac1ef61943a2b2185025bc17b0291c9e4cd352d`
**Audit scope:** read-only repository, Render, Supabase, and Vercel review.
**Decision:** **NO-GO pending the release conditions in this record.**

This is an operational-readiness record, not authorization to merge or deploy.
It does not modify production data, run migrations, change feature access, or
activate security exceptions.

## Verified release evidence

| Control | Evidence | Result |
| --- | --- | --- |
| Full-stack acceptance | `scripts/run-account-survival-fullstack.sh` used React, real Spring Boot, a fresh PostgreSQL 16 container, Flyway V001--V031, and real CSV import APIs | PASS locally |
| Backend regression | Full Surefire reports at the audited commit: 723 tests, 0 failures, 0 errors, 2 skipped | PASS locally |
| Fresh migration baseline | `mvn -Ppostgres-integration test` applied V001--V031 and initialized Hibernate validation | PASS locally |
| Frontend acceptance | Vitest: 12 passed; mocked Account Survival Playwright: 7 passed; production build prerendered 25 routes | PASS locally |
| Hosted CI | Owner confirmed all seven required hosted jobs passed for `1ac1ef6` | Owner-attested; retain run links on PR #1 |
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

Backend ownership is enforced. However, the Account Survival controller requires
authentication through the application security chain and does **not** call
`FeatureAccessService.requireFeature(Feature.SESSION_REVIEW)`. The Session Review
service and navigation apply the Session Review plan gate, but a direct,
authenticated Account Survival API request is not independently plan-gated.

**Owner decision recorded 2026-11-01: Account Survival Coach V1 is Pro-only.**
All nine Account Survival HTTP routes now call the existing
`FeatureAccessService.requireFeature(user, Feature.SESSION_REVIEW)`, matching
Session Review's established plan policy. Free users receive structured HTTP
403 `SUBSCRIPTION_REQUIRED` with `requiredPlan=PRO`; Pro users continue
to require account ownership and receive privacy-safe 404 responses for
other users' accounts. The ownership integration suite includes a Free-tier
all-routes rejection matrix, and the real-backend Playwright acceptance journey
checks Free-tier denial and Pro-tier access.

**Implementation is committed; hosted verification and production release
prerequisites remain separate gates.**

## Authenticated production smoke checklist

Use controlled accounts and supported sample data. Do not use a production admin
account for normal-user ownership checks, do not expose tokens, and do not run a
payment transaction.

1. Log in as a controlled normal user and confirm the authenticated identity.
2. Select an owned trading account; verify no other account is listed.
3. Import a supported CSV and verify the existing import summary and journal
   continue to render correctly.
4. Confirm dashboard and journal behavior remains unchanged.
5. Open Session Review / Account Survival, save a valid risk profile, and verify
   assessment and evidence wording is deterministic and does not claim
   psychology, rule violations, or profitability.
6. Save then accept a protection commitment. Retry acceptance and confirm its
   timestamp is preserved; confirm editing is locked.
7. Import a later eligible session and verify the evaluation/history appears.
   A later recovery must not hide the worst cumulative realized session loss.
8. Confirm missing contemporaneous evidence is `NOT_EVALUABLE`, never counted
   as followed.
9. With a different normal user, request the first account's Account Survival
   endpoints and require 404/no data disclosure.
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

## Release decision and blockers

**NO-GO** until all of the following are recorded for the merge SHA:

1. Pro-only access policy is approved and implemented; require final hosted
   CI and real-backend acceptance confirmation for the guard.
2. Supabase backup/PITR retention and restore-owner evidence.
3. Render JDBC runtime-role/RLS compatibility evidence and reconfirmed
   production fail-closed feature/development flags.
4. Successful Render Flyway V031/Hibernate startup and Vercel production
   deployment provenance.
5. Completed controlled authenticated smoke checklist with ownership isolation.

When these are complete and the seven hosted CI jobs remain green for the final
merge candidate, the recommendation becomes **GO FOR OWNER-APPROVED RELEASE**.
