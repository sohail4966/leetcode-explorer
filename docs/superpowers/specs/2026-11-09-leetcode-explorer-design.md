# LeetCode Company-Wise Problem Explorer — Design Spec

## Intent

A tool for software engineers preparing for interviews or studying
problem patterns. It ingests LeetCode company-tagged problem data from
[liquidslr/leetcode-company-wise-problems](https://github.com/liquidslr/leetcode-company-wise-problems)
into a PostgreSQL database and exposes it through a REST API with rich
filtering. A web UI comes later once the backend is stable.

### Why

The raw repo is a folder of CSVs — useful but hard to query. Engineers
need to slice data many ways: "give me all hard graph problems Amazon
asked in the last 3 months," or "which problems do Google AND Meta both
ask frequently?" That requires a real database, an API, and eventually
a UI.

## Goals

1. **Always-fresh data** — a background worker keeps the DB in sync with
   the upstream repo automatically.
2. **Rich, composable filtering** — users can combine any filters in a
   single query (company, difficulty, topic, recency, frequency range,
   keyword search, cross-company overlap).
3. **Direct LeetCode links** — every result includes a clickable link to
   the problem on LeetCode.
4. **Fast and paginated** — large result sets don't choke the API.

## Non-Goals

- Hosting LeetCode problem descriptions or solutions (just metadata and
  links).
- User accounts, auth, or saved progress (phase 1 is stateless).
- A mobile app.

## Anti-Goals

- Becoming a LeetCode scraper or violating LeetCode's ToS — the data
  source is a public GitHub repo, not LeetCode directly.
- Overcomplicating the data model beyond what the CSVs provide.

---

## Data Source

**Repo:** `github.com/liquidslr/leetcode-company-wise-problems`

**Structure:** One folder per company, each containing up to 5 CSVs:

| File                        | Recency Period   |
|-----------------------------|------------------|
| `1. Thirty Days.csv`        | Last 30 days     |
| `2. Three Months.csv`       | Last 3 months    |
| `3. Six Months.csv`         | Last 6 months    |
| `4. More Than Six Months.csv` | Older than 6 months |
| `5. All.csv`                | All time         |

**CSV Columns (identical across all files):**

| Column          | Type   | Example                                      |
|-----------------|--------|----------------------------------------------|
| Difficulty      | String | `EASY`, `MEDIUM`, `HARD`                     |
| Title           | String | `Two Sum`                                    |
| Frequency       | Float  | `100.0` (0–100 scale, company-specific)      |
| Acceptance Rate | Float  | `0.0058` (normalized decimal, not percentage) |
| Link            | String | `https://leetcode.com/problems/two-sum`      |
| Topics          | String | `"Array, Hash Table"` (comma-separated)      |

**Key observations:**
- The same problem appears in multiple companies with different frequency
  scores — frequency is company-specific.
- A problem can appear in multiple recency buckets for the same company
  (e.g., a problem in "Thirty Days" also appears in "All").
- Topics are multi-valued (comma-separated within a single CSV field).
- ~300+ companies, ~2500+ unique problems.

---

## Data Model (PostgreSQL)

### Tables

#### `problems`
The canonical set of unique LeetCode problems.

| Column          | Type         | Notes                        |
|-----------------|--------------|------------------------------|
| id              | BIGSERIAL PK |                              |
| title           | VARCHAR(500) | Unique, not null             |
| difficulty      | VARCHAR(10)  | EASY / MEDIUM / HARD         |
| acceptance_rate | DOUBLE       | Normalized decimal           |
| link            | VARCHAR(500) | Unique LeetCode URL          |
| created_at      | TIMESTAMP    |                              |
| updated_at      | TIMESTAMP    |                              |

#### `topics`
Normalized topic tags.

| Column | Type         | Notes            |
|--------|--------------|------------------|
| id     | BIGSERIAL PK |                  |
| name   | VARCHAR(100) | Unique, not null |

#### `problem_topics`
Join table: many-to-many between problems and topics.

| Column     | Type   | Notes |
|------------|--------|-------|
| problem_id | BIGINT | FK    |
| topic_id   | BIGINT | FK    |

#### `companies`
Normalized company names.

| Column | Type         | Notes            |
|--------|--------------|------------------|
| id     | BIGSERIAL PK |                  |
| name   | VARCHAR(200) | Unique, not null |

#### `company_problems`
The core relationship: which companies ask which problems, with
company-specific metrics and recency.

| Column        | Type         | Notes                                       |
|---------------|--------------|---------------------------------------------|
| id            | BIGSERIAL PK |                                             |
| company_id    | BIGINT       | FK                                          |
| problem_id    | BIGINT       | FK                                          |
| frequency     | DOUBLE       | 0–100, company-specific                     |
| recency       | VARCHAR(30)  | `THIRTY_DAYS`, `THREE_MONTHS`, `SIX_MONTHS`, `MORE_THAN_SIX_MONTHS`, `ALL` |
| created_at    | TIMESTAMP    |                                             |
| updated_at    | TIMESTAMP    |                                             |

**Unique constraint:** `(company_id, problem_id, recency)`

#### `sync_metadata`
Tracks the state of the data sync worker.

| Column          | Type         | Notes                          |
|-----------------|--------------|--------------------------------|
| id              | BIGSERIAL PK |                                |
| last_commit_sha | VARCHAR(40)  | Last processed commit          |
| last_synced_at  | TIMESTAMP    |                                |
| status          | VARCHAR(20)  | `SUCCESS`, `FAILED`, `RUNNING` |
| error_message   | TEXT         | Nullable                       |

### Indexes

- `problems.title` — for keyword search
- `problems.difficulty` — for filtering
- `company_problems(company_id, recency)` — most common query pattern
- `company_problems(problem_id)` — for cross-company lookups
- `company_problems.frequency` — for sorting/range filters
- `topics.name` — for topic filtering
- Full-text index on `problems.title` — for search

---

## Background Sync Worker

A Spring `@Scheduled` job that runs on a configurable interval (default:
every 6 hours).

### Flow

1. **Check for updates** — call the GitHub API:
   `GET /repos/liquidslr/leetcode-company-wise-problems/commits/main`
   Compare the latest commit SHA against `sync_metadata.last_commit_sha`.

2. **If no new commits** — log and skip. No DB writes.

3. **If new commits detected:**
   a. Clone or pull the repo into a temp directory.
   b. Walk every company folder and parse each CSV file.
   c. Ingest **one transaction per company:**
      - Upsert `problems` by title (update acceptance_rate if changed).
      - Upsert `topics` by name.
      - Upsert `problem_topics` associations.
      - Upsert `companies` by folder name.
      - **Full-replace** `company_problems` for this company: delete all
        existing rows for the company, then insert the fresh CSV data.
        This is simple, idempotent, and within the per-company
        transaction the data gap is invisible to readers.
   d. Update `sync_metadata` with new SHA and timestamp.

4. **On startup** — if `sync_metadata` is empty (first run) or stale,
   trigger an immediate sync so the API isn't empty for 6 hours.

5. **On failure** — log the error, set `sync_metadata.status = FAILED`,
   and retry on next scheduled run.

### Config

| Property                  | Default | Description                  |
|---------------------------|---------|------------------------------|
| `sync.interval-hours`     | 6       | Hours between sync checks    |
| `sync.repo-url`           | (the repo) | GitHub repo to track      |
| `sync.branch`             | main    | Branch to watch              |
| `sync.enabled`            | true    | Kill switch for the worker   |

### Manual trigger

`POST /api/v1/sync/trigger` — kicks off a sync immediately. Returns the
sync status.

---

## REST API

Base path: `/api/v1`

### Problem Search — `GET /api/v1/problems`

The primary endpoint. All filters are optional and composable via query
params.

| Param        | Type           | Description                                     |
|--------------|----------------|-------------------------------------------------|
| `company`    | String (multi) | Filter by company name(s). Comma-separated.     |
| `difficulty` | String (multi) | `EASY`, `MEDIUM`, `HARD`. Comma-separated.      |
| `topic`      | String (multi) | Filter by topic tag(s). Comma-separated.         |
| `recency`    | String         | `THIRTY_DAYS`, `THREE_MONTHS`, `SIX_MONTHS`, `MORE_THAN_SIX_MONTHS`, `ALL` |
| `search`     | String         | Keyword search on problem title (case-insensitive, partial match) |
| `minFrequency` | Float        | Minimum frequency score (0–100)                 |
| `maxFrequency` | Float        | Maximum frequency score (0–100)                 |
| `sortBy`     | String         | `frequency`, `acceptance_rate`, `title`, `difficulty`. Default: `frequency` |
| `sortOrder`  | String         | `asc`, `desc`. Default: `desc`                  |
| `page`       | Integer        | Page number (0-indexed). Default: 0             |
| `size`       | Integer        | Page size. Default: 20, max: 100                |

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Two Sum",
      "difficulty": "EASY",
      "acceptanceRate": 0.0058,
      "link": "https://leetcode.com/problems/two-sum",
      "topics": ["Array", "Hash Table"],
      "companies": [
        {
          "name": "Amazon",
          "frequency": 100.0,
          "recency": "THIRTY_DAYS"
        },
        {
          "name": "Google",
          "frequency": 100.0,
          "recency": "THIRTY_DAYS"
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 342,
  "totalPages": 18
}
```

When `company` filter is set, `companies` in the response shows only the
matched companies. When no company filter, it shows all companies that
ask that problem (limited to the top 5 by frequency, with a count of
the rest).

**Multi-company sorting:** When filtering by multiple companies and
sorting by `frequency`, sort by the **maximum frequency** across the
selected companies. This surfaces the most-asked problems first
regardless of which company asks them most.

**`problemCount` in company lists:** Count of **unique problems**
(deduplicated across recency buckets), derived from the `ALL` bucket
when available, otherwise `COUNT(DISTINCT problem_id)` across all
buckets.

### Cross-Company Overlap — `GET /api/v1/problems/overlap`

Find problems asked by multiple specific companies.

| Param        | Type           | Description                                |
|--------------|----------------|--------------------------------------------|
| `companies`  | String (multi) | **Required.** Two or more company names.   |
| `difficulty` | String (multi) | Optional filter.                           |
| `topic`      | String (multi) | Optional filter.                           |
| `recency`    | String         | Optional filter.                           |
| `page`/`size`| Integer        | Pagination.                                |

Returns problems that appear in **all** specified companies.

**Recency filter on overlap:** Strict — the problem must appear in the
specified recency bucket for **every** listed company, not just one.

### Company List — `GET /api/v1/companies`

| Param    | Type   | Description                     |
|----------|--------|---------------------------------|
| `search` | String | Partial name match.             |

**Response:**
```json
[
  {
    "id": 1,
    "name": "Amazon",
    "problemCount": 856
  }
]
```

### Company Detail — `GET /api/v1/companies/{name}/stats`

Aggregated stats for a single company.

**Response:**
```json
{
  "name": "Amazon",
  "totalProblems": 856,
  "difficultyBreakdown": {
    "EASY": 210,
    "MEDIUM": 445,
    "HARD": 201
  },
  "topTopics": [
    { "name": "Array", "count": 312 },
    { "name": "Dynamic Programming", "count": 178 }
  ],
  "recencyBreakdown": {
    "THIRTY_DAYS": 45,
    "THREE_MONTHS": 120,
    "SIX_MONTHS": 210,
    "MORE_THAN_SIX_MONTHS": 481,
    "ALL": 856
  }
}
```

### Topic List — `GET /api/v1/topics`

| Param    | Type   | Description         |
|----------|--------|---------------------|
| `search` | String | Partial name match. |

Returns all topics with problem counts.

### Sync Status — `GET /api/v1/sync/status`

Returns current sync state: last sync time, last commit SHA, status.

### Trigger Sync — `POST /api/v1/sync/trigger`

Manually triggers a sync. Returns immediately with `202 Accepted` and
the sync begins asynchronously.

---

## Tech Stack

| Layer            | Technology                          |
|------------------|-------------------------------------|
| Runtime          | Java 23, Spring Boot 3.4            |
| Database         | PostgreSQL                          |
| ORM              | Spring Data JPA / Hibernate         |
| CSV Parsing      | OpenCSV                             |
| GitHub API       | Spring RestClient (or WebClient)    |
| Git operations   | JGit (clone/pull the repo)          |
| Scheduling       | Spring `@Scheduled`                 |
| API docs         | SpringDoc OpenAPI (Swagger UI)      |
| DB migrations    | Liquibase                           |
| Build            | Maven                               |
| Containerization | Docker Compose (app + PostgreSQL)   |

---

## Project Structure

```
src/main/java/org/example/
├── LeetcodeExplorerApplication.java
├── config/
│   └── SyncConfig.java
├── controller/
│   ├── ProblemController.java
│   ├── CompanyController.java
│   ├── TopicController.java
│   └── SyncController.java
├── dto/
│   ├── ProblemResponse.java
│   ├── CompanyResponse.java
│   ├── CompanyStatsResponse.java
│   ├── TopicResponse.java
│   └── ProblemSearchRequest.java
├── entity/
│   ├── Problem.java
│   ├── Topic.java
│   ├── Company.java
│   ├── CompanyProblem.java
│   └── SyncMetadata.java
├── repository/
│   ├── ProblemRepository.java
│   ├── TopicRepository.java
│   ├── CompanyRepository.java
│   ├── CompanyProblemRepository.java
│   └── SyncMetadataRepository.java
├── service/
│   ├── ProblemService.java
│   ├── CompanyService.java
│   ├── TopicService.java
│   └── SyncService.java
└── worker/
    ├── GitHubSyncWorker.java
    └── CsvParser.java
```

---

## Constraints

- **No auth in phase 1.** API is open. Auth can be layered on later.
- **Rate-limit the GitHub API.** The sync worker uses unauthenticated
  GitHub API calls (60/hour limit). One call per sync check is fine at
  a 6-hour interval. Optionally support a GitHub token for higher limits.
- **Idempotent syncs.** Running the sync twice on the same commit SHA
  produces the same DB state.
- **Transactional ingestion.** The entire CSV ingest for one sync runs in
  a transaction — partial ingestion never corrupts the DB.

---

## What's Left to the Builder

- Exact Flyway migration SQL (follows from the data model above).
- Error handling strategy (global exception handler, error response
  format).
- Docker Compose configuration for local development.
- Logging levels and format.
- CORS configuration (for future frontend).
- Whether to use JGit (in-process) or shell out to `git` for
  clone/pull — JGit is cleaner but heavier.
