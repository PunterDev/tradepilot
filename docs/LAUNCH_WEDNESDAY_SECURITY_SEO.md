# Wednesday Security and SEO Launch Checklist

Canonical public host: `https://www.tradepilotcoach.com`.

## Automated Repository Controls

- Vercel redirects `tradepilotcoach.com` to the canonical `www` host.
- The backend production CORS allowlist defaults to `https://www.tradepilotcoach.com`; production startup rejects localhost and a missing canonical origin.
- Production startup fails when dev auth or dev tools are enabled, shared database-backed rate limiting is not configured, or the required admin access protection declaration is missing.
- `/api/admin/**` is protected by backend `ADMIN` RBAC. Dev and E2E helpers are disabled unless the local dev gate is enabled.
- Private API responses receive Spring Security's `no-cache, no-store, max-age=0, must-revalidate` response policy.
- Vercel provides browser-facing CSP, HSTS, nosniff, referrer, permissions, and anti-framing headers. The backend independently protects API responses with the corresponding security headers and an API-safe CSP.
- `robots.txt` allows public pages and disallows private app paths. It is crawl guidance, not authorization.
- The sitemap contains public canonical URLs only. Authenticated pages render `noindex,nofollow` metadata.

## Production Verification

Run these after the Vercel deployment and backend deployment are live:

```bash
curl -sSI https://www.tradepilotcoach.com
curl -sSI https://tradepilotcoach.com
curl -sSI http://tradepilotcoach.com
curl -s https://www.tradepilotcoach.com/robots.txt
curl -s https://www.tradepilotcoach.com/sitemap.xml
curl -s https://www.tradepilotcoach.com/futures-trading-journal | rg 'canonical|application/ld\\+json'
```

Confirm every non-canonical host returns a permanent redirect to the equivalent `https://www.tradepilotcoach.com/...` URL. Confirm the canonical public response includes `Content-Security-Policy`, `Strict-Transport-Security`, `X-Content-Type-Options`, `Referrer-Policy`, and `Permissions-Policy`.

In the production backend environment, explicitly set and verify:

```text
SPRING_PROFILES_ACTIVE=production
APP_ALLOWED_ORIGINS=https://www.tradepilotcoach.com
APP_FRONTEND_URL=https://www.tradepilotcoach.com
DEV_AUTH_ENABLED=false
DEV_TOOLS_ENABLED=false
RATE_LIMIT_STORE=database
APP_ADMIN_ACCESS_PROTECTION=external-mfa
```

Use a normal-user token to confirm `/api/admin/users` returns `403`, and that `/api/dev/seed-demo-data`, `/api/dev/demo-data/reseed`, and `/api/dev/e2e/prepare` are not usable. With two non-admin accounts, confirm guessed account and trade identifiers return the existing `404` ownership response. Do not place tokens in shell history or tickets.

In Search Console, inspect the canonical homepage, each journal landing page, and each calculator; request indexing for priority public URLs; verify private URLs are excluded/noindexed; then review Coverage and Mobile Usability after crawl data arrives.

Priority URLs: `/`, `/futures-trading-journal`, `/ai-trading-journal`, `/prop-firm-trading-journal`, `/tools/trading-expectancy-calculator`, `/tools/overtrading-calculator`, `/tools/revenge-trading-calculator`, `/privacy`, `/terms`, and `/contact`.

## Release Gate

The repository is ready for production verification once the automated suite is green. Do not launch until production DNS/Vercel redirects, deployed headers, and required environment values have been confirmed.

Credential-hygiene reassessment: the local `backend/src/main/resources/application-dev.yml` is ignored, untracked, and absent from reachable Git history. No real provider credential was found in tracked files or reachable history; tracked configuration uses environment-variable references, placeholders, or test-only synthetic values. Repository-history cleanup and credential rotation are not release gates for this finding.
