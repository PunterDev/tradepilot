# Production Access Policy

## Purpose

Define safeguards for direct access to production systems and production customer data.

## Scope

Applies to production access for Render, Vercel, Supabase, Stripe, OpenAI, Qdrant, Databento, Resend, GitHub production branches/actions/secrets, domain/DNS providers, and TradePilot ADMIN accounts.

## Roles and Responsibilities

- TradePilot Management approves production access and high-risk operations.
- Production Administrators manage production credentials, deployments, and access changes.
- Application Developers request temporary or least-privilege access when needed.
- Service Owners verify vendor-specific production access.

## Policy Statements

- Production access must be limited to authorized personnel with current business need.
- MFA must be enabled wherever supported.
- Production data must not be copied to local/dev environments without approval and minimization.
- Direct production database writes require documented approval, a rollback/recovery plan, and evidence.
- Billing and entitlement changes require reconciliation with Stripe and audit trail preservation.
- Production credentials must be stored only in approved provider secret stores.

## Procedures

- Use access review, change management, incident response, and Stripe/database rollout runbooks as applicable.
- Record production access in `docs/compliance/registers/PRODUCTION_ACCESS_REGISTER.md`.

## Evidence Retained

Access approvals, access review evidence, deployment logs, migration logs, direct-change approvals, reconciliation records, and incident records.

## Review Frequency

Quarterly for access; annually for policy.

## Exceptions

Emergency access requires post-event review and documented removal or approval.

## Enforcement

Unapproved production access must be removed and reviewed as a security exception.

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
