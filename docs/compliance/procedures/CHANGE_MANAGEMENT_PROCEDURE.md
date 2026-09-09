# Change Management Procedure

## Purpose

Define TradePilot's lightweight production change workflow.

## Scope

Application code, frontend code, backend code, database migrations, CI/CD, production configuration, vendor settings, and billing configuration.

## Procedure

1. Document the issue, requirement, or operational reason for the change.
2. Classify risk level: low, medium, high, or emergency.
3. Create a development branch for code changes.
4. Implement the change with scoped edits.
5. Run relevant automated tests.
6. Open a pull request for review when using GitHub flow.
7. Verify CI results. Backend production database changes must pass the PostgreSQL Flyway baseline gate when applicable.
8. Obtain reviewer/management approval for high-risk or production-impacting changes.
9. Deploy through the approved platform.
10. Perform production verification.
11. Execute rollback or forward recovery if verification fails.
12. Record evidence using `docs/compliance/evidence/templates/CHANGE_RECORD_TEMPLATE.md` for sampled and high-risk changes.

## Repository Evidence Required

Branch protection, required reviewer settings, deployment permissions, and production approval rules are not fully visible in source code. Repository configuration evidence is required.

## Evidence

Issue/requirement, PR, review, CI reports, deployment logs, migration logs, verification notes, and rollback evidence.
