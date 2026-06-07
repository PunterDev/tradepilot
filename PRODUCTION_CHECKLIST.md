# TradePilot Production Checklist

## Configuration Safety

- `backend/src/main/resources/application-dev.yml` is local-only and must stay gitignored.
- Use `backend/src/main/resources/application-dev.example.yml` for placeholder development configuration.
- Do not commit real database passwords, SMTP app passwords, JWT secrets, Stripe keys, webhook secrets, or OpenAI keys.
- If any secret was ever committed, rotate it before launch.

## Required Environment Variables

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `OPENAI_API_KEY`
- `APP_FRONTEND_URL`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `CONTACT_EMAIL`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `STRIPE_PRO_PRICE_ID`
- `STRIPE_PRO_YEARLY_PRICE_ID`
- `APP_ADMIN_EMAIL`
- `JWT_EXPIRATION_MINUTES` default `30`
- `VITE_IDLE_TIMEOUT_MINUTES` default `30`
- `VITE_IDLE_WARNING_MINUTES` default `2`

## Session Timeout

- Access tokens expire according to `JWT_EXPIRATION_MINUTES`.
- Expired or invalid bearer tokens must return HTTP `401`.
- The frontend clears local auth state on `401` and redirects users to login with a session-expired message.
- User inactivity is enforced in the frontend with `VITE_IDLE_TIMEOUT_MINUTES`.
- Only direct user interaction resets the idle timer; background polling should not extend a session.

## Database / Migrations

- Supabase SQL files are the source of truth for schema changes.
- In production, keep `spring.jpa.hibernate.ddl-auto` set to `validate` or `none`.
- Do not run production with Hibernate `update`.
- Follow `SQL_RUN_ORDER.md` for fresh database setup or compatibility repairs.
- Back up the database before running repair scripts on an existing environment.

## Dev Tooling

- H2 console and frame-option disabling are only allowed for `dev` or `test` profiles.
- `app.dev-auth.enabled` must be `false` in production.
- Dev demo-data and mock billing tools must remain disabled outside local/dev environments.

## Rate Limiting

TradePilot includes a basic in-memory limiter for first-pass abuse protection.

- Public endpoints are keyed by IP plus route:
  - login
  - register
  - forgot password
  - resend verification
  - contact form
- Authenticated AI generation endpoints are keyed by user plus feature.
- Blocked requests return HTTP `429` with a safe generic message.

Production tuning variables:

- `RATE_LIMIT_AUTH_LOGIN_MAX`
- `RATE_LIMIT_AUTH_LOGIN_WINDOW_SECONDS`
- `RATE_LIMIT_AUTH_REGISTER_MAX`
- `RATE_LIMIT_AUTH_REGISTER_WINDOW_SECONDS`
- `RATE_LIMIT_AUTH_EMAIL_MAX`
- `RATE_LIMIT_AUTH_EMAIL_WINDOW_SECONDS`
- `RATE_LIMIT_CONTACT_MAX`
- `RATE_LIMIT_CONTACT_WINDOW_SECONDS`
- `RATE_LIMIT_AI_GENERATION_MAX`
- `RATE_LIMIT_AI_GENERATION_WINDOW_SECONDS`

For multi-instance production deployments, replace the in-memory limiter with Redis or an edge/WAF-backed limiter.

## Error Handling

- Unexpected backend exceptions return a safe generic message and `requestId`.
- Full stack traces are logged server-side only.
- Do not expose internal class names, SQL, tokens, prompts, or secrets to clients.

## Pre-Launch Manual Checks

- Confirm production profile does not permit `/h2-console/**`.
- Confirm repeated bad login/contact/reset/AI attempts eventually return `429`.
- Confirm frontend auth, `/api/accounts`, billing, admin, AI coach, Trading DNA, and Session Review flows still work.
- Confirm logs include request IDs and do not include passwords, JWTs, reset tokens, verification tokens, OpenAI keys, Stripe secrets, or raw prompts.
