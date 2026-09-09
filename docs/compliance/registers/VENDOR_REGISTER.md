# Vendor Register

This register is seeded only with vendors confirmed by repository code or documentation. Security report status, DPA status, and risk ratings require manual review.

| Vendor | Service | Data Shared | Data Classification | Criticality | Security Dependency | SOC 2 / Security Report Status | DPA Required? | Last Review | Next Review | Risk Rating | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| GitHub | Source control and CI | Source code, CI logs, repository secrets metadata | Restricted | High | Code integrity, CI, access control | REVIEW REQUIRED | REVIEW REQUIRED | Pending | Pending | REVIEW REQUIRED | Branch protection, MFA, secret scanning evidence required. |
| Vercel | Frontend hosting | Public frontend, frontend env vars, deployment logs | Confidential for operational metadata | High | Public web availability and TLS | REVIEW REQUIRED | REVIEW REQUIRED | Pending | Pending | REVIEW REQUIRED | Verify access, MFA, TLS, headers, deploy controls. |
| Render | Backend hosting | Runtime env, backend logs, app traffic | Restricted | High | API availability and secret hosting | REVIEW REQUIRED | REVIEW REQUIRED | Pending | Pending | REVIEW REQUIRED | Verify env secrets, access, logs, deploy rollback. |
| Supabase | PostgreSQL database | User, trade, journal, AI, billing, observability data | Restricted | Critical | Primary system of record | REVIEW REQUIRED | REVIEW REQUIRED | Pending | Pending | REVIEW REQUIRED | Backup/PITR and restore evidence required. |
| Stripe | Payments and billing | Customer email, payment/customer/subscription metadata | Restricted | Critical | Billing source of truth | REVIEW REQUIRED | REVIEW REQUIRED | Pending | Pending | REVIEW REQUIRED | Live webhook/config and access evidence required. |
| OpenAI | AI analysis provider | Trade/journal/analysis prompts and outputs | Confidential | High | AI feature generation | REVIEW REQUIRED | REVIEW REQUIRED | Pending | Pending | REVIEW REQUIRED | Data retention/settings and DPA review required. |
| Qdrant | Vector store | Trading memory vectors and metadata | Confidential | High | Trading Memory retrieval | REVIEW REQUIRED | REVIEW REQUIRED | Pending | Pending | REVIEW REQUIRED | Collection/access/deletion evidence required. |
| Databento | Market data provider | Contract symbols, timestamps, request metadata | Internal/Confidential | Medium | Trade Replay market data | REVIEW REQUIRED | REVIEW REQUIRED | Pending | Pending | REVIEW REQUIRED | Cost-control and vendor security review required. |
| Resend | Transactional email | Recipient emails, verification/reset/contact templates | Confidential | High | Account verification, password reset, support email | REVIEW REQUIRED | REVIEW REQUIRED | Pending | Pending | REVIEW REQUIRED | Domain auth and email delivery evidence required. |
