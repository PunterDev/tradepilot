# TradePilot 📈

AI-powered trading journal built for futures traders.

TradePilot helps traders track performance, review executions, analyze setups, and generate AI-driven trading insights using a modern full-stack architecture.

---

# 🚀 Features

## Trading Journal
- Log trades manually
- Import Tradovate CSV exports
- Multi-account support
- Track:
  - Entry / Exit
  - Contracts
  - P&L
  - Setup type
  - Session
  - Notes
  - Psychology

---

## Analytics Dashboard
- Win rate
- Equity curve
- Daily / Weekly performance
- Performance by:
  - Setup
  - Session
  - Symbol
  - Weekday

---

## AI Insights
- Weekly AI trade reviews
- Performance reflection summaries
- Trading psychology observations
- Pattern detection

---

## Calendar Journal
- Daily trading calendar
- Click any trading day to:
  - Review trades
  - Add journal notes
  - Track emotions & discipline

---

# 🏗 Tech Stack

## Frontend
- React
- TypeScript
- Vite
- Axios
- TailwindCSS

## Backend
- Java 17
- Spring Boot 3
- Spring Security
- JWT Authentication
- Maven

## Database
- PostgreSQL
- H2 (tests/local fallback)

## Infrastructure
- GitHub
- Render (backend hosting)
- Vercel (frontend hosting)
- Supabase Postgres

---

# 📂 Project Structure

```bash
tradepilot/
 ├── backend/
 ├── frontend/
 └── README.md
```

---

# ⚙️ Backend Setup

## Requirements
- Java 17+
- Maven 3.9+
- PostgreSQL

---

## Run Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

For local development with the temporary `/api/dev/token` auto-login helper:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend runs on:

```bash
http://localhost:8080
```

---

# 🔐 Environment Variables

TradePilot uses environment variables to avoid committing secrets to GitHub.

Use `backend/.env.example` as the reference for required environment variables.

Example:

```env
DB_URL=jdbc:postgresql://localhost:5432/tradepilot
DB_USERNAME=postgres
DB_PASSWORD=your_password

JWT_SECRET=your_base64_32_byte_secret

OPENAI_API_KEY=your_openai_key

SERVER_PORT=8080
DEV_AUTH_ENABLED=false
```

## How to configure Gmail SMTP

TradePilot sends auth emails for account verification and password reset. In development, Gmail SMTP works well with a Gmail App Password.

1. Enable 2-Step Verification on the Gmail account that will send TradePilot emails.
2. Go to your Google Account security settings and create an App Password for Mail.
3. Use the generated 16-character app password as `MAIL_PASSWORD`. Do not use your normal Gmail password.
4. Set the frontend URL to the Vite app URL so email links open the right screen.

Example backend `.env` values:

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-gmail-address@gmail.com
MAIL_PASSWORD=your-16-character-app-password
MAIL_FROM=your-gmail-address@gmail.com
FRONTEND_URL=http://localhost:5173
```

On startup the backend logs either `EMAIL MODE: SMTP_ENABLED` or `EMAIL MODE: DEV_LOG_ONLY`. If SMTP settings are missing, registration and password reset still work, but the verification/reset links are logged to the backend console instead of emailed.

---

# 🛡 Important

Never commit:
- database passwords
- API keys
- JWT secrets
- `.env` files

The repository includes `.gitignore` to protect sensitive files.

---

# 🗄 Database

Default database:

```bash
PostgreSQL
```

Test database:

```bash
H2 in-memory database
```

---

# 🔌 API Endpoints

## Authentication

```http
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

---

## Trades

```http
GET    /api/trades
POST   /api/trades
PUT    /api/trades/{id}
DELETE /api/trades/{id}
```

---

## Statistics

```http
GET /api/stats/summary
GET /api/stats/by-setup
GET /api/stats/by-session
GET /api/stats/by-symbol
GET /api/stats/by-weekday
```

---

## AI

```http
POST /api/ai/weekly-review
```

---

# 💻 Frontend Setup

## Install dependencies

```bash
cd frontend
npm install
```

---

## Run frontend

```bash
npm run dev
```

Frontend runs on:

```bash
http://localhost:5173
```

---

# Admin Portal

Set the first local admin with:

```bash
APP_ADMIN_EMAIL=you@example.com
```

On backend startup, if a user with that email exists, TradePilot promotes that user to `ADMIN`.
Admin users can open `/admin` to search users and manually upgrade or downgrade plans. Run
`backend/supabase_admin_portal.sql` in Supabase for existing databases.

---

# 🌍 Deployment

## Recommended Free Stack

| Service | Platform |
|---|---|
| Frontend | Vercel |
| Backend | Render |
| Database | Supabase |

---

# 📌 Roadmap

- Tradovate API integration
- AI trade coaching
- Screenshot uploads
- Mobile responsive dashboard
- Advanced analytics
- Trade replay
- Order flow integrations
- Multi-user support
- Prop firm metrics
- Risk management dashboard

---

# 🤝 Contributing

Private personal project for now.

---

# 📜 License

MIT License

---

# 👨‍💻 Author

Built by Atish Shrestha

Futures trader + software engineer building tools for traders.
