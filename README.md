# LeetCode Company-Wise Problem Explorer

A full-stack application for searching and filtering LeetCode problems by company, difficulty, topic, recency, and frequency. Data is sourced from [liquidslr/leetcode-company-wise-problems](https://github.com/liquidslr/leetcode-company-wise-problems) and synced automatically every 6 hours.

## Prerequisites

- Java 21+
- Node.js 18+
- PostgreSQL 15+

## Database Setup

1. Install PostgreSQL if not already installed:

```bash
# macOS
brew install postgresql@17
brew services start postgresql@17

# Ubuntu/Debian
sudo apt install postgresql
sudo systemctl start postgresql
```

2. Create the database:

```bash
psql postgres -c "CREATE DATABASE leetcode_explorer;"
```

The app connects with your OS username and no password by default (peer/trust auth). To customize, set environment variables before starting the backend:

```bash
export SPRING_DATASOURCE_USERNAME=your_user
export SPRING_DATASOURCE_PASSWORD=your_password
```

Tables and indexes are created automatically by Liquibase on first startup.

## Backend

The backend is a Spring Boot app that provides REST APIs and a background sync worker.

```bash
cd BACKEND
./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. On first startup it clones the source repo and ingests all CSV data into PostgreSQL (~3,000 problems, ~340 companies). This takes a couple of minutes.

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/problems` | Search/filter problems (company, difficulty, topic, recency, frequency, keyword) |
| GET | `/api/v1/problems/overlap` | Find problems shared across multiple companies |
| GET | `/api/v1/companies` | List all companies with problem counts |
| GET | `/api/v1/companies/{name}/stats` | Company stats (difficulty breakdown, top topics, recency) |
| GET | `/api/v1/topics` | List all topics with problem counts |
| GET | `/api/v1/sync/status` | Current sync status |
| POST | `/api/v1/sync/trigger` | Manually trigger a sync |

## Frontend

The frontend is a React + Vite app with dark/light theme support.

```bash
cd UI
npm install
npm run dev
```

Runs on `http://localhost:3000` and proxies API requests to the backend at `localhost:8080`.

### Features

- **Search Tab** — Filter by company, difficulty, topic, recency, and frequency with auto-apply
- **Compare Tab** — Find problems shared across multiple companies
- **Companies Tab** — Browse companies, view stats, jump to filtered search
- **Topics Tab** — Browse topics, click to filter search by topic

## Project Structure

```
├── BACKEND/
│   ├── src/main/java/org/example/
│   │   ├── controller/      # REST endpoints
│   │   ├── dto/              # Response records
│   │   ├── entity/           # JPA entities
│   │   ├── repository/       # Spring Data repositories
│   │   ├── service/          # Business logic
│   │   ├── worker/           # CSV parser, sync worker
│   │   └── config/           # Sync configuration
│   └── src/main/resources/
│       ├── application.properties
│       └── db/changelog/     # Liquibase migrations
├── UI/
│   └── src/
│       ├── api/              # API client
│       └── components/       # React components
└── docs/                     # Design spec
```
