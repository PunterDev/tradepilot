# Business Continuity Policy

## Purpose

Define TradePilot's approach to sustaining essential services during significant disruptions.

## Scope

Applies to production application hosting, database availability, billing, email delivery, AI features, vector memory, market data, source control, and customer support operations.

## Roles and Responsibilities

- TradePilot Management defines continuity priorities and acceptable downtime/data-loss targets.
- Service Owners document vendor dependencies and fail-safe behavior.
- Production Administrators coordinate recovery for hosting and infrastructure.

## Policy Statements

- Essential services and vendor dependencies must be documented.
- RTO and RPO targets require management decision and approval.
- Continuity planning must account for Supabase, Render, Vercel, Stripe, OpenAI, Qdrant, Databento, Resend, GitHub, and DNS/provider outages.
- TradePilot must not claim automatic failover unless it is implemented and tested.

## Procedures

- Use `docs/compliance/runbooks/DISASTER_RECOVERY_RUNBOOK.md` for vendor/system failure scenarios.
- Review continuity risk during annual risk assessment and significant architecture changes.

## Evidence Retained

Approved RTO/RPO, vendor dependency register, DR tabletop records, incident records, and recovery evidence.

## Review Frequency

At least annually and after significant vendor or architecture changes.

## Exceptions

Exceptions require management approval and documented customer impact assessment.

## Enforcement

Unreviewed critical dependencies must be added to the risk register.

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
