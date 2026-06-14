# TradePilot

TradePilot is an AI-powered trading journal for futures and prop firm traders. It helps traders import executions, review sessions, understand behavioral patterns, and turn trading data into practical coaching.

The product is built for beta launch with CSV trade import, multi-account tracking, AI coaching, billing, usage limits, admin operations, and production deployment documentation.

## Product

Core trader workflow:

- CSV trade import
- Multi-account support
- PnL calendar
- Analytics dashboard
- Session Review
- AI Trade Review
- AI Daily Coach
- AI Weekly Coach
- Trading DNA
- Trade journal metadata: setup, confidence, emotion, notes

SaaS and operations:

- Stripe billing and usage limits
- Admin Portal
- Contact CRM
- Feature Usage Analytics
- Retention Analytics

## Tech Stack

Frontend:

- React
- Vite
- TypeScript
- Axios

Backend:

- Spring Boot 3.3.5
- Java 17
- Maven
- Spring Security
- JWT authentication

Data and integrations:

- Supabase Postgres
- Stripe Checkout, Customer Portal, and webhooks
- OpenAI
- Resend transactional email

Deployment targets:

- Vercel frontend
- Render backend
- Supabase database

## Documentation

Production and launch docs:

- [Production Checklist](docs/PRODUCTION_CHECKLIST.md)
- [Environment Variables](docs/ENVIRONMENT_VARIABLES.md)
- [Database Migration Order](docs/DB_MIGRATION_ORDER.md)
- [Deployment Notes](docs/DEPLOYMENT_NOTES.md)

Maintenance utilities:

- [Safe Test Data Cleanup SQL](docs/SAFE_TEST_DATA_CLEANUP.sql)

## Project Structure

```text
tradepilot/
  backend/    Spring Boot API, billing, AI services, Supabase SQL
  frontend/   React/Vite app
  docs/       production, deployment, migration, and cleanup docs
```

## Security

Never commit `.env` files or real secrets. Production secrets must be configured in the hosting provider, not in Git.

Important production rules:

- Keep `.env`, `.env.local`, `backend/.env`, and `frontend/.env` out of Git.
- Configure backend secrets in Render or the selected backend host.
- Configure frontend public environment variables in Vercel or the selected frontend host.
- `APP_ADMIN_BOOTSTRAP_ENABLED` is opt-in and must be intentionally enabled to promote the first admin.
- Stripe webhooks are public at the route level but require Stripe signature verification.
- Do not expose Stripe secret keys, webhook secrets, OpenAI keys, Resend API keys, JWT secrets, or database credentials to the frontend.

## Backend Setup

Requirements:

- Java 17
- Maven 3.9+
- Postgres or Supabase Postgres

Copy the backend example env file and set real local values outside Git:

```bash
cp backend/.env.example backend/.env
```

Required production backend variables are documented in [Environment Variables](docs/ENVIRONMENT_VARIABLES.md). The most important values are:

```env
DATABASE_URL=jdbc:postgresql://...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
JWT_SECRET=...
JWT_EXPIRATION_MS=86400000
OPENAI_API_KEY=...
STRIPE_SECRET_KEY=...
STRIPE_WEBHOOK_SECRET=...
STRIPE_PRO_MONTHLY_PRICE_ID=...
STRIPE_PRO_YEARLY_PRICE_ID=...
RESEND_API_KEY=...
EMAIL_FROM=TradePilot <no-reply@your-domain.com>
CONTACT_EMAIL=...
APP_ALLOWED_ORIGINS=https://your-frontend-domain.com
APP_ADMIN_EMAIL=founder@example.com
APP_ADMIN_BOOTSTRAP_ENABLED=false
```

Run the backend:

```bash
cd backend
mvn spring-boot:run
```

The backend defaults to:

```text
http://localhost:8080
```

For local development with dev-only helpers:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Dev helpers must stay disabled in production.

## Frontend Setup

Requirements:

- Node.js
- npm

Create a frontend env file outside Git:

```bash
cp frontend/.env.example frontend/.env
```

Set the API URL:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend defaults to:

```text
http://localhost:5173
```

## Database Setup

TradePilot uses Supabase Postgres for hosted environments and H2 for tests.

Apply SQL migrations in the documented order:

- [Database Migration Order](docs/DB_MIGRATION_ORDER.md)

The migration set includes core users/accounts/trades, user isolation indexes, billing limits, admin audit logs, contact messages, feature usage events, session reviews, Trading DNA, journal metadata, observability tables, and performance indexes.

## Stripe Setup

Required Stripe setup:

- Create monthly and yearly Pro price IDs.
- Configure `STRIPE_SECRET_KEY`.
- Configure `STRIPE_WEBHOOK_SECRET`.
- Configure `STRIPE_PRO_MONTHLY_PRICE_ID`.
- Configure `STRIPE_PRO_YEARLY_PRICE_ID`.
- Point Stripe webhooks to `/api/stripe/webhook`.

Supported billing flows include checkout, customer portal, subscription updates, invoice payment events, duplicate webhook protection, and billing usage limits.

Feature access is enforced by the backend:

| Feature | Free | Pro |
| --- | --- | --- |
| CSV Import | Yes | Yes |
| PnL Calendar | Yes | Yes |
| Basic Analytics | Yes | Yes |
| AI Trade Review | Yes, subject to credits | Yes, subject to credits |
| Session Review | No | Yes |
| Daily Coach | No | Yes |
| Weekly Coach | No | Yes |
| Trading DNA | No | Yes |
| Advanced Analytics | No | Yes |

Monthly AI credit limits:

| Plan | Credits |
| --- | ---: |
| Free | 10 |
| Pro | 100 |

AI credit costs:

| AI feature | Credits |
| --- | ---: |
| AI Trade Review | 1 |
| Session Review | 2 |
| Daily Coach | 2 |
| Weekly Coach | 4 |
| Trading DNA | 3 |

Cached AI results do not consume credits. Force refresh or regeneration consumes credits.

## Email Setup

TradePilot uses Resend for transactional email:

- Email verification
- Password reset
- Contact form notifications
- Admin contact replies

Configure Resend credentials through environment variables only. If Resend is not configured locally, the backend uses safe development behavior and does not send real email.

## Admin Setup

Admin bootstrap is intentionally opt-in.

To bootstrap the first admin:

1. Register the admin user normally.
2. Set `APP_ADMIN_EMAIL` to that user's email.
3. Set `APP_ADMIN_BOOTSTRAP_ENABLED=true`.
4. Start the backend once.
5. Disable bootstrap again after the admin is promoted.

Admin users can access `/admin` for user management, billing operations, contact CRM, observability, feature usage analytics, and retention analytics.

## Testing

Frontend production build:

```bash
cd frontend
npm run build
```

Backend tests:

```bash
cd backend
mvn test
```

Full backend clean test:

```bash
cd backend
mvn clean test
```

## Deployment

Recommended beta deployment:

- Frontend: Vercel
- Backend: Render
- Database: Supabase Postgres

Deployment notes:

- Configure all backend secrets in Render.
- Configure `VITE_API_BASE_URL` in Vercel.
- Configure `APP_ALLOWED_ORIGINS` to the deployed frontend origin.
- Apply Supabase migrations before backend smoke tests.
- Configure Stripe webhook endpoint after backend deployment.
- Verify Resend email, auth, checkout, AI generation, admin access, and CORS using [Production Checklist](docs/PRODUCTION_CHECKLIST.md).

## Roadmap

- Observability dashboard
- Broker integrations
- Chart screenshot AI review
- Advanced risk analytics
- Mobile responsive polish

## Contributing

Private beta project for now.

## License

MIT License

## Author

Built by Atish Shrestha.
