# Change Management Policy

## Purpose

Ensure changes to TradePilot application code, infrastructure configuration, database schema, and production billing settings are authorized, tested, reviewed, and recoverable.

## Scope

Applies to backend, frontend, database migrations, CI/CD, Render, Vercel, Supabase, Stripe, and production configuration changes.

## Roles and Responsibilities

- TradePilot Management approves high-risk production changes and exceptions.
- Application Developers implement and test changes.
- Production Administrators deploy and verify production changes.
- Service Owners approve vendor-dashboard changes within their scope.

## Policy Statements

- Changes must have a documented reason, risk level, tests, and rollback plan appropriate to impact.
- Production changes should be made through version-controlled code and CI/CD where practical.
- Database changes must use Flyway for new production schema management.
- Billing and Stripe configuration changes require extra review and reconciliation evidence.
- Emergency changes must be documented after containment.

## Procedures

- Follow `docs/compliance/procedures/CHANGE_MANAGEMENT_PROCEDURE.md`.
- Use `docs/compliance/evidence/templates/CHANGE_RECORD_TEMPLATE.md` for high-risk changes and sampled audit evidence.

## Evidence Retained

Issues/requirements, PR links, review approvals, CI results, deployment logs, migration logs, verification notes, and rollback records.

## Review Frequency

At least annually and after major deployment process changes.

## Exceptions

Emergency exceptions require post-change documentation and management review.

## Enforcement

Unapproved production changes may be reverted and reviewed as control exceptions.

## Document Control

Owner:
TradePilot Management

Approved By:
Pending Management Approval

Effective Date:
Pending

Last Review:
Pending

Next Review:
Pending

Version:
1.0
