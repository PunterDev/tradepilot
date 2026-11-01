# Account Survival V1 Release Acceptance

## Scope and decision

This record covers the Account Survival Coach V1 branch. It is a deterministic
review of imported, realized trade data. It does not assess trader psychology,
prove a rule violation, predict profitability, or provide a unified
cross-account compliance score.

**Decision:** GO for PR approval only when the checks below and the hosted CI
for the final commit are green. This is not production deployment approval.

## Acceptance matrix

| Criterion | Evidence | Status |
| --- | --- | --- |
| Empty PostgreSQL schema migrates through V031 and Hibernate validates | `FlywayPostgresBaselineIntegrationTest`; fresh PostgreSQL acceptance harness | Verified |
| Profile, assessment, and cross-account evidence use real API/persistence | `account-survival-real-backend.spec.ts` | Verified |
| Supported CSV import reaches the standard import service | Real browser harness imports Tradovate-shaped CSV and asserts `savedCount=2` | Verified |
| Draft can be saved; accepted commitment is immutable | ownership integration plus real browser flow | Verified |
| Duplicate acceptance preserves the original acceptance state/time | lifecycle and PostgreSQL concurrency tests | Verified |
| Import of a later eligible session creates a durable evaluation | real browser harness; post-import evaluation uses a new transaction | Verified |
| Worst cumulative realized loss remains the observed loss after later recovery | real CSV sequence `-200`, then `+100`; UI asserts observed `-$200.00` | Verified |
| Missing contemporaneous evidence is NOT_EVALUABLE, not FOLLOWED | evaluator tests and real browser flow | Verified |
| History keeps corrected-import versions and one current version | PostgreSQL concurrency/history tests and V031 partial unique constraint | Verified |
| Ownership is account-owner scoped | `AccountSurvivalOwnershipIntegrationTest` and real browser non-owner 404 | Verified |
| Cross-account continuation remains observational | real browser evidence checks show scoped source/later accounts and limitations | Verified |
| Insufficient data is rendered without fabricated measurements | real browser later-account check and UI suite | Verified |
| Structured API error UI remains usable | `account-survival-review.spec.ts` | Verified (mocked UI contract) |
| Layout at 375, 390, 768, and 1440 px | real browser overflow checks plus mocked UI layout suite | Verified |

## Test boundary

`frontend/e2e/account-survival-review.spec.ts` intercepts `**/api/**`. It is
retained for focused UI error, empty-state, and responsive-contract coverage;
it is not full-stack evidence.

`frontend/e2e/account-survival-real-backend.spec.ts` is the full-stack gate. It
starts React against a local Spring Boot `e2e` profile and an empty PostgreSQL
16 container, applies Flyway V001--V031, and uses the real authentication,
CSV-import, Account Survival, ownership, and persistence APIs. Its profile-only
clock lets the test accept on 2026-10-01, import/evaluate 2026-10-02, and never
changes production time behavior.

## Commands and results

The final verification run records its exact command output in CI. Local gates
for this acceptance update include:

```text
bash scripts/run-account-survival-fullstack.sh                         PASS
mvn -Dtest=AccountSurvivalTimeSourceTests,AccountSurvivalOwnershipIntegrationTest,AccountSurvivalEvaluationPostgresConcurrencyIntegrationTest test  PASS (15 tests)
npm run test:unit -- --run src/components/AccountSurvivalReview.test.tsx  PASS (12 tests)
npx playwright test e2e/account-survival-review.spec.ts --workers=1       PASS (7 tests)
npm run build                                                           PASS (25 public routes)
git diff --check                                                        PASS
```

## Security and deployment

The approved Dependency-Check exception set remains governed by
`docs/security/ACCOUNT_SURVIVAL_V1_DEPENDENCY_CHECK_RISK_ACCEPTANCE.md`.
Its exact-scope exceptions expire at `2026-11-30T00:00:00Z`; any dependency or
relevant configuration change requires renewed review. This document neither
changes nor activates security exceptions.

Deploy only after Flyway V027--V031 have been applied successfully and the
production PostgreSQL runtime role's RLS behavior has been checked. Rollback is
code/configuration rollback only: do not remove applied migrations or delete
commitment/evaluation history. Use a forward migration for any data correction.

## Remaining limitations

- Completed-trade imports cannot prove contemporaneous setup confirmation, so
  that rule is deliberately `NOT_EVALUABLE`.
- Cross-account continuation is evidence scoped to the authenticated owner's
  imported accounts; it is not global enforcement.
- The local e2e clock is available only under the test-only `e2e` profile and
  cannot be advanced in production profiles.
- This feature is not investment, compliance, or profitability guidance.
