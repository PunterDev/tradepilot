# Secure SDLC Policy

## Purpose

Define secure software development expectations for TradePilot changes.

## Scope

Applies to application code, frontend code, backend code, database migrations, CI/CD, infrastructure-as-configuration, and production configuration changes.

## Roles and Responsibilities

- Application Developers implement secure changes and tests.
- Production Administrators deploy approved changes.
- TradePilot Management approves high-risk changes and exceptions.
- Service Owners review security impact for vendor configuration changes.

## Policy Statements

- Security, authorization, billing, and data-processing changes require appropriate tests and review.
- Database schema changes for production must be versioned through Flyway.
- Pull requests should document risk, tests, and verification for meaningful changes.
- Secrets must never be committed or included in frontend bundles.
- Security defects must be prioritized according to risk.

## Procedures

- Follow `docs/compliance/procedures/CHANGE_MANAGEMENT_PROCEDURE.md`.
- Use CI evidence, test reports, migration verification, and PR review records as audit evidence.

## Evidence Retained

PRs, code review approvals, CI results, test output, migration logs, deployment records, and rollback evidence.

## Review Frequency

At least annually and after major development workflow changes.

## Exceptions

Emergency fixes may be expedited but must be documented after deployment.

## Enforcement

Production changes that bypass review/testing must be documented as exceptions.

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
