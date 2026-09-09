# SOC 2 Type I Readiness Gate

READY FOR READINESS REVIEW does not mean SOC 2 compliant. Each gate below requires evidence before engaging the CPA auditor.

| Gate | Status | Required Evidence | Responsible Role | Blocking Issue | Completion Criteria |
| --- | --- | --- | --- | --- | --- |
| GATE 1 - Policies approved | MANAGEMENT APPROVAL REQUIRED | Approved policy versions with owner, approver, effective date, review cadence | TradePilot Management | Draft policies are not evidence of operation | All required policies approved and communicated. |
| GATE 2 - Production access inventory complete | COLLECTION REQUIRED | Production access register, user/role exports, business need, MFA status | Production Administrator | Actual provider access not recorded | Every production system has current access inventory. |
| GATE 3 - MFA verified | COLLECTION REQUIRED | MFA/SSO evidence for GitHub, Render, Vercel, Supabase, Stripe, OpenAI, Qdrant, Databento, Resend, DNS | Production Administrator | Repo cannot verify provider MFA | MFA enabled where supported or exception approved. |
| GATE 4 - Vendor reviews complete | COLLECTION REQUIRED | Vendor register, review templates, SOC/security reports, DPA status | Service Owner | Vendor risk status unknown | All critical vendors reviewed or risk accepted. |
| GATE 5 - Risk assessment complete | MANAGEMENT APPROVAL REQUIRED | Risk register, impact/likelihood ratings, treatment owners, accepted risks | TradePilot Management | Initial risk assessment not approved | Risk register reviewed and signed off. |
| GATE 6 - Backup restore test completed | COLLECTION REQUIRED | Supabase backup settings, restore test record, schema/data verification | Production Administrator | Restore capability unproven | Restore test succeeds or risk accepted with remediation plan. |
| GATE 7 - Incident-response tabletop completed | COLLECTION REQUIRED | Tabletop notes, incident template exercise, corrective actions | TradePilot Management | No operational exercise evidence | Tabletop completed and action items tracked. |
| GATE 8 - Change-management evidence available | COLLECTION REQUIRED | PR/review/CI/deployment sample, branch protection, deployment permissions | Application Developer / Production Administrator | Provider settings not evidenced | Sample change package proves procedure operation. |
| GATE 9 - Monitoring/escalation evidence available | EXTERNAL CONFIGURATION REQUIRED | Alert routing, alert test, owner acknowledgement, escalation record | Production Administrator | Logging exists but alert routing not proven | Critical alert reaches accountable owner and is documented. |
| GATE 10 - Evidence repository organized | MANAGEMENT APPROVAL REQUIRED | Evidence index, folder/repository access restrictions, retention decision | TradePilot Management | Evidence storage/retention not formalized | Evidence repository exists with restricted access and retention decision. |
| GATE 11 - Vulnerability/scanning process active | CI EVIDENCE REQUIRED | Dependency/SAST/secret scan evidence, remediation SLA | Application Developer | Workflow exists but required-check evidence is pending | Critical/high findings create tracked remediation. |
| GATE 12 - Admin MFA or compensating control active | EXTERNAL CONFIGURATION REQUIRED | App-admin MFA or MFA-enforced access layer evidence | Application Developer / Production Administrator | Production validator requires declaration; external enforcement proof pending | Admin access requires MFA or approved compensating control. |
| GATE 13 - Distributed abuse protection active | DATABASE / CONFIGURATION REQUIRED | Database rate-limit migration/configuration and test | Application Developer / Production Administrator | Production migration/config evidence pending | Auth/billing/import/AI abuse limits work across instances. |

## Readiness Decision Rule

TradePilot should not schedule a Type I audit until Gates 1 through 10 are complete or explicitly accepted by management and the auditor. Gates 11 through 13 should be resolved or reviewed with the auditor as potential Type I scope risks before the readiness review.
