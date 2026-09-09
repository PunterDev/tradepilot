# Vendor Risk Management Policy

## Purpose

Define how TradePilot evaluates and monitors third-party vendors that support production services or process customer data.

## Scope

Applies to GitHub, Vercel, Render, Supabase, Stripe, OpenAI, Qdrant, Databento, Resend, domain/DNS providers, and future vendors.

## Roles and Responsibilities

- TradePilot Management approves use of critical vendors and risk acceptance.
- Service Owners perform vendor reviews and update the vendor register.
- Production Administrators verify vendor access and MFA settings.

## Policy Statements

- Vendors must be recorded in `docs/compliance/registers/VENDOR_REGISTER.md`.
- Critical vendors must be reviewed before production use and at least annually.
- Vendor reviews must consider data shared, criticality, security documentation, SOC 2/security reports, DPA needs, breach history, subprocessors, data location, encryption, MFA, incident commitments, and exit considerations.
- Unknown vendor security status must be marked REVIEW REQUIRED, not assumed safe.

## Procedures

- Follow `docs/compliance/procedures/VENDOR_REVIEW_PROCEDURE.md`.
- Use `docs/compliance/evidence/templates/VENDOR_REVIEW_TEMPLATE.md`.

## Evidence Retained

Vendor register, completed vendor review templates, SOC/security reports where available, DPA status, access review evidence, and risk acceptance decisions.

## Review Frequency

At onboarding, annually, and after significant vendor changes or incidents.

## Exceptions

Exceptions require management approval and documented risk acceptance.

## Enforcement

Unreviewed critical vendors must be tracked as open risks before production launch.

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
