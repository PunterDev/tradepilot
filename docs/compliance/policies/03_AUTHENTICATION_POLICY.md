# Authentication Policy

## Purpose

Define authentication requirements for TradePilot application and production-system access.

## Scope

Applies to customer accounts, TradePilot ADMIN accounts, source code and deployment platforms, vendor dashboards, and production infrastructure.

## Roles and Responsibilities

- TradePilot Management approves authentication requirements and exceptions.
- Production Administrators verify MFA and secret storage settings.
- Application Developers maintain authentication code and tests.
- Users are responsible for keeping credentials secure.

## Policy Statements

- Passwords must be stored only as strong salted hashes in the application.
- Password reset and email verification tokens must be single-use, time-limited, and not stored raw.
- JWT and session secrets must be strong, environment-specific, and stored only in approved secret managers or deployment-provider secret stores.
- Production system accounts must use MFA where supported.
- ADMIN access must require MFA or an equivalent MFA-enforced access layer before public production launch.
- Authentication failures and suspicious login behavior must be monitored.

## Procedures

- Application authentication controls are implemented in backend security services and covered by automated tests.
- Production system MFA evidence must be collected during access reviews.
- Secret rotation must be documented when credentials are exposed, compromised, or personnel with access leave.

## Evidence Retained

Authentication tests, JWT validation tests, MFA screenshots/exports, access review records, and secret rotation records.

## Review Frequency

At least annually and after authentication architecture changes.

## Exceptions

Any exception to MFA or secret strength requirements must be documented and approved by TradePilot Management.

## Enforcement

Accounts that do not meet requirements may be disabled or have privileges reduced until remediated.

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
