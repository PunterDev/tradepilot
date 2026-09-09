# Incident Response Policy

## Purpose

Define how TradePilot identifies, escalates, contains, investigates, recovers from, and documents security and availability incidents.

## Scope

Applies to suspected or confirmed incidents involving TradePilot customer data, production systems, source code, credentials, billing, third-party services, or application availability.

## Roles and Responsibilities

- TradePilot Management owns incident decisions, customer notification decisions, and post-incident review.
- Incident Lead coordinates response activities for each incident.
- Application Developers and Service Owners investigate systems under their control.
- Production Administrators perform containment and recovery actions.

## Policy Statements

- Suspected incidents must be reported promptly.
- Incidents must be classified by severity and assigned an Incident Lead.
- Customer impact and data impact must be assessed.
- Evidence must be preserved and secrets must not be exposed in incident records.
- Post-incident reviews must identify corrective actions for significant incidents.

## Procedures

- Follow `docs/compliance/runbooks/SECURITY_INCIDENT_RESPONSE_RUNBOOK.md`.
- Record incidents using `docs/compliance/evidence/templates/INCIDENT_RECORD_TEMPLATE.md`.
- Track incidents in `docs/compliance/registers/SECURITY_INCIDENT_REGISTER.md` without fabricating historical incidents.

## Evidence Retained

Incident records, timeline, logs, screenshots with secrets redacted, containment actions, notification decision, postmortem, and corrective action tracking.

## Review Frequency

At least annually and after each significant incident or tabletop exercise.

## Exceptions

Emergency containment may occur before formal approval, but must be documented afterward.

## Enforcement

Failure to report or preserve incident evidence may result in access review or corrective action.

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
