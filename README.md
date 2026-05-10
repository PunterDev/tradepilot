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

Backend runs on:

```bash
http://localhost:8080
```

---

# 🔐 Environment Variables

TradePilot uses environment variables to avoid committing secrets to GitHub.

Create:

```bash
backend/application.env
```

Example:

```env
DB_URL=jdbc:postgresql://localhost:5432/tradepilot
DB_USERNAME=postgres
DB_PASSWORD=your_password

JWT_SECRET=your_secret_key

OPENAI_API_KEY=your_openai_key

SERVER_PORT=8080
```

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
