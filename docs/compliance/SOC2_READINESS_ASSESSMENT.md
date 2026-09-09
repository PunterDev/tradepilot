# TradePilot SOC 2 Readiness Assessment

Assessment date: 2026-08-31

Scope reviewed: backend Spring Boot/Java code, React/Vite frontend, Flyway migrations, Supabase/PostgreSQL SQL artifacts, GitHub Actions, deployment and Stripe runbooks, security tests, example environment files, and existing operational documentation in this repository.

This is a readiness assessment, not an attestation. It does not claim SOC 2 compliance and does not replace a formal readiness review by an auditor.

## 1. Executive Summary

| Area | Readiness | Estimate | Basis |
| --- | --- | ---: | --- |
| Security technical readiness | MODERATE to NEAR READY | 78% | Strong auth, authorization, Stripe idempotency, production validators, Flyway baseline, and tests are present. Remaining technical gaps are mostly distributed abuse protection, app-admin MFA, centralized alerting evidence, and operational hardening. |
| Operational/policy readiness | EARLY | 38% | Many controls require formal policies, named owners, access reviews, vendor reviews, incident/DR procedures, backup evidence, and change-management evidence that are not fully present in the repo. |
| Overall SOC 2 readiness | MODERATE | 60% | Weighted estimate: 60% technical implementation, 40% governance/evidence maturity. |

Overall readiness: MODERATE.

TradePilot has a credible technical foundation for SOC 2 Security/Common Criteria. The strongest areas are authentication hardening, route authorization, tenant-scoped data access, Stripe production safety, database schema determinism, and observability tables. The main gap is not one large missing security feature; it is the operating system around the software: policies, reviews, evidence capture, vendor management, incident response, backups, monitoring escalation, and production access governance.

## 2. Recommended Initial SOC 2 Scope

Recommended initial scope: Security + Confidentiality, defer Availability unless public uptime commitments are required for the first audit.

Rationale:

| Trust Service Category | Recommendation | Reason |
| --- | --- | --- |
| Security | Include | Core SaaS access control, change management, vulnerability management, incident response, and infrastructure controls are mandatory baseline expectations. |
| Confidentiality | Include if customer trading data is positioned as private/confidential | TradePilot stores trading history, journal notes, AI-generated analysis, billing metadata, and vector memory metadata. Confidentiality controls will be expected by customers. |
| Availability | Defer or include lightly | Repo has observability and deployment runbooks, but formal uptime objectives, DR tests, backup restore tests, and incident escalation evidence are not mature enough for a comfortable Availability scope. |

## 3. Critical Blockers

| Finding | Severity | Status | Impact | Recommendation |
| --- | --- | --- | --- | --- |
| Formal SOC 2 governance and policies are not complete | CRITICAL | PROCESS / EVIDENCE REQUIRED | An auditor cannot rely only on application code for CC1, CC2, CC3, CC4, CC5, CC8, and CC9. | Create and approve the required policy set, assign owners, define review cadence, and retain approval evidence. |
| Production access control evidence is missing | CRITICAL | PROCESS / EVIDENCE REQUIRED | Cannot prove least privilege, access approvals, offboarding, MFA, or periodic access review for GitHub, Render, Vercel, Supabase, Stripe, OpenAI, Qdrant, Databento, and Resend. | Inventory all privileged systems, require MFA/SSO where available, perform initial access review, and retain screenshots/export evidence. |
| Backup, restore, and disaster recovery evidence is missing | CRITICAL | PROCESS / EVIDENCE REQUIRED | Cannot support Availability readiness or prove recovery capability for customer trading/billing data. | Document RTO/RPO, enable Supabase backups/PITR as appropriate, perform restore drill, and retain evidence. |
| Incident response program evidence is missing | CRITICAL | PROCESS / EVIDENCE REQUIRED | Cannot prove detection, escalation, response, communication, breach notification, or postmortem process. | Create incident response plan, on-call/escalation procedure, severity matrix, tabletop exercise, and evidence retention procedure. |

## 4. High-Priority Gaps

| Finding | Severity | Evidence | Production Impact | Recommendation |
| --- | --- | --- | --- | --- |
| App-admin MFA is not implemented in application admin flows | HIGH | Backend RBAC exists in `SecurityConfig`, admin services use `@PreAuthorize`, but auth is password + JWT. | Compromised admin password can access user/billing/admin data if provider-level controls fail. | Add admin MFA or enforce SSO/MFA through an identity provider before exposing admin features broadly. |
| Distributed rate limiting requires production migration/config evidence | MEDIUM | `RateLimitInterceptor` can use `DatabaseRateLimiter`; production validator rejects in-memory rate limiting. | Limits are production-ready only after `V006__distributed_rate_limit_windows.sql` is applied and `RATE_LIMIT_STORE=database` is configured. | Apply migration, configure Render, and retain abuse-limit test evidence. |
| Centralized monitoring and alert-response evidence is incomplete | HIGH | Observability tables/services exist, plus `backend/OBSERVABILITY.md`; external alert routing is not proven by repo. | Failures may be recorded but not noticed or acted on. | Configure production alert destinations, define severity thresholds, owners, and escalation evidence. |
| Vendor risk management evidence is missing | HIGH | Third-party dependencies include Stripe, OpenAI, Qdrant, Databento, Resend, Supabase, Render, Vercel, GitHub. | SOC 2 auditors expect vendor inventory, risk ratings, and security document review. | Build vendor register, review SOC reports/DPAs/security pages, set annual review cadence. |
| Vulnerability management evidence is partial | HIGH | Maven/GitHub workflow exists for tests; no repo evidence of Dependabot, SAST, container/image scanning, or recurring triage. | Known vulnerable dependencies may not be detected or remediated on a defined timeline. | Enable Dependabot/Renovate, CodeQL/SAST, secret scanning, npm/Maven audits, and vulnerability SLA tracking. |
| Data retention/deletion process is incomplete | HIGH | `SAFE_TEST_DATA_CLEANUP.sql` and vector delete methods exist; user-facing retention/deletion policy and production process evidence are not complete. | Inability to satisfy retention commitments or deletion requests reliably. | Define retention schedule, deletion/export process, Qdrant deletion verification, and support workflow. |

## 5. Existing Strong Controls

| Area | Evidence | Status |
| --- | --- | --- |
| Route-level auth and admin RBAC | `backend/src/main/java/com/tradepilot/security/SecurityConfig.java`, `AdminUserService.java`, admin controllers/services | IMPLEMENTED |
| JWT secret validation and token invalidation | `JwtService.java`, `AuthService.java`, `AuthTokenService.java`, `JwtServiceSecurityTests.java` | IMPLEMENTED |
| Password reset/email verification token hashing | `AuthTokenService.java` stores SHA-256 token hashes and invalidates prior active tokens | IMPLEMENTED |
| Production-safe Stripe billing | `StripeBillingService.java`, `StripeProductionConfigValidator.java`, `StripeBillingTests.java`, Stripe audit docs | IMPLEMENTED |
| Flyway-owned production schema | `backend/src/main/resources/db/migration/V001__fresh_production_schema.sql` through `V006__distributed_rate_limit_windows.sql` | IMPLEMENTED |
| PostgreSQL baseline CI gate | `.github/workflows/postgres-flyway-baseline.yml`, `FlywayPostgresBaselineIntegrationTest.java` | IMPLEMENTED |
| Log/error sanitization | `LogSanitizer.java`, `EmailService.java`, `StripeBillingService.java`, structured security errors | PARTIALLY IMPLEMENTED |
| Request correlation | `RequestCorrelationFilter.java`, `logback-spring.xml`, observability metrics | IMPLEMENTED |
| Admin audit log for manual plan/role changes | `AdminAuditLog.java`, `AdminUserService.java`, admin UI detail views | IMPLEMENTED |

## 6. SOC 2 Common Criteria Assessment

### CC1 - Control Environment

| Criterion | Control | Status | Severity | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| CC1.1 | Integrity and ethical values are defined and communicated | MISSING | HIGH | No formal code of conduct/security responsibility policy found. | No auditable management-approved policy evidence. | Create Information Security Policy and Code of Conduct/Acceptable Use. |
| CC1.2 | Board/founder oversight over security | PROCESS / EVIDENCE REQUIRED | HIGH | Repo has technical docs, but no governance minutes or assigned control owners. | Auditor needs ownership and oversight evidence. | Assign executive/security owner, create review cadence, retain minutes. |
| CC1.3 | Management establishes authority and responsibility | PARTIALLY IMPLEMENTED | MEDIUM | Admin roles exist in application code. | Organizational role matrix and production access owners missing. | Maintain responsibility matrix for GitHub, Render, Vercel, Supabase, Stripe, OpenAI, Qdrant, Databento, Resend. |
| CC1.4 | Competence and security awareness | MISSING | MEDIUM | No training evidence found. | No onboarding/security-awareness proof. | Add security training and onboarding/offboarding checklist. |
| CC1.5 | Accountability enforced through performance/reviews | PROCESS / EVIDENCE REQUIRED | MEDIUM | Admin audit logs exist for app actions. | Employee/contractor accountability evidence outside repo missing. | Define disciplinary/accountability process and access review accountability. |

### CC2 - Communication and Information

| Criterion | Control | Status | Severity | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| CC2.1 | Relevant security information is identified and communicated | PARTIALLY IMPLEMENTED | MEDIUM | `docs/ENVIRONMENT_VARIABLES.md`, `docs/PRODUCTION_CHECKLIST.md`, Stripe rollout docs. | Internal security communication channels/process not documented. | Define security communication path and evidence capture. |
| CC2.2 | Internal communication supports control operation | PARTIALLY IMPLEMENTED | MEDIUM | GitHub workflow and docs provide developer guidance. | No formal control calendar or owner reminders. | Create recurring access review, vendor review, backup test, and incident test schedule. |
| CC2.3 | External communication channels exist | PARTIALLY IMPLEMENTED | MEDIUM | Contact page/backend contact message flow; terms/privacy pages. | Security contact, vulnerability disclosure, and customer incident notification process missing. | Publish security contact and vulnerability reporting process. |

### CC3 - Risk Assessment

| Criterion | Control | Status | Severity | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| CC3.1 | Objectives specify risk tolerance | MISSING | HIGH | No risk register/RTO/RPO/SLA policy found. | Risks cannot be prioritized against defined objectives. | Create risk assessment and risk appetite statement. |
| CC3.2 | Risks are identified and analyzed | PARTIALLY IMPLEMENTED | HIGH | Stripe and authorization audits exist in `docs/`. | No enterprise risk register or recurring review. | Maintain risk register with owner, severity, treatment, due date. |
| CC3.3 | Fraud and abuse risks are considered | PARTIALLY IMPLEMENTED | MEDIUM | Rate limits, Stripe idempotency, duplicate subscription prevention. | Abuse monitoring and incident playbooks not formalized. | Add abuse/security monitoring runbooks. |
| CC3.4 | Significant changes trigger risk review | PARTIALLY IMPLEMENTED | HIGH | Flyway CI gate and migration docs exist. | No formal change-risk review criteria. | Add change management policy with security review triggers. |

### CC4 - Monitoring Activities

| Criterion | Control | Status | Severity | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| CC4.1 | Controls are monitored | PARTIALLY IMPLEMENTED | HIGH | Observability metrics, alert service, admin observability UI. | External alert routing and owner response evidence missing. | Connect alerts to email/Slack/PagerDuty and retain incident/alert review evidence. |
| CC4.2 | Deficiencies are communicated and remediated | PROCESS / EVIDENCE REQUIRED | HIGH | Audit docs exist. | No formal deficiency tracker or SLA. | Track findings in issue tracker with owners and closure evidence. |

### CC5 - Control Activities

| Criterion | Control | Status | Severity | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| CC5.1 | Control activities mitigate risks | PARTIALLY IMPLEMENTED | MEDIUM | SecurityConfig, validators, Flyway, tests. | Business/process controls not complete. | Map repo controls to formal SOC 2 control matrix. |
| CC5.2 | Technology control activities are selected and developed | PARTIALLY IMPLEMENTED | MEDIUM | JWT validation, route RBAC, Stripe production safety, schema verification. | Some platform controls require external evidence. | Gather provider screenshots/settings evidence. |
| CC5.3 | Policies and procedures deployed | MISSING | HIGH | No complete policy set found. | Auditor cannot test undocumented controls. | Approve and publish policy suite before Type I. |

### CC6 - Logical and Physical Access Controls

| Criterion | Control | Status | Severity | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| CC6.1 | Logical access is restricted | VERIFIED BY CODE AND TEST | LOW | `SecurityConfig.java`, `RbacSecurityTests.java`, `UserDataIsolationTests.java`. | Provider access evidence still needed. | Keep app tests and add external access review evidence. |
| CC6.2 | New access is authorized | PARTIALLY IMPLEMENTED | HIGH | Application registration/email verification and admin role changes audited. | Internal employee access approval not documented. | Require approval tickets for GitHub/Render/Vercel/Supabase/Stripe access. |
| CC6.3 | Access is modified/removed timely | PARTIALLY IMPLEMENTED | HIGH | App admin role changes audited. | Offboarding process for operators/vendors missing. | Create offboarding checklist and retain evidence. |
| CC6.4 | Access is reviewed periodically | MISSING | HIGH | No access review exports found. | Cannot prove least privilege. | Quarterly access reviews for all privileged systems. |
| CC6.5 | Authentication controls | VERIFIED BY CODE AND TEST | MEDIUM | BCrypt, JWT secret validation, token version invalidation, hashed reset tokens. | No app-admin MFA. | Add admin MFA/SSO requirement. |
| CC6.6 | Infrastructure/network access protected | PROCESS / EVIDENCE REQUIRED | HIGH | Repo cannot prove Render/Supabase/GitHub access settings. | MFA, least privilege, network restrictions need evidence. | Capture provider security settings and restrict production DB access. |
| CC6.7 | Data transmission protected | PARTIALLY IMPLEMENTED | MEDIUM | Production URLs are HTTPS-oriented; CORS rejects localhost in prod. | TLS settings are mostly platform-managed and need evidence. | Retain Render/Vercel/Supabase TLS evidence. |
| CC6.8 | Unauthorized/malicious software prevented | PARTIALLY IMPLEMENTED | MEDIUM | Dependency-managed Java/Node builds. | No SAST/dependency/image scanning evidence. | Add CodeQL/dependency scanning and patch SLA. |

### CC7 - System Operations

| Criterion | Control | Status | Severity | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| CC7.1 | System boundaries and events are monitored | PARTIALLY IMPLEMENTED | HIGH | Request, AI, email, Stripe, cache, query, and system alert tables. | Centralized production log retention/alerting evidence missing. | Configure external monitoring and alert runbooks. |
| CC7.2 | Security events are evaluated | PARTIALLY IMPLEMENTED | HIGH | Stripe/auth/rate-limit logs and admin observability. | No triage workflow/SLA. | Define alert review and incident escalation process. |
| CC7.3 | Incidents are communicated | MISSING | HIGH | No incident communication policy found. | No auditable escalation/customer notification process. | Create Incident Response and Breach Notification plans. |
| CC7.4 | Incidents are remediated | PROCESS / EVIDENCE REQUIRED | HIGH | Stripe rollback docs exist. | No postmortem template/evidence. | Add postmortem process and corrective action tracking. |
| CC7.5 | Recovery from incidents | PARTIALLY IMPLEMENTED | HIGH | Stripe rollback/reconciliation docs. | Backup restore/DR tests missing. | Perform tabletop plus restore drill. |

### CC8 - Change Management

| Criterion | Control | Status | Severity | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| CC8.1 | Changes are authorized, tested, approved, and tracked | PARTIALLY IMPLEMENTED | HIGH | GitHub Actions backend tests and PostgreSQL Flyway baseline gate. | Branch protection, required reviews, deployment approval evidence not in repo. | Require PR reviews, branch protection, CI checks, deployment approvals, and change tickets. |
| CC8.1 | Database changes are controlled | VERIFIED BY CODE AND TEST | LOW | Flyway migrations V001-V006, PostgreSQL Testcontainers baseline, verification script. | Production migration application evidence still required. | Retain migration logs and schema verification output. |

### CC9 - Risk Mitigation

| Criterion | Control | Status | Severity | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| CC9.1 | Vendor and business risks are identified | MISSING | HIGH | No vendor register found. | Third-party processors are central to product. | Create vendor risk inventory and review SOC reports/DPAs. |
| CC9.2 | Business continuity and disaster recovery risks are managed | MISSING | HIGH | Deployment notes mention backups; no tested BCP/DR evidence. | Cannot prove resilience. | Create BCP/DR plan with RTO/RPO and restore drill. |

## 7. Availability Readiness

| Control Area | Status | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- |
| Health checks | IMPLEMENTED | `/actuator/health` is permitted in `SecurityConfig.java`. | Health detail/uptime monitor evidence not in repo. | Configure external uptime monitor. |
| Observability | PARTIALLY IMPLEMENTED | `ObservabilityService`, `ObservabilityAlertService`, admin observability UI. | Alert delivery and on-call evidence missing. | Add alert routing and incident response runbook. |
| Backups | PROCESS / EVIDENCE REQUIRED | `docs/DEPLOYMENT_NOTES.md` says enable backups. | No backup configuration or restore test evidence. | Enable Supabase backups/PITR and perform restore drill. |
| DR/rollback | PARTIALLY IMPLEMENTED | Stripe rollout/rollback docs exist. | Full application DR and Render/Vercel rollback evidence incomplete. | Create DR plan and test rollback. |
| Capacity/cost controls | PARTIALLY IMPLEMENTED | AI cost thresholds, Databento cost checks, rate limiting. | Autoscaling/capacity planning evidence missing. | Document expected load, limits, and alert thresholds. |

Availability is not ready for a broad audited scope without operational evidence.

## 8. Confidentiality Readiness

| Control Area | Status | Evidence | Gap | Recommendation |
| --- | --- | --- | --- | --- |
| Data classification | MISSING | No formal classification policy found. | Trading/journal/AI data sensitivity is not formally classified. | Create Data Classification and Confidentiality policies. |
| Encryption in transit | PARTIALLY IMPLEMENTED | HTTPS production URLs expected; platform-managed. | Need Render/Vercel/Supabase TLS evidence. | Retain provider TLS evidence. |
| Encryption at rest | PROCESS / EVIDENCE REQUIRED | Platform-managed by Supabase/Qdrant/OpenAI/Stripe. | Need vendor/provider evidence. | Gather SOC reports/security docs. |
| Least privilege access | PARTIALLY IMPLEMENTED | App route/user scoping is strong. | Production operator access review missing. | Quarterly access review. |
| Third-party AI processing | PARTIALLY IMPLEMENTED | OpenAI/Qdrant integrations and privacy page exist. | DPA/retention settings/evidence required. | Review vendor data retention, DPAs, and user disclosure. |
| Data deletion | PARTIALLY IMPLEMENTED | SQL cleanup and Qdrant delete methods exist. | Production process and SLA missing. | Create retention/deletion workflow and test it. |

Confidentiality can be included if policy/evidence gaps are closed before Type I.

## 9. Authentication Assessment

| Control | Status | Evidence | Notes |
| --- | --- | --- | --- |
| Password hashing | IMPLEMENTED | `SecurityConfig.passwordEncoder()` uses BCrypt. | Good baseline. |
| JWT strength | VERIFIED BY CODE AND TEST | `JwtService.java`, `JwtServiceSecurityTests.java`. | Requires Base64 32-byte secret and positive expiry. |
| Token invalidation | IMPLEMENTED | `tokenVersion` checked in JWT; password reset increments version. | Good defense against stale tokens. |
| Email verification | IMPLEMENTED | `AuthService.register/login/verifyEmail`. | Login blocked until verified. |
| Reset tokens | IMPLEMENTED | `AuthTokenService.java`; raw tokens not stored. | Token hashes, single-use, expiry, invalidation present. |
| Brute-force protection | PARTIALLY IMPLEMENTED | `RateLimitInterceptor.java`. | In-memory only; add distributed/edge protection. |
| Admin MFA | MISSING | No app admin MFA flow found. | High-priority SOC 2 gap. |
| Frontend token storage | PARTIALLY IMPLEMENTED | `frontend/src/api/authApi.ts`, `frontend/src/api/axios.ts` use `localStorage`. | Works but increases XSS impact; mitigate with CSP and consider httpOnly cookies. |

## 10. Authorization & Tenant Isolation Assessment

| Control | Status | Evidence | Notes |
| --- | --- | --- | --- |
| Default authenticated API | IMPLEMENTED | `SecurityConfig.anyRequest().authenticated()`. | Public endpoints are explicitly enumerated. |
| Admin RBAC | VERIFIED BY CODE AND TEST | `/api/admin/**` has role ADMIN; services use `@PreAuthorize`. | Good defense-in-depth. |
| `/api/auth/me` protected | VERIFIED BY CODE AND TEST | SecurityConfig explicitly authenticates `/api/auth/me` and `/api/v1/auth/me`; controller retains guard. | Recent hardening present. |
| User data scoping | VERIFIED BY CODE AND TEST | Repositories/services use `findByUser...`, `findByIdAndUser...`, `OwnershipGuard`, `ResourceNotFoundException`. | Good tenant isolation pattern. |
| Cross-user enumeration resistance | VERIFIED BY TEST | `UserDataIsolationTests`, authorization audit hardening. | Cross-user resources return 404 in reviewed areas. |
| Direct object reference controls | PARTIALLY IMPLEMENTED | Many services scope by user. | Continue review as new features are added. |
| Frontend route hiding | INFO | React checks user role for admin routes. | Backend RBAC is authoritative; frontend hiding is UX only. |

## 11. Production Security Assessment

| Control | Status | Evidence | Gap / Notes |
| --- | --- | --- | --- |
| Hibernate schema mode | IMPLEMENTED | `application.yml` defaults `JPA_DDL_AUTO=validate`. | Good Flyway ownership. |
| Flyway baseline | VERIFIED BY CODE AND TEST | V001-V006, CI integration test. | Production migration logs/evidence required. |
| Dev endpoints disabled in prod | IMPLEMENTED | `SecurityProductionConfigValidator.java` rejects dev auth/tools. | Good. |
| Production frontend URL validation | IMPLEMENTED | `ProductionUrlConfig.java` rejects missing/localhost in prod. | Good. |
| CORS production validation | IMPLEMENTED | `CorsConfig.java` rejects localhost/wildcard misuse. | Good. |
| Security headers | PARTIALLY IMPLEMENTED | Backend sets frame/CSP frame-ancestors/referrer/content-type. | Frontend CSP/HSTS provider evidence needed. |
| Secrets in config | PARTIALLY IMPLEMENTED | `application.yml` uses env references; secret readiness docs exist. | Provider secret settings and rotation evidence required. |
| CI checks | PARTIALLY IMPLEMENTED | GitHub Actions backend and PostgreSQL baseline. | Add dependency/SAST/secret scanning gates. |

## 12. Stripe / Billing Security Assessment

| Control | Status | Evidence | Notes |
| --- | --- | --- | --- |
| Webhook signature verification | VERIFIED BY CODE AND TEST | `StripeBillingService.handleWebhook`, Stripe tests. | Uses Stripe SDK signature verification. |
| Livemode enforcement | VERIFIED BY CODE AND TEST | Production rejects `livemode=false`. | Good. |
| Event idempotency | VERIFIED BY CODE AND TEST | `processed_stripe_events.event_id` unique plus save/flush race handling. | Good. |
| Purchase idempotency | VERIFIED BY CODE AND TEST | Checkout Session ID unique claim before credit grant. | Good. |
| Subscription reconciliation | VERIFIED BY CODE AND TEST | Sensitive events retrieve current Stripe subscription through `StripeApiClient`. | Good; runtime depends on Stripe API availability. |
| Duplicate subscription prevention | VERIFIED BY CODE AND TEST | Local locking, checkout attempts, subscription state checks. | Good. |
| Credit-pack validation | VERIFIED BY CODE AND TEST | Mode, paid status, amount, currency, pack ID, customer ownership. | Good. |
| Refund/dispute handling | VERIFIED BY CODE AND TEST | Ledger-backed reason-aware reversal fields. | Good. |
| Manual billing reconciliation | PARTIALLY IMPLEMENTED | Runbooks and admin displays exist. | Dedicated support tooling for partial refunds/unknown purchases still limited. |

## 13. Logging & Monitoring Assessment

| Control | Status | Evidence | Gap |
| --- | --- | --- | --- |
| Request correlation | IMPLEMENTED | `RequestCorrelationFilter.java`, `logback-spring.xml`. | Good. |
| Security error hygiene | IMPLEMENTED | `JwtAuthenticationFilter`, `SecurityErrorResponseWriter`, `ApiErrorResponseFactory`. | Good. |
| Secret/log sanitization | PARTIALLY IMPLEMENTED | `LogSanitizer`, email/Stripe error handling. | Some DTO `toString()` methods include trading notes/AI text; avoid logging request/response objects. |
| Metrics/audit tables | IMPLEMENTED | Observability entities/migrations. | Good technical foundation. |
| Alert handling | PARTIALLY IMPLEMENTED | `ObservabilityAlertService`, system alert tables. | Need external routing, owner, SLA, and evidence. |
| Log retention | PARTIALLY IMPLEMENTED | Local rolling logs configured. | Need production centralized log retention policy/evidence. |

## 14. Secrets Management Assessment

| Control | Status | Evidence | Gap |
| --- | --- | --- | --- |
| Production secret fallbacks | IMPLEMENTED | JWT/Stripe/URL validators and env references. | Continue scanning before releases. |
| Secret examples | IMPLEMENTED | `.env.example` files contain placeholders/blanks. | Good. |
| Runtime secret storage | PROCESS / EVIDENCE REQUIRED | Render/Vercel/Supabase settings outside repo. | Need provider screenshots/export evidence and rotation schedule. |
| Rotation evidence | PROCESS / EVIDENCE REQUIRED | `docs/PRODUCTION_SECRET_READINESS.md`. | Historical exposure required rotation; retain provider rotation evidence. |
| GitHub secret scanning | PROCESS / EVIDENCE REQUIRED | No repo proof of enabled GitHub Advanced Security/secret scanning. | Enable and retain setting evidence. |

## 15. Vendor Risk Inventory

| Vendor | Purpose | Data shared | Sensitivity | Current Evidence | Gap |
| --- | --- | --- | --- | --- | --- |
| Supabase/PostgreSQL | Primary database | User, trade, journal, AI, billing metadata | HIGH | Flyway schema/docs | SOC report, DPA, backup settings, access review |
| Render | Backend hosting | Runtime env, logs, app traffic | HIGH | Env manifest/runbooks | Access/MFA/deployment evidence |
| Vercel | Frontend hosting | Public app, frontend env | MEDIUM | `frontend/vercel.json`, env docs | Access/MFA/deployment evidence |
| Stripe | Payments/billing | Customer email, IDs, payment metadata | HIGH | Stripe billing code/runbooks | Live config evidence, DPA/SOC report, webhook evidence |
| OpenAI | AI analysis | Trading/journal/analysis prompts | HIGH | OpenAI service code | DPA, retention setting, approved data-flow documentation |
| Qdrant | Vector memory | Trade memory vectors/metadata | HIGH | Trading memory code/docs | DPA/SOC/security review, access review |
| Databento | Market data | Request metadata/contracts/timestamps | MEDIUM | Trade replay provider code | Vendor review/security docs |
| Resend | Transactional email | User/contact emails, templates | MEDIUM | Email service code | DPA/SOC/security review |
| GitHub | Source code/CI/secrets | Code, CI logs, repo secrets | HIGH | Actions workflow | Branch protection, MFA, access review, secret scanning evidence |

## 16. Data Inventory

| Data Category | Sensitivity | Storage | Third Parties | Retention Known? | Deletion Mechanism Known? |
| --- | --- | --- | --- | --- | --- |
| Auth data | HIGH | `users`, `auth_tokens`; BCrypt password hash, hashed reset/verification tokens | Email provider for links | PARTIAL | App token invalidation; account deletion process partial |
| Profile data | MEDIUM | `users`, admin views | Resend, Stripe for relevant flows | PARTIAL | SQL cleanup exists; production process missing |
| Trading data | HIGH | `trading_accounts`, `trades`, imports, replay tables | OpenAI/Qdrant/Databento for features | PARTIAL | User-scoped deletes exist in places; full process not formalized |
| Journal content | HIGH | `journal_entries`, AI prompts/results | OpenAI | NO | Cleanup script exists; production retention/deletion workflow needed |
| AI analysis | HIGH | AI review/reflection/coach/DNA tables | OpenAI | PARTIAL | Delete/recompute paths partial; retention policy needed |
| Billing metadata | HIGH | `users`, `ai_credit_purchases`, `processed_stripe_events`, Stripe checkout/subscription state | Stripe | PARTIAL | Runbooks exist; legal retention policy needed |
| Telemetry/SEO analytics | MEDIUM | feature usage, SEO acquisition tables | Possibly none beyond hosting/logs | PARTIAL | `docs/SEO_ANALYTICS.md` recommends retention; enforcement not proven |
| Logs/observability | MEDIUM/HIGH | Log files, observability tables, platform logs | Render/GitHub/providers | PARTIAL | Cleanup services exist; centralized retention evidence missing |

## 17. Required Policies

| Policy | Status | Priority | SOC 2 Relevance | Recommendation |
| --- | --- | --- | --- | --- |
| Information Security Policy | MISSING | P0 | CC1, CC5 | Create founder-approved umbrella policy. |
| Access Control Policy | MISSING | P0 | CC6 | Define least privilege, MFA, approvals, reviews, offboarding. |
| Password/Auth Policy | PARTIALLY IMPLEMENTED | P1 | CC6 | Document password/JWT/reset behavior and admin MFA requirement. |
| Acceptable Use Policy | PARTIALLY IMPLEMENTED | P2 | CC1 | Terms exist; internal acceptable use missing. |
| Change Management Policy | MISSING | P0 | CC8 | Define PR review, CI, migration, deployment approval controls. |
| Incident Response Plan | MISSING | P0 | CC7 | Create severity, roles, timeline, comms, postmortem process. |
| Business Continuity Plan | MISSING | P1 | CC7, CC9 | Define continuity for hosting/DB/vendor outages. |
| Disaster Recovery Plan | MISSING | P1 | CC7, CC9 | Define RTO/RPO, restore drill cadence. |
| Backup/Restore Policy | MISSING | P0 | CC7, Availability | Enable and test Supabase backup recovery. |
| Vendor Risk Policy | MISSING | P0 | CC9 | Vendor inventory, review cadence, DPAs/SOC reports. |
| Risk Assessment Policy | MISSING | P0 | CC3 | Define risk register cadence and owners. |
| Vulnerability Management Policy | MISSING | P1 | CC6, CC7 | Define scan cadence and remediation SLAs. |
| Secure SDLC Policy | PARTIALLY IMPLEMENTED | P1 | CC8 | CI exists; formal review/security requirements needed. |
| Data Classification Policy | MISSING | P1 | Confidentiality | Classify trading/journal/AI/billing data. |
| Retention/Deletion Policy | MISSING | P1 | Confidentiality, CC7 | Define retention windows and deletion procedures. |
| Confidentiality Policy | MISSING | P1 | Confidentiality | Define handling of customer trading data and vendor sharing. |
| Logging/Monitoring Policy | PARTIALLY IMPLEMENTED | P1 | CC4, CC7 | Observability exists; alert response and retention policy needed. |
| Employee Onboarding/Offboarding | MISSING | P0 | CC1, CC6 | Required for access control evidence. |
| Security Awareness Training | MISSING | P1 | CC1 | Annual training and completion evidence. |
| Access Review Procedure | MISSING | P0 | CC6 | Quarterly review across all privileged systems. |
| Production Deployment Procedure | PARTIALLY IMPLEMENTED | P1 | CC8 | Stripe/DB docs exist; formal approval evidence needed. |
| Security Incident Runbook | MISSING | P0 | CC7 | Operationalize incident response. |
| Breach Notification Procedure | MISSING | P0 | CC7, Confidentiality | Define customer/regulatory communication timeline. |

## 18. Evidence Needed for Auditor

| Evidence Type | Examples |
| --- | --- |
| Governance | Approved policies, control owner matrix, risk register, management review notes. |
| Access | GitHub/Render/Vercel/Supabase/Stripe/OpenAI/Qdrant/Databento/Resend user lists, MFA settings, access approvals, access review signoff, offboarding samples. |
| Change management | PRs with review, CI results, deployment approvals, release notes, migration logs. |
| Security testing | `mvn test` reports, frontend build/test reports, PostgreSQL baseline CI results, SAST/dependency scan reports, secret scan results. |
| Operations | Monitoring dashboards, alert rules, incident/ticket samples, postmortems, on-call schedule. |
| Backup/DR | Supabase backup settings, restore drill evidence, RTO/RPO approval, DR tabletop. |
| Vendor risk | Vendor register, SOC 2 reports/bridging letters/DPAs/security reviews. |
| Billing | Stripe webhook config, event logs, purchase ledger reconciliation, live smoke-test evidence. |
| Confidentiality | Data inventory, subprocessors, retention/deletion workflow evidence, user request samples. |

## 19. Remediation Roadmap

### PHASE 0 - Critical Blockers

| Priority | Remediation | Effort | Owner Type | Dependencies |
| --- | --- | --- | --- | --- |
| P0 | Create and approve core policy set: InfoSec, Access Control, Change Management, Incident Response, Backup/Restore, Vendor Risk, Risk Assessment, Retention/Deletion | L | FOUNDER / OPERATIONS / AUDITOR | Policy templates and auditor expectations |
| P0 | Complete privileged access inventory and initial access review | M | FOUNDER / OPERATIONS | GitHub, Render, Vercel, Supabase, Stripe, OpenAI, Qdrant, Databento, Resend access |
| P0 | Configure/verify production backups and run restore drill | M | ENGINEERING / OPERATIONS | Supabase production setup |
| P0 | Enable external alert routing and define incident escalation | M | ENGINEERING / OPERATIONS | Monitoring provider selection |

### PHASE 1 - Before Readiness Review

| Priority | Remediation | Effort | Owner Type | Dependencies |
| --- | --- | --- | --- | --- |
| P1 | Add distributed/edge rate limiting for auth, billing, import, and AI generation | M | ENGINEERING | Render/edge provider |
| P1 | Add dependency scanning, SAST, and secret scanning evidence | M | ENGINEERING | GitHub settings/tools |
| P1 | Add admin MFA or documented SSO/MFA gate | M | ENGINEERING / OPERATIONS | Auth strategy |
| P1 | Complete vendor risk register and collect SOC reports/DPAs | M | OPERATIONS | Vendor portals |
| P1 | Document data classification and AI/vendor data flows | M | FOUNDER / OPERATIONS | Product/privacy commitments |

### PHASE 2 - Before Type I

| Priority | Remediation | Effort | Owner Type | Dependencies |
| --- | --- | --- | --- | --- |
| P2 | Formalize production deployment approvals and branch protection evidence | S | ENGINEERING / OPERATIONS | GitHub/Render settings |
| P2 | Define log retention and centralized logging | M | ENGINEERING | Logging provider |
| P2 | Test user deletion/export process across PostgreSQL and Qdrant | M | ENGINEERING | Retention policy |
| P2 | Run incident response tabletop and document corrective actions | S | OPERATIONS | IR plan |

### PHASE 3 - During Type II Observation Period

| Priority | Remediation | Effort | Owner Type | Dependencies |
| --- | --- | --- | --- | --- |
| P3 | Perform recurring access reviews | S recurring | OPERATIONS | Access inventory |
| P3 | Perform recurring vendor reviews | S recurring | OPERATIONS | Vendor register |
| P3 | Review alerts/incidents and retain evidence | S recurring | ENGINEERING / OPERATIONS | Monitoring |
| P3 | Track vulnerability remediation SLAs | M recurring | ENGINEERING | Scanning tools |
| P3 | Retain change-management samples for each release | S recurring | ENGINEERING | PR/deploy workflow |

## 20. Final Assessment

Is TradePilot ready to begin a SOC 2 Type I audit today? NO.

Why: the technical codebase is substantially prepared for a Security/Common Criteria audit, but the operating controls that auditors test are not sufficiently documented or evidenced. The biggest blockers are formal policies, access reviews, vendor risk reviews, backup/restore proof, incident response procedures, external monitoring evidence, and change-management approval evidence.

TradePilot is a reasonable candidate for an auditor-led readiness review once Phase 0 items are completed. For an initial audit, pursue Security first with Confidentiality if the data-classification and vendor-processing evidence can be completed quickly.
