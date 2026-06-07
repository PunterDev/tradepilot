# TradePilot Deployment Readiness Checklist

Use this checklist before every production deployment. TradePilot uses Spring Boot 3.3.5, Java 17, React + Vite + TypeScript, Supabase Postgres, JWT auth, Stripe billing, OpenAI, SMTP email, admin tooling, observability, and Playwright smoke tests.

## 1. Required Environment Variables

### Backend

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `DATABASE_URL`
- [ ] `DATABASE_USERNAME`
- [ ] `DATABASE_PASSWORD`
- [ ] `JWT_SECRET`
- [ ] `JWT_EXPIRATION_MINUTES`
- [ ] `OPENAI_API_KEY`
- [ ] `STRIPE_SECRET_KEY`
- [ ] `STRIPE_WEBHOOK_SECRET`
- [ ] `STRIPE_PRO_PRICE_ID`
- [ ] `SMTP_HOST`
- [ ] `SMTP_PORT`
- [ ] `SMTP_USERNAME`
- [ ] `SMTP_PASSWORD`
- [ ] `ADMIN_BOOTSTRAP_EMAIL`
- [ ] `CORS_ALLOWED_ORIGINS`
- [ ] `JPA_DDL_AUTO=validate`
- [ ] `LOG_FILE`

### Frontend

- [ ] `VITE_API_BASE_URL`
- [ ] `VITE_IDLE_TIMEOUT_MINUTES`
- [ ] `VITE_IDLE_WARNING_MINUTES`

## 2. Supabase Database Checklist

- [ ] Review [SQL_RUN_ORDER.md](./SQL_RUN_ORDER.md) before running migrations.
- [ ] Run required base schema files in the documented order.
- [ ] Run feature-specific SQL files for contact messages, feature usage, session reviews, and Trading DNA.
- [ ] Run observability SQL after base and feature tables exist.
- [ ] Apply repair/compatibility SQL files during a quiet maintenance window for existing databases.
- [ ] Confirm Trading DNA performance SQL is applied.
- [ ] Confirm Stripe webhook idempotency table exists: `processed_stripe_events`.
- [ ] Verify indexes for users, accounts, trades, feature usage, observability, billing, and Trading DNA tables.
- [ ] Verify `ai_usage_events` allows `SESSION_REVIEW`.
- [ ] Enable Supabase backups and PITR if available for the plan.
- [ ] Complete at least one restore drill in a non-production environment.
- [ ] Export a pre-deployment backup before applying new SQL to production.

## 3. Security Checklist

- [ ] `backend/src/main/resources/application-dev.yml` remains gitignored and is never deployed.
- [ ] No secrets are committed in tracked files.
- [ ] Production profile is active.
- [ ] H2 console is disabled in production.
- [ ] Dev tools and dev auth are disabled in production.
- [ ] Frame protection is enabled in production.
- [ ] CORS is restricted to the production frontend domain.
- [ ] JWT secret is strong, random, and long enough for HMAC signing.
- [ ] JWT expiration is configured intentionally.
- [ ] Rate limiting is enabled for auth, reset, contact, and AI generation endpoints.
- [ ] Generic safe 500 handler is enabled and returns request IDs.
- [ ] Logs do not expose passwords, JWTs, reset tokens, verification tokens, OpenAI keys, Stripe secrets, raw prompts, or raw email bodies.
- [ ] Admin endpoints are accessible only to users with `ADMIN` role.
- [ ] Manual admin bootstrap email is correct and removed/rotated after first admin is confirmed if desired.

## 4. Stripe Checklist

- [ ] Production Stripe secret key configured.
- [ ] Correct production Pro price ID configured.
- [ ] Stripe webhook endpoint configured to backend `/api/stripe/webhook`.
- [ ] `STRIPE_WEBHOOK_SECRET` configured from the production webhook endpoint.
- [ ] Webhook signature verification confirmed.
- [ ] Webhook idempotency verified with duplicate event replay.
- [ ] Customer portal configured in Stripe dashboard.
- [ ] Successful Free to Pro upgrade tested in production-like environment.
- [ ] Downgrade/cancel flow tested from customer portal.
- [ ] Failed payment and past-due behavior tested or reviewed.
- [ ] Duplicate webhook replay does not double-update subscription state.
- [ ] Stripe event failures appear in observability dashboard.

## 5. OpenAI / AI Cost Checklist

- [ ] `OPENAI_API_KEY` configured.
- [ ] AI model configuration reviewed for current cost/performance.
- [ ] Usage limits active for Free and Pro plans.
- [ ] AI usage event tracking active.
- [ ] AI request metrics and estimated costs are recorded.
- [ ] Cached AI responses return before usage checks where intended.
- [ ] Force refresh consumes usage and is rate limited.
- [ ] OpenAI failures return safe user-facing errors.
- [ ] Cost spike monitoring plan defined.
- [ ] Admin observability can show daily/monthly AI cost.

## 6. Email Checklist

- [ ] Production SMTP credentials configured.
- [ ] `MAIL_FROM` or production sender configured and verified.
- [ ] Verification email sends and link works.
- [ ] Password reset email sends and link works.
- [ ] Contact form notification sends to support inbox.
- [ ] Admin contact reply flow tested.
- [ ] Email failures are logged and stored in observability/contact CRM.
- [ ] Raw verification/reset tokens are never logged.
- [ ] Sender domain SPF/DKIM/DMARC reviewed.

## 7. Backend Deployment Checklist

- [ ] `mvn clean test` passes.
- [ ] Production build/package command succeeds.
- [ ] Java 17 runtime available on deployment target.
- [ ] `spring.jpa.hibernate.ddl-auto` resolves to `validate`.
- [ ] Database connection uses production Supabase credentials.
- [ ] Health endpoint is available if deployment platform requires it.
- [ ] Logs directory is writable.
- [ ] Rolling log file writes to `LOG_FILE` path.
- [ ] Request IDs appear in console/file logs.
- [ ] Scheduled jobs are safe for the production instance count.
- [ ] Observability cleanup job enabled.
- [ ] Observability alert evaluator enabled.
- [ ] Stripe webhook endpoint reachable publicly over HTTPS.
- [ ] Public auth/contact endpoints are rate limited.

## 8. Frontend Deployment Checklist

- [ ] `npm run build` passes.
- [ ] `VITE_API_BASE_URL` points to production backend HTTPS URL.
- [ ] SPA route fallback is configured on the hosting platform.
- [ ] Login, protected routes, and public marketing routes work after refresh.
- [ ] Idle timeout values are configured.
- [ ] 401 auto logout verified.
- [ ] Security headers configured at hosting/CDN layer:
  - [ ] Content-Security-Policy
  - [ ] Strict-Transport-Security
  - [ ] X-Frame-Options
  - [ ] Referrer-Policy
  - [ ] Permissions-Policy
- [ ] Static assets are served with appropriate cache headers.
- [ ] Production source maps policy reviewed.

## 9. E2E Smoke Test Checklist

Run the starter suite and the manual smoke plan in [E2E_SMOKE_TEST_PLAN.md](./E2E_SMOKE_TEST_PLAN.md).

- [ ] Login
- [ ] Dashboard
- [ ] Accounts
- [ ] CSV import
- [ ] Analytics heatmap
- [ ] Trade detail
- [ ] AI review
- [ ] Daily Coach
- [ ] Weekly Coach
- [ ] Trading DNA
- [ ] Session Review
- [ ] Billing usage
- [ ] Admin access control
- [ ] Admin observability
- [ ] Rate limiting
- [ ] Expired auth redirect

## 10. Observability Checklist

- [ ] API request metrics tracked.
- [ ] Slow requests tracked.
- [ ] Error rate tracked.
- [ ] AI cost tracked.
- [ ] Cache hit/miss tracked.
- [ ] Stripe events tracked.
- [ ] SMTP events tracked.
- [ ] Query performance events use logical names only, not raw SQL.
- [ ] Alerts are created by scheduled evaluator.
- [ ] Alerts are resolvable from admin UI.
- [ ] Log file is created at configured path.
- [ ] Request ID visible in logs and safe 500 responses.
- [ ] Admin observability dashboard loads without 500s.
- [ ] Cleanup job retention windows reviewed.

## 11. Production Go / No-Go Criteria

- [ ] All P0 issues resolved.
- [ ] `mvn clean test` passes.
- [ ] `npm run build` passes.
- [ ] Playwright smoke tests pass or skips are explicitly explained.
- [ ] No 500s during smoke test.
- [ ] Stripe webhook verified.
- [ ] Database backup/PITR enabled.
- [ ] Production secrets configured and rotated if previously exposed.
- [ ] Frontend production URL works.
- [ ] Backend production URL works.
- [ ] Admin login and admin observability verified.
- [ ] Email verification and password reset verified.
- [ ] Go/No-Go decision recorded.

## 12. Rollback Plan

- [ ] Frontend rollback: keep previous static build artifact or hosting deployment available.
- [ ] Backend rollback: keep previous deployable artifact/image and environment config available.
- [ ] Database migration caution: avoid destructive migrations; if additive migration has run, confirm old backend remains compatible before rollback.
- [ ] Stripe webhook rollback: ensure only one active production endpoint points to the intended backend version.
- [ ] Emergency disable AI features: remove/disable OpenAI key or feature-toggle AI generation if available; cached reads should remain viewable.
- [ ] Emergency disable billing actions: remove/disable Stripe secret or block checkout endpoint at gateway if needed.
- [ ] Emergency admin access: verify at least one production admin can log in after rollback.
- [ ] Preserve logs and observability data for incident review.

## 13. Final Sign-Off Template

- Date:
- Version / commit hash:
- Deployed by:
- Backend test result:
- Frontend build result:
- E2E result:
- Database migration result:
- Stripe verification:
- Email verification:
- Observability verification:
- Security review:
- Backup verified:
- Go / No-Go decision:
- Notes / follow-ups:
