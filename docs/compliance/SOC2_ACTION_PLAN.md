# TradePilot SOC 2 Action Plan

This plan lists remediation work only. It does not claim SOC 2 compliance.

## Phase 2 Disposition Summary

| ID | Phase 2 Status | Notes |
| --- | --- | --- |
| SOC2-P0-001 | ADDRESSED - MANAGEMENT ACTION REQUIRED | Draft policy suite created in `docs/compliance/policies/`; management approval, effective dates, communication, and acknowledgements remain required. |
| SOC2-P0-002 | DOCUMENTED - MANAGEMENT ACTION REQUIRED | Access policy, procedure, register, and template created; actual access inventory/review evidence remains required. |
| SOC2-P0-003 | DOCUMENTED - MANAGEMENT ACTION REQUIRED | Backup/restore policy, procedure, and restore template created; actual backup settings, RTO/RPO, and restore test evidence remain required. |
| SOC2-P0-004 | DOCUMENTED - TECHNICAL/INFRA REMEDIATION REQUIRED | Incident/monitoring runbooks drafted; external alert routing and alert test evidence still required. |
| SOC2-P0-005 | DOCUMENTED - MANAGEMENT ACTION REQUIRED | Change management policy/procedure/template created; GitHub/Render/Vercel branch/deploy approval evidence still required. |
| SOC2-P0-006 | DOCUMENTED - MANAGEMENT ACTION REQUIRED | Vendor register/procedure/template created; vendor security reviews, SOC/security report review, and DPA status remain required. |
| SOC2-P1-001 | FIXED - EXTERNAL EVIDENCE REQUIRED | PostgreSQL-backed rate limiter and production validator added; production migration/config evidence still required. |
| SOC2-P1-002 | PARTIALLY FIXED - EXTERNAL CONFIGURATION REQUIRED | Production startup now requires an admin MFA/access-gateway declaration; actual MFA/gateway setup evidence remains required. |
| SOC2-P1-003 | FIXED - CI EVIDENCE REQUIRED | Security scanning workflow added for SAST, dependency, and secret scanning; branch protection and scan artifacts still required. |
| SOC2-P1-004 | ADDRESSED - MANAGEMENT ACTION REQUIRED | Data classification policy and data inventory drafted; approval and validation remain required. |
| SOC2-P1-005 | DOCUMENTED - MANAGEMENT ACTION REQUIRED | Retention/deletion policy and data inventory drafted; tested production workflow remains required. |
| SOC2-P1-006 | PARTIALLY FIXED - EXTERNAL CONFIGURATION REQUIRED | App observability/retention exists and evidence doc added; centralized log destination and routed alert test remain required. |
| SOC2-P1-007 | FIXED - FURTHER DESIGN OPTIONAL | Production auth storage moved to sessionStorage and frontend/backend security headers added; httpOnly cookie redesign remains optional future hardening. |

## P0 - Critical Before Type I Readiness

### SOC2-P0-001 - Approve Core Security Policy Set

| Field | Detail |
| --- | --- |
| Why required | SOC 2 auditors cannot test undocumented controls. |
| SOC 2 criterion | CC1, CC5, CC6, CC7, CC8, CC9 |
| Current state | Technical docs exist, but formal approved policies are missing. |
| Required change | Create and approve Information Security, Access Control, Change Management, Incident Response, Backup/Restore, Vendor Risk, Risk Assessment, Retention/Deletion, Confidentiality, Logging/Monitoring policies. |
| Implementation type | DOC / PROCESS |
| Files/components likely affected | `docs/compliance/` policy docs later; do not mix with code. |
| Evidence required | Approved policy versions, owner, effective date, team acknowledgment. |
| Acceptance criteria | All P0/P1 policies approved and stored; owner and review cadence documented. |

### SOC2-P0-002 - Complete Privileged Access Inventory and Review

| Field | Detail |
| --- | --- |
| Why required | CC6 requires authorized, least-privilege access and periodic review. |
| SOC 2 criterion | CC6.2, CC6.3, CC6.4 |
| Current state | Application RBAC exists; external provider access evidence is not in repo. |
| Required change | Inventory GitHub, Render, Vercel, Supabase, Stripe, OpenAI, Qdrant, Databento, Resend users/roles; remove unnecessary access; retain signoff. |
| Implementation type | PROCESS / THIRD-PARTY / AUDITOR EVIDENCE |
| Files/components likely affected | Documentation only. |
| Evidence required | Redacted user/role exports, MFA screenshots, access review signoff. |
| Acceptance criteria | Initial review completed; recurring quarterly review scheduled. |

### SOC2-P0-003 - Verify Backups and Restore

| Field | Detail |
| --- | --- |
| Why required | Backup and recovery evidence is required for Security operations and especially Availability. |
| SOC 2 criterion | CC7.5, CC9.2 |
| Current state | Deployment notes mention backups, but no restore evidence. |
| Required change | Enable production Supabase backups/PITR as appropriate; perform restore drill to non-production database; document RTO/RPO. |
| Implementation type | INFRA / PROCESS |
| Files/components likely affected | `docs/PRODUCTION_DB_BOOTSTRAP.md`, future DR docs. |
| Evidence required | Backup settings screenshot, restore logs, verification query output, RTO/RPO approval. |
| Acceptance criteria | Restore drill proves data can be recovered and app validates schema. |

### SOC2-P0-004 - Configure Alert Routing and Incident Escalation

| Field | Detail |
| --- | --- |
| Why required | Recorded metrics are not enough; someone must be notified and respond. |
| SOC 2 criterion | CC4, CC7 |
| Current state | Observability tables and alert service exist, but external alert routing is not evidenced. |
| Required change | Route critical alerts to a monitored channel; define severity, owner, response SLA, and postmortem process. |
| Implementation type | INFRA / PROCESS |
| Files/components likely affected | Observability configuration, future runbooks. |
| Evidence required | Alert test, routing screenshot, incident ticket sample. |
| Acceptance criteria | Simulated alert creates notification and response record. |

### SOC2-P0-005 - Establish Change Management Gate

| Field | Detail |
| --- | --- |
| Why required | SOC 2 CC8 requires changes be authorized, tested, approved, and tracked. |
| SOC 2 criterion | CC8.1 |
| Current state | Backend and PostgreSQL baseline CI exist; branch/deploy protection evidence not available. |
| Required change | Require PR review, backend tests, PostgreSQL baseline gate, frontend build/test, and deployment approval before production. |
| Implementation type | PROCESS / INFRA |
| Files/components likely affected | GitHub branch protection, Render/Vercel deployment settings. |
| Evidence required | Branch protection screenshots, passing CI reports, deployment approval logs. |
| Acceptance criteria | Production branch cannot deploy unless required checks pass and approval is recorded. |

### SOC2-P0-006 - Build Vendor Risk Register

| Field | Detail |
| --- | --- |
| Why required | TradePilot relies on multiple subprocessors for confidential and billing data. |
| SOC 2 criterion | CC9.1 |
| Current state | Vendors are inferable from code but not formally risk-ranked. |
| Required change | Create vendor inventory with data shared, criticality, SOC report/DPA/security review, renewal/review date. |
| Implementation type | PROCESS / THIRD-PARTY |
| Files/components likely affected | Future compliance docs. |
| Evidence required | Vendor register, SOC reports or security pages, DPA status. |
| Acceptance criteria | All critical vendors reviewed and assigned owner/risk rating. |

## P1 - High Priority Technical/Operational Hardening

### SOC2-P1-001 - Add Distributed or Edge Rate Limiting

| Field | Detail |
| --- | --- |
| Why required | In-memory rate limiting does not coordinate across multiple instances. |
| SOC 2 criterion | CC6.6, CC7.1 |
| Current state | `RateLimitInterceptor.java` can use `DatabaseRateLimiter.java`; production validator requires `RATE_LIMIT_STORE=database`. |
| Required change | Apply `V006__distributed_rate_limit_windows.sql`, configure production `RATE_LIMIT_STORE=database`, and retain abuse test evidence. |
| Implementation type | INFRA / CODE |
| Files/components likely affected | Security/rate-limit config; edge provider settings. |
| Evidence required | Config screenshots, abuse test results, logs. |
| Acceptance criteria | Limits apply consistently across restarts and multiple instances; verifier shows `rate_limit_windows` exists. |

### SOC2-P1-002 - Add Admin MFA or Equivalent SSO/MFA Gate

| Field | Detail |
| --- | --- |
| Why required | Admin role can view/change sensitive user and billing state. |
| SOC 2 criterion | CC6.1, CC6.5 |
| Current state | Admin RBAC exists; production startup requires `APP_ADMIN_ACCESS_PROTECTION=external-mfa` or `privileged-access-gateway`. |
| Required change | Restrict admin app through an MFA-enforced identity provider/VPN/access proxy and retain proof. |
| Implementation type | CODE / INFRA |
| Files/components likely affected | Auth flow, admin route access, provider settings. |
| Evidence required | MFA enrollment and login test evidence. |
| Acceptance criteria | Admin access requires a second factor or equivalent enforced provider control, and Render production config records the selected control. |

### SOC2-P1-003 - Enable SAST, Dependency, and Secret Scanning

| Field | Detail |
| --- | --- |
| Why required | Vulnerability management needs recurring detection and remediation evidence. |
| SOC 2 criterion | CC6.8, CC7.1, CC8.1 |
| Current state | CI tests exist; `.github/workflows/security-scans.yml` now adds Gitleaks, OWASP Dependency-Check, npm audit, and Semgrep. |
| Required change | Make the scanning workflow a required production-branch/deployment check and retain scan outputs. |
| Implementation type | INFRA / PROCESS |
| Files/components likely affected | `.github/`, GitHub settings. |
| Evidence required | Scan reports, alert triage tickets. |
| Acceptance criteria | New critical/high vulnerability or secret scan finding fails CI and creates a tracked remediation item. |

### SOC2-P1-004 - Define Data Classification and Confidentiality Program

| Field | Detail |
| --- | --- |
| Why required | Trading history, journal notes, AI analysis, and billing metadata are confidential customer data. |
| SOC 2 criterion | Confidentiality, CC6 |
| Current state | Privacy page exists; classification and handling rules missing. |
| Required change | Classify data categories, define handling, retention, vendor sharing, and support access rules. |
| Implementation type | DOC / PROCESS |
| Files/components likely affected | Privacy/compliance docs. |
| Evidence required | Data inventory, approved classification policy, vendor data-flow map. |
| Acceptance criteria | Every data category has classification, owner, storage, vendor, retention, deletion path. |

### SOC2-P1-005 - Formalize Retention and Deletion Workflow

| Field | Detail |
| --- | --- |
| Why required | Confidentiality/privacy controls require reliable retention and deletion. |
| SOC 2 criterion | CC7, Confidentiality |
| Current state | Cleanup SQL and some user/vector delete paths exist, but no production support workflow. |
| Required change | Define retention windows, deletion SLA, Qdrant deletion verification, Stripe/legal retention exceptions. |
| Implementation type | PROCESS / CODE |
| Files/components likely affected | Deletion services/scripts, support runbook. |
| Evidence required | Test deletion record, verification output, support ticket template. |
| Acceptance criteria | A test account can be deleted/anonymized consistently across Postgres and Qdrant. |

### SOC2-P1-006 - Complete Production Logging and Retention Configuration

| Field | Detail |
| --- | --- |
| Why required | SOC 2 requires monitoring and evidence retention without leaking confidential data. |
| SOC 2 criterion | CC4, CC7, Confidentiality |
| Current state | App logging, DB observability, retention settings, and alert records exist; centralized log retention not evidenced. |
| Required change | Configure centralized logs, retention period, access controls, alerting, and log redaction standards. |
| Implementation type | INFRA / PROCESS |
| Files/components likely affected | Render/log provider settings, logging policy. |
| Evidence required | Log destination screenshot, retention setting, redaction test. |
| Acceptance criteria | Production logs are searchable by request ID and retained per policy. |

### SOC2-P1-007 - Harden Frontend Session/XSS Risk

| Field | Detail |
| --- | --- |
| Why required | JWTs are stored in `localStorage`, so XSS impact is high. |
| SOC 2 criterion | CC6.5, CC6.6 |
| Current state | Production auth session writes use `sessionStorage`; legacy localStorage read fallback remains for migration and tests. CSP/security headers are configured in `frontend/vercel.json`. |
| Required change | Retain header scan evidence and consider httpOnly same-site cookies or short-lived access/refresh-token design as a future reduction of residual XSS token exposure. |
| Implementation type | CODE / INFRA |
| Files/components likely affected | Frontend hosting headers, auth API. |
| Evidence required | Header scan, XSS/session design review. |
| Acceptance criteria | Production CSP exists and session-token exposure risk is documented/mitigated. |

## P2 - Before Type I / Early Type II

### SOC2-P2-001 - Run Incident Response Tabletop

| Field | Detail |
| --- | --- |
| Why required | Auditors test that incident process operates, not just that it exists. |
| SOC 2 criterion | CC7.3, CC7.4 |
| Current state | No tabletop evidence found. |
| Required change | Conduct tabletop for credential exposure, Stripe webhook failure, and database outage. |
| Implementation type | PROCESS |
| Files/components likely affected | Incident runbooks. |
| Evidence required | Exercise notes, action items, owner signoff. |
| Acceptance criteria | Corrective actions are tracked to closure. |

### SOC2-P2-002 - Document and Test Billing Support Reconciliation

| Field | Detail |
| --- | --- |
| Why required | Stripe billing controls are strong but support must reconcile edge cases. |
| SOC 2 criterion | CC7, CC9 |
| Current state | Stripe rollout/reconciliation docs exist. |
| Required change | Add support procedure for partial refunds, unknown events, duplicate charges/subscriptions, and negative purchased-credit balances. |
| Implementation type | PROCESS / DOC |
| Files/components likely affected | Billing support runbook. |
| Evidence required | Sample reconciliation output and support ticket. |
| Acceptance criteria | Support can trace user plan/credits to Stripe events and ledger rows. |

### SOC2-P2-003 - Verify Provider Security Settings

| Field | Detail |
| --- | --- |
| Why required | Many controls are platform-managed. |
| SOC 2 criterion | CC6, CC7, CC8, CC9 |
| Current state | Repo cannot prove provider settings. |
| Required change | Capture settings for MFA, TLS, access, secrets, logging, backups, deployment approvals. |
| Implementation type | THIRD-PARTY / AUDITOR EVIDENCE |
| Files/components likely affected | Evidence repository outside code. |
| Evidence required | Redacted screenshots/exports. |
| Acceptance criteria | Evidence package covers each critical vendor. |

### SOC2-P2-004 - Add Frontend CI Gate

| Field | Detail |
| --- | --- |
| Why required | Frontend auth/session/build changes affect security and production availability. |
| SOC 2 criterion | CC8.1 |
| Current state | Frontend scripts exist; repo evidence of required frontend CI is incomplete. |
| Required change | Add/require frontend typecheck, tests, and build in CI. |
| Implementation type | INFRA |
| Files/components likely affected | `.github/workflows/`. |
| Evidence required | Passing CI reports. |
| Acceptance criteria | Production deploy requires frontend checks. |

## P3 - Type II Operating Period

### SOC2-P3-001 - Run Quarterly Access Reviews

| Field | Detail |
| --- | --- |
| Why required | CC6 requires recurring review. |
| SOC 2 criterion | CC6.4 |
| Current state | Initial review not yet evidenced. |
| Required change | Quarterly review all privileged systems. |
| Implementation type | PROCESS |
| Files/components likely affected | Evidence tracker. |
| Evidence required | Quarterly exports and signoffs. |
| Acceptance criteria | No stale privileged access remains. |

### SOC2-P3-002 - Run Annual Vendor Reviews

| Field | Detail |
| --- | --- |
| Why required | CC9 requires risk mitigation for third-party dependencies. |
| SOC 2 criterion | CC9.1 |
| Current state | Vendor register missing. |
| Required change | Review vendor SOC reports, DPAs, access, data shared, and incidents annually. |
| Implementation type | PROCESS |
| Files/components likely affected | Vendor register. |
| Evidence required | Review notes and updated risk ratings. |
| Acceptance criteria | Critical vendor reviews are current. |

### SOC2-P3-003 - Retain Change Evidence for Every Release

| Field | Detail |
| --- | --- |
| Why required | Type II tests control operation over time. |
| SOC 2 criterion | CC8.1 |
| Current state | CI exists; release evidence collection is not formalized. |
| Required change | Store PR, review, CI, deployment, rollback, migration evidence per release. |
| Implementation type | PROCESS |
| Files/components likely affected | Release checklist. |
| Evidence required | Sampled release packets. |
| Acceptance criteria | Auditor can sample releases and see approval/test/deploy evidence. |

### SOC2-P3-004 - Track Vulnerability Remediation SLAs

| Field | Detail |
| --- | --- |
| Why required | Vulnerability management requires timely remediation. |
| SOC 2 criterion | CC6.8, CC7.1 |
| Current state | Scanning and SLA evidence missing. |
| Required change | Define severity SLA and track vulnerabilities to closure. |
| Implementation type | PROCESS / INFRA |
| Files/components likely affected | GitHub/security tools. |
| Evidence required | Vulnerability tickets and closure dates. |
| Acceptance criteria | Critical/high issues meet SLA or have approved exceptions. |
