# TradePilot SOC 2 Evidence Checklist

Use this as an evidence collection checklist for a future readiness review. Do not store live secrets in evidence artifacts.

## Phase 2 Draft Artifacts

- [ ] Management-approved versions of draft policies in `docs/compliance/policies/`.
- [ ] Completed Type I gate review using `docs/compliance/SOC2_TYPE1_READINESS_GATE.md`.
- [ ] Evidence index updated with real evidence locations in `docs/compliance/SOC2_EVIDENCE_INDEX.md`.
- [ ] Management checklist reviewed and assigned using `docs/compliance/SOC2_MANAGEMENT_CHECKLIST.md`.
- [ ] Draft procedures reviewed for operational fit before use.

## Governance

- [ ] Approved Information Security Policy.
- [ ] Approved Access Control Policy.
- [ ] Approved Authentication Policy.
- [ ] Approved Acceptable Use Policy.
- [ ] Approved Change Management Policy.
- [ ] Approved Incident Response Plan.
- [ ] Approved Business Continuity Policy.
- [ ] Approved Disaster Recovery Policy.
- [ ] Approved Backup/Restore Policy.
- [ ] Approved Vendor Risk Policy.
- [ ] Approved Risk Assessment Policy.
- [ ] Approved Vulnerability Management Policy.
- [ ] Approved Secure SDLC Policy.
- [ ] Approved Data Classification and Confidentiality Policy.
- [ ] Approved Retention/Deletion Policy.
- [ ] Approved Logging/Monitoring Policy.
- [ ] Approved Personnel Security Policy.
- [ ] Approved Access Review Policy.
- [ ] Approved Security Awareness Policy.
- [ ] Approved Production Access Policy.
- [ ] Security owner and control owner matrix.
- [ ] Risk register with owner, severity, treatment, and review date.
- [ ] Initial risk assessment signoff.
- [ ] Security awareness training completion records.
- [ ] Employee/contractor onboarding checklist sample.
- [ ] Employee/contractor offboarding checklist sample.
- [ ] Incident response tabletop notes and action items.
- [ ] Quarterly access review schedule and first completed review.
- [ ] Vulnerability remediation SLA.

## GitHub

- [ ] Organization/repository user access export.
- [ ] Admin list and least-privilege review.
- [ ] MFA/SSO enforcement screenshot or export.
- [ ] Branch protection settings for production branch.
- [ ] Required status checks list, including backend tests and PostgreSQL Flyway baseline gate.
- [ ] Pull request sample with approval and passing CI.
- [ ] Deployment/release change evidence sample.
- [ ] Secret scanning setting evidence.
- [ ] Dependabot/Renovate or dependency-alert evidence.
- [ ] CodeQL/SAST or equivalent scan evidence.
- [ ] GitHub Actions run for `postgres-flyway-baseline` showing 1 run, 0 failures, 0 errors, 0 skipped.

## Vercel

- [ ] User/team access export.
- [ ] MFA/SSO setting evidence.
- [ ] Production project environment variable list with values redacted.
- [ ] `VITE_API_BASE_URL` production value evidence.
- [ ] Deployment history and rollback evidence.
- [ ] Domain/TLS configuration evidence.
- [ ] Security headers/CSP/HSTS configuration evidence, if configured at Vercel.
- [ ] Frontend build/test CI evidence.

## Render

- [ ] User/team access export.
- [ ] MFA/SSO setting evidence.
- [ ] Production service environment variable list with values redacted.
- [ ] Backend deployment logs showing startup validation passed.
- [ ] Health check configuration and recent health check evidence.
- [ ] Deployment approval/rollback evidence.
- [ ] Log retention and log access control evidence.
- [ ] Alert routing configuration for production backend.

## Supabase

- [ ] Production project access export.
- [ ] MFA/SSO/access controls evidence.
- [ ] Database connection restrictions, if configured.
- [ ] Backup/PITR configuration screenshot.
- [ ] Restore drill evidence to non-production database.
- [ ] Flyway migration logs for V001 through V006.
- [ ] `flyway_schema_history` verification output.
- [ ] `backend/db/verify_production_schema.sql` output.
- [ ] Quarterly access review evidence.
- [ ] Encryption/TLS/security documentation evidence.

## Stripe

- [ ] Live Products/Prices screenshots or exports.
- [ ] Live webhook endpoint configuration.
- [ ] Live webhook signing secret stored in Render with value redacted.
- [ ] Required webhook event list matches `docs/STRIPE_LIVE_WEBHOOK_EVENTS.md`.
- [ ] Customer Portal settings and return URL evidence.
- [ ] Live smoke-test evidence for Starter credit pack.
- [ ] Live smoke-test evidence for Pro monthly subscription.
- [ ] Ledger reconciliation evidence for smoke tests.
- [ ] Refund/dispute support runbook evidence.
- [ ] Stripe access list and MFA evidence.
- [ ] Stripe SOC report/DPA/security review evidence.

## OpenAI

- [ ] Account/project access export.
- [ ] API key storage evidence with value redacted.
- [ ] DPA/security documentation review.
- [ ] Data retention/settings evidence, if available.
- [ ] Model/configuration approval evidence.
- [ ] AI data-flow documentation showing what customer data is sent.
- [ ] Usage/cost monitoring evidence.

## Qdrant

- [ ] Account/project access export.
- [ ] API key storage evidence with value redacted.
- [ ] Collection configuration evidence.
- [ ] Payload index/collection validation evidence.
- [ ] DPA/security documentation review.
- [ ] User/trade memory deletion verification sample.
- [ ] Backup/availability evidence if Qdrant is in production path.

## Databento

- [ ] Account access export.
- [ ] API key storage evidence with value redacted.
- [ ] Cost-control configuration evidence.
- [ ] Vendor security documentation review.
- [ ] Usage monitoring evidence.

## Resend

- [ ] Account access export.
- [ ] API key storage evidence with value redacted.
- [ ] Domain/DKIM/SPF/DMARC configuration evidence.
- [ ] Transactional email delivery logs sample.
- [ ] Vendor security documentation review.
- [ ] Email template/link redaction evidence.

## Application

- [ ] Backend test report for `mvn test`.
- [ ] Frontend test/typecheck/build report.
- [ ] Security tests: auth, RBAC, authorization, abuse, JWT, Stripe, production validators.
- [ ] Evidence that `/api/auth/me` requires authentication.
- [ ] Evidence that admin endpoints require ADMIN.
- [ ] Evidence that cross-user resources return 404.
- [ ] Evidence that dev auth/tools are disabled in production.
- [ ] Evidence that production CORS rejects localhost/wildcards.
- [ ] Evidence that production frontend URL cannot be localhost.
- [ ] Evidence that JWT secret validation rejects missing/weak secrets.
- [ ] Evidence that Stripe production validator rejects test config.
- [ ] Observability dashboard screenshot.
- [ ] Request ID/correlation log sample.
- [ ] Log redaction review sample.

## Database

- [ ] Flyway migration files V001-V006 checksums.
- [ ] `flyway_schema_history` output.
- [ ] Schema verification output from `backend/db/verify_production_schema.sql`.
- [ ] Unique index evidence for users Stripe customer/subscription IDs.
- [ ] Unique index evidence for processed Stripe event IDs.
- [ ] Unique index evidence for checkout session/payment intent ledger IDs.
- [ ] Evidence obsolete purchased-credit nonnegative constraint is absent.
- [ ] Backup restore verification query output.
- [ ] Data retention/deletion workflow test evidence.
- [ ] Database access review evidence.

## Monitoring

- [ ] Uptime monitor configuration.
- [ ] Health check alert test.
- [ ] Error-rate alert test.
- [ ] Stripe webhook failure alert test.
- [ ] Email delivery failure alert test.
- [ ] AI cost threshold alert test.
- [ ] Slow request/slow query alert evidence.
- [ ] Alert acknowledgement and escalation evidence.
- [ ] Incident ticket/postmortem sample.

## Evidence Templates To Complete

- [ ] `docs/compliance/evidence/templates/ACCESS_REVIEW_TEMPLATE.md`
- [ ] `docs/compliance/evidence/templates/VENDOR_REVIEW_TEMPLATE.md`
- [ ] `docs/compliance/evidence/templates/INCIDENT_RECORD_TEMPLATE.md`
- [ ] `docs/compliance/evidence/templates/RESTORE_TEST_TEMPLATE.md`
- [ ] `docs/compliance/evidence/templates/CHANGE_RECORD_TEMPLATE.md`
- [ ] `docs/compliance/evidence/templates/SECURITY_TRAINING_RECORD.md`
