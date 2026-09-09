# Access Control Policy

## Purpose

Ensure TradePilot systems and customer data are accessed only by authorized users with a documented business need.

## Scope

Applies to GitHub, Vercel, Render, Supabase, Stripe, OpenAI, Qdrant, Databento, Resend, domain/DNS providers, TradePilot ADMIN accounts, and any other production system.

## Roles and Responsibilities

- TradePilot Management approves privileged access and exceptions.
- Production Administrators provision, modify, and remove access.
- Service Owners review access for their assigned services.
- Application Developers request access only when needed for assigned work.

## Policy Statements

- Access must follow least privilege.
- Production access must have documented approval before provisioning.
- MFA must be enabled for all production systems where supported.
- Shared accounts must be avoided unless a provider does not support named users; compensating evidence is required.
- Access must be removed promptly when no longer required.
- Access must be reviewed at least quarterly.
- TradePilot ADMIN accounts must be granted only for operational need and reviewed with other production access.

## Procedures

- Use `docs/compliance/procedures/ACCESS_REVIEW_PROCEDURE.md` for quarterly reviews.
- Record authorized access in `docs/compliance/registers/PRODUCTION_ACCESS_REGISTER.md`.
- Retain review evidence in `docs/compliance/evidence/templates/ACCESS_REVIEW_TEMPLATE.md`.

## Evidence Retained

Access approvals, user/role exports, MFA evidence, quarterly review signoffs, access removal records, and exception approvals.

## Review Frequency

Quarterly for access reviews; annually for the policy.

## Exceptions

Exceptions require management approval, expiration, and compensating controls.

## Enforcement

Unauthorized or unnecessary access must be removed. Repeated or intentional violations may result in loss of access.

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
