# Backup Restore Policy

## Purpose

Define backup and restore expectations for TradePilot production data and evidence.

## Scope

Applies to production PostgreSQL/Supabase data, application configuration evidence, compliance records, and any other system that stores customer or operational data.

## Roles and Responsibilities

- TradePilot Management approves RTO/RPO and retention expectations.
- Production Administrators verify backup settings and perform restore tests.
- Application Developers validate application schema and data integrity after restore.
- Service Owners retain backup evidence for systems they manage.

## Policy Statements

- Production database backups must be enabled before accepting production customer data.
- Restore capability must be tested to a non-production environment before relying on backup controls.
- RTO and RPO are MANAGEMENT DECISION REQUIRED until formally approved.
- Backup records must not include secrets unless stored in an approved secret manager.
- Restore tests must verify data integrity and application startup/schema validation where practical.

## Procedures

- Follow `docs/compliance/procedures/BACKUP_RESTORE_PROCEDURE.md`.
- Record restore tests with `docs/compliance/evidence/templates/RESTORE_TEST_TEMPLATE.md`.

## Evidence Retained

Backup configuration screenshots, restore test records, verification query output, startup validation logs, and issue/corrective action records.

## Review Frequency

Backup settings quarterly; restore testing at least annually and before Type I readiness review.

## Exceptions

Exceptions require management approval and documented compensating controls.

## Enforcement

Production launch should not proceed without verified backup and restore evidence.

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
