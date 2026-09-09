# Information Security Policy

## Purpose

Define TradePilot's baseline expectations for protecting customer data, production systems, source code, credentials, billing data, and operational evidence.

## Scope

This policy applies to TradePilot Management, System Administrators, Application Developers, Production Administrators, Service Owners, contractors, and any person with access to TradePilot systems or customer data.

## Roles and Responsibilities

- TradePilot Management owns the security program and may currently perform multiple operational roles due to the company's size.
- Production Administrators manage production access and deployment platforms.
- Application Developers design, implement, test, and maintain secure application changes.
- Service Owners maintain assigned third-party services and evidence.

## Policy Statements

- Customer trading data, journal content, AI analysis, billing metadata, credentials, and production configuration must be protected from unauthorized access, disclosure, alteration, or loss.
- Production access must be least privilege, approved, reviewed, and removed when no longer needed.
- Secrets must not be committed to source control or exposed in frontend bundles, logs, tickets, screenshots, or documentation.
- Security-relevant changes must follow documented change management and testing procedures.
- Security incidents must be reported, triaged, contained, investigated, and documented.
- Vendor services that process TradePilot data must be reviewed before and during use.

## Procedures

- Follow the Access Control, Change Management, Incident Response, Vendor Risk, Backup/Restore, Logging/Monitoring, Data Classification, and Retention/Deletion policies.
- Retain evidence using the templates in `docs/compliance/evidence/templates/`.
- Escalate policy exceptions to TradePilot Management before implementation unless emergency containment is required.

## Evidence Retained

Approved policy versions, access reviews, change records, incident records, vendor reviews, risk register updates, backup restore tests, security training records, and relevant CI/test evidence.

## Review Frequency

At least annually and after significant security, architectural, vendor, or regulatory changes.

## Exceptions

Exceptions require documented approval from TradePilot Management, a business justification, expiration date, and compensating controls.

## Enforcement

Violations may result in access removal, corrective action, incident response, or vendor/contractor review.

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
