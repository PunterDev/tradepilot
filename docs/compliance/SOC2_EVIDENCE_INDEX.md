# SOC 2 Evidence Index

Status values: AVAILABLE, COLLECTION REQUIRED, IMPLEMENTATION REQUIRED, MANAGEMENT APPROVAL REQUIRED.

| Evidence ID | SOC 2 Criterion | Control | Evidence Description | Source | Owner | Frequency | Retention | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| EVD-001 | CC1/CC5 | Policy governance | Draft security policies | `docs/compliance/policies/` | TradePilot Management | Annual | Audit period + policy retention | MANAGEMENT APPROVAL REQUIRED |
| EVD-002 | CC6 | Access control | Quarterly production access review | `docs/compliance/procedures/ACCESS_REVIEW_PROCEDURE.md` and template | TradePilot Management | Quarterly | Audit period | COLLECTION REQUIRED |
| EVD-003 | CC6 | Production access inventory | Production access register | `docs/compliance/registers/PRODUCTION_ACCESS_REGISTER.md` | Production Administrator | Quarterly | Audit period | COLLECTION REQUIRED |
| EVD-004 | CC9 | Vendor risk | Vendor register and review templates | `docs/compliance/registers/VENDOR_REGISTER.md` | Service Owner | Annual | Vendor term + audit period | COLLECTION REQUIRED |
| EVD-005 | CC3 | Risk assessment | Risk register and annual review | `docs/compliance/registers/RISK_REGISTER.md` | TradePilot Management | Annual/significant change | Audit period | MANAGEMENT APPROVAL REQUIRED |
| EVD-006 | CC7 | Incident response | Incident response runbook and incident register | `docs/compliance/runbooks/SECURITY_INCIDENT_RESPONSE_RUNBOOK.md` | TradePilot Management | Per incident/annual tabletop | Audit period | MANAGEMENT APPROVAL REQUIRED |
| EVD-007 | CC7/CC9 | Backup restore | Restore test record | `docs/compliance/evidence/templates/RESTORE_TEST_TEMPLATE.md` | Production Administrator | Annual/major change | Audit period | COLLECTION REQUIRED |
| EVD-008 | CC7 | Monitoring escalation | Monitoring escalation runbook | `docs/compliance/runbooks/MONITORING_ESCALATION_RUNBOOK.md` | Production Administrator | Annual | Audit period | MANAGEMENT APPROVAL REQUIRED |
| EVD-009 | CC8 | Change management | Change management procedure and change records | `docs/compliance/procedures/CHANGE_MANAGEMENT_PROCEDURE.md` | Application Developer | Per change | Audit period | MANAGEMENT APPROVAL REQUIRED |
| EVD-010 | CC1 | Security training | Training record template | `docs/compliance/evidence/templates/SECURITY_TRAINING_RECORD.md` | TradePilot Management | Onboarding/annual | Audit period | COLLECTION REQUIRED |
| EVD-011 | CC6 | Backend route security | Auth/RBAC/user isolation tests | `backend/src/test/java/com/tradepilot/RbacSecurityTests.java`, `UserDataIsolationTests.java`, `AuthHardeningTests.java` | Application Developer | Per release | Audit period | AVAILABLE |
| EVD-012 | CC6 | JWT controls | JWT secret/token tests | `backend/src/test/java/com/tradepilot/JwtServiceSecurityTests.java` | Application Developer | Per release | Audit period | AVAILABLE |
| EVD-013 | CC6/CC7 | Stripe controls | Stripe billing and webhook tests | `backend/src/test/java/com/tradepilot/StripeBillingTests.java`, `StripeWebhookLiveModeTests.java` | Application Developer | Per release | Audit period | AVAILABLE |
| EVD-014 | CC8 | Database schema control | Flyway migrations and PostgreSQL baseline CI | `backend/src/main/resources/db/migration/`, `.github/workflows/postgres-flyway-baseline.yml` | Application Developer | Per release | Audit period | AVAILABLE |
| EVD-015 | CC7 | Observability | Request/AI/email/Stripe monitoring implementation | `backend/OBSERVABILITY.md`, `ObservabilityService.java` | Application Developer | Per release | Audit period | AVAILABLE |
| EVD-016 | CC6/CC8 | Production validators | Production config validators and tests | `SecurityProductionConfigValidator.java`, `ProductionUrlConfig.java`, `StripeProductionConfigValidator.java` | Application Developer | Per release | Audit period | AVAILABLE |
| EVD-017 | CC6/Confidentiality | Secrets readiness | Secret readiness audit docs | `docs/PRODUCTION_SECRET_READINESS.md` | Production Administrator | Per release/rotation | Audit period | AVAILABLE |
| EVD-018 | Confidentiality | Data inventory | Data classification and retention inventory | `docs/compliance/registers/DATA_INVENTORY.md` | TradePilot Management | Annual/significant change | Audit period | MANAGEMENT APPROVAL REQUIRED |
| EVD-019 | CC8 | Branch protection/deployment approvals | GitHub/Render/Vercel settings | Provider dashboards | Production Administrator | Per release/quarterly | Audit period | COLLECTION REQUIRED |
| EVD-020 | CC7/Availability | Uptime/alert routing | Uptime monitor and alert notification evidence | Monitoring provider | Production Administrator | Continuous/review monthly | Audit period | IMPLEMENTATION REQUIRED |
| EVD-021 | CC6/CC7 | Technical security remediation | Distributed rate limiting, admin-access production gate, CI security scans, and frontend/session/header hardening | `docs/compliance/SOC2_TECHNICAL_SECURITY_EVIDENCE.md`, `.github/workflows/security-scans.yml`, `DatabaseRateLimiter.java`, `frontend/vercel.json` | Application Developer | Per release | Audit period | AVAILABLE |
| EVD-022 | CC1-CC9 | Final internal readiness verification | Final Type I readiness report and auditor package index | `docs/compliance/SOC2_TYPE1_FINAL_READINESS_REPORT.md`, `docs/compliance/SOC2_AUDITOR_PACKAGE_INDEX.md` | TradePilot Management | Before auditor engagement/significant change | Audit period | AVAILABLE |
