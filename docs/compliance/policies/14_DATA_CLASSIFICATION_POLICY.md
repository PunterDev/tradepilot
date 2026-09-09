# Data Classification Policy

## Purpose

Classify TradePilot data so it receives protection appropriate to sensitivity.

## Scope

Applies to authentication data, profile data, account configuration, trading data, journal content, AI analysis, trading memories, replay metadata, billing metadata, usage analytics, application logs, and security logs.

## Roles and Responsibilities

- TradePilot Management approves classifications and exceptions.
- Service Owners maintain data inventory entries for systems they manage.
- Application Developers implement protections aligned to classification.
- Production Administrators restrict access to classified data stores.

## Policy Statements

- Customer trading history, journal content, AI analysis, trading memories, billing metadata, credentials, and security logs are confidential or restricted data.
- Public marketing content is public data.
- Data classifications must be recorded in `docs/compliance/registers/DATA_INVENTORY.md`.
- Data must be shared with third parties only for documented product, billing, support, security, or operational purposes.
- Unknown retention, deletion, encryption, or vendor status must be marked UNKNOWN or REVIEW REQUIRED.

## Procedures

- Update the data inventory when new data types, vendors, or storage locations are introduced.
- Review AI and vector-memory data flows before adding new AI features.

## Evidence Retained

Data inventory, vendor data-flow review, access review, retention/deletion evidence, and policy approvals.

## Review Frequency

At least annually and after significant data-processing changes.

## Exceptions

Exceptions require management approval and documented compensating controls.

## Enforcement

Misclassified or undocumented sensitive data must be corrected and tracked.

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
