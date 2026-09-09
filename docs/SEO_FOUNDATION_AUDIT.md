# TradePilot SEO Foundation Audit

Date: 2026-08-30

## Current Architecture

- Frontend is a Vite React SPA using React Router in `frontend/src/App.tsx`.
- `react-helmet-async` is already installed and wrapped in `HelmetProvider` from `frontend/src/main.tsx`.
- Public marketing pages live under `frontend/src/features/marketing`.
- The homepage already used a reusable `Seo` component, structured data, and the shared marketing layout/styles.
- Vercel deploys the SPA with a catch-all rewrite to `/index.html` in `frontend/vercel.json`.
- There is no server-side rendering or static prerendering for public marketing routes today.

## Existing SEO Surface Before This Work

- `frontend/src/components/Seo.tsx` supported title, description, canonical path, Open Graph title/description/url/image, Twitter image/card, and one JSON-LD object.
- `frontend/public/robots.txt` allowed all routes and referenced a `www.tradepilotcoach.com` sitemap.
- `frontend/public/sitemap.xml` existed, but it included `/login` and `/register` and did not include high-intent SEO pages.
- Homepage copy already positioned TradePilot as an AI trading journal, but the near-hero copy could more explicitly mention futures traders, prop-firm traders, and CSV import sources.
- Legal/footer pages already existed: `/privacy`, `/terms`, and `/contact`.

## Import Claim Audit

- Topstep support is implemented through `orders_export.csv` order-history exports.
- Topstep import reconstructs completed trades from filled opening/closing executions and rejects multiple `AccountName` values in one file for MVP safety.
- Topstep duplicate keys are prefixed with `TOPSTEP`.
- Tradovate support is implemented through TradePilot-compatible CSV fields including `symbol`, `qty`, `buyPrice`, `sellPrice`, `pnl`, `boughtTimestamp`, and `soldTimestamp`.
- Tradovate duplicate protection uses deterministic external trade keys.
- There is no direct Topstep or Tradovate API connection claimed by this implementation.

## Implemented In This Pass

- Expanded `Seo` to support robots directives, canonical overrides, Open Graph overrides, and Twitter card selection.
- Standardized public canonical URLs and sitemap URLs on `https://tradepilotcoach.com`.
- Added five public high-intent SEO landing pages:
  - `/futures-trading-journal`
  - `/topstep-trading-journal`
  - `/tradovate-trading-journal`
  - `/prop-firm-trading-journal`
  - `/ai-trading-journal`
- Added per-page SoftwareApplication and FAQ structured data without ratings, review counts, testimonials, or invented social proof.
- Added first-touch UTM/referrer/landing-page preservation in localStorage.
- Added SEO page-view and CTA-click event pushes to `window.dataLayer` when available, plus a local debug event buffer.
- Added a public not-found page with `noindex,follow` for unknown non-private routes.
- Updated sitemap to include public indexable pages and exclude login/register/private app routes.
- Updated robots hints for authenticated app, admin, billing, and user-specific routes.

## 2026-08-30 Extension: Prerendering and Behavioral Cluster

- Added a post-build prerender step for known public marketing routes.
- `npm run build` now runs TypeScript, Vite production build, and `scripts/prerender-public-pages.mjs`.
- The prerenderer serves the production `dist` build locally, visits each public route with a build-only `__prerender` query flag, serializes the rendered HTML, and writes route-specific files such as `dist/revenge-trading/index.html`.
- The generated route HTML is validated for title, meta description, canonical URL, H1, meaningful body copy, and structured data where appropriate.
- The build-only prerender flag skips the initial anonymous auth-loading check for public pages. It does not change normal user routing or production URLs.
- Added public route manifest: `frontend/seo-public-routes.json`.
- Added behavioral SEO cluster:
  - `/trading-psychology`
  - `/revenge-trading`
  - `/overtrading`
  - `/trading-discipline`
  - `/breaking-trading-rules`
  - `/trading-after-losses`
- Updated footer links into Product, Journals, Behavior, and Company groups for crawlable but restrained navigation.
- Added a Vercel host redirect from `www.tradepilotcoach.com` to `https://tradepilotcoach.com`.
- Updated sitemap with all behavioral routes and the hub.

## 404 and Host Notes

- Known prerendered marketing routes now have static `index.html` files in `dist`.
- Known authenticated app routes still rely on the SPA fallback so deep links continue to work.
- Unknown public URLs still depend on Vercel's catch-all rewrite and may return HTTP 200 at the edge, but the React app renders a `noindex,follow` not-found page.
- A true HTTP 404 for unknown public routes would require replacing the broad catch-all rewrite with more explicit Vercel routing or a lightweight serverless/static routing layer that preserves authenticated SPA deep links.
- Canonical host is `https://tradepilotcoach.com`. The repo now includes a Vercel redirect rule for `www.tradepilotcoach.com`, but final behavior depends on the production Vercel domain configuration.

## Follow-Up Recommendations

- Consider a more explicit Vercel routing strategy for true HTTP 404s on unknown public routes while preserving authenticated app deep links.
- If backend analytics should persist SEO attribution, add a dedicated acquisition table or extend existing feature usage metadata intentionally rather than overloading authenticated feature usage events.
- Consider future behavior-specific calculators only when they can provide useful, accurate calculations without implying trading outcomes.
