# Data Retention Deletion Policy

## Purpose

Define how TradePilot retains and deletes customer, operational, security, and billing data.

## Scope

Applies to PostgreSQL/Supabase records, Qdrant trading memories, application logs, observability records, billing ledgers, Stripe metadata, email records, and compliance evidence.

## Roles and Responsibilities

- TradePilot Management approves retention periods and legal/business exceptions.
- Production Administrators execute production deletion or restoration tasks.
- Application Developers maintain deletion mechanisms and verification queries.
- Service Owners verify deletion or retention behavior for vendors.

## Policy Statements

- Retention periods are MANAGEMENT DECISION REQUIRED until approved.
- Deletion requests must be documented, authorized, and verified.
- Billing, tax, security, and legal records may require retention exceptions.
- TradePilot must not fabricate missing deletion evidence.
- Deletion of customer data must consider PostgreSQL records, Qdrant vectors, Stripe/legal retention, email/contact records, logs, and backups.

## Procedures

- Use documented cleanup/deletion scripts only after review and approval.
- Verify deletion across storage systems and record evidence.
- Mark unverifiable legacy records for manual review.

## Evidence Retained

Retention schedule, deletion request records, approval, verification output, vendor deletion confirmations, and exception approvals.

## Review Frequency

At least annually and after material data model changes.

## Exceptions

Retention exceptions require legal/business justification and management approval.

## Enforcement

Undocumented deletion or retention exceptions must be tracked as compliance risks.

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
