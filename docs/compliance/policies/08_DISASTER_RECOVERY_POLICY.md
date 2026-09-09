# Disaster Recovery Policy

## Purpose

Define expectations for restoring TradePilot systems and data after major failures.

## Scope

Applies to Supabase/PostgreSQL, Render backend, Vercel frontend, Stripe billing dependencies, Qdrant vector memory, and supporting vendors.

## Roles and Responsibilities

- TradePilot Management approves recovery objectives and major recovery decisions.
- Production Administrators execute recovery steps.
- Service Owners validate recovered services.
- Application Developers assist with schema, application, and data integrity verification.

## Policy Statements

- Recovery procedures must prioritize data integrity over speed.
- Database recovery must preserve billing ledgers, Stripe event audit trails, and customer trading data.
- RTO and RPO are currently MANAGEMENT DECISION REQUIRED until approved.
- Restore tests must be performed before relying on recovery claims.
- Rollback should favor forward recovery for billing data and migrations.

## Procedures

- Use `docs/compliance/runbooks/DISASTER_RECOVERY_RUNBOOK.md`.
- Use `docs/compliance/evidence/templates/RESTORE_TEST_TEMPLATE.md` for restore tests.

## Evidence Retained

Restore test records, recovery logs, migration verification output, incident records, and management approvals.

## Review Frequency

At least annually and after significant database or hosting changes.

## Exceptions

Exceptions require management approval and documented risk acceptance.

## Enforcement

Critical recovery gaps must be tracked in the risk register.

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
