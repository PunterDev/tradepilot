# Backup Restore Procedure

## Purpose

Define backup and restore verification for TradePilot production data.

## Scope

Primary production systems requiring backup or recovery review: Supabase/PostgreSQL, compliance evidence repository, production configuration evidence, and any future storage containing customer data. Vendor-managed systems such as Stripe, OpenAI, Qdrant, Databento, Resend, Render, Vercel, and GitHub require vendor recovery review.

## Procedure

1. Confirm production backup settings for each system.
2. Record backup frequency, retention, and owner.
3. If RTO/RPO are not approved, mark them MANAGEMENT DECISION REQUIRED.
4. Perform restore tests only to approved non-production targets.
5. Verify restored database schema with Flyway history and `backend/db/verify_production_schema.sql` where applicable.
6. Verify representative records and data integrity.
7. Confirm application startup/Hibernate validation where practical.
8. Record results in `docs/compliance/evidence/templates/RESTORE_TEST_TEMPLATE.md`.
9. Track failures or unmet objectives in the risk register.

## Evidence

Backup setting screenshots, restore logs, schema verification output, integrity checks, application startup evidence, and corrective actions.

## Current Known State

Repository documentation says backups must be enabled before beta/production, but actual Supabase/Render/Vercel backup settings are not verifiable from the repository. No restore test evidence is created by this procedure.
