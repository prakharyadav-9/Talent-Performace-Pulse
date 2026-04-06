# 🎯 Talent Performance Pulse

A backend system for an internal HR tool that helps managers track and review employee performance. Built with **Spring Boot 3**, **PostgreSQL**, and clean layered architecture — exposes RESTful APIs to manage employees, review cycles, goals, and analytics.

---

## 📋 Table of Contents

- [What is this project?](#what-is-this-project)
- [Tech Stack](#tech-stack)
- [How to Run](#how-to-run)
- [API Endpoints](#api-endpoints)
- [High-Level Design (HLD)](#high-level-design-hld)
- [Low-Level Design (LLD)](#low-level-design-lld)
- [Project Structure](#project-structure)
- [More Docs](#more-docs)

---

## What is this project?

Talent Performance Pulse is a REST API backend that allows HR teams to:

- **Create and manage employees** across departments with role and joining date info
- **Define review cycles** (e.g., "Q1 2026") with start/end dates and status lifecycle
- **Submit performance reviews** (self, peer, or manager) with ratings (1–5) and notes
- **Track goals** per employee per cycle with completion status
- **Query analytics** — cycle summaries with average ratings, top performer, and goal completion rates
- **Filter employees** by department and minimum average rating

**Sample output from a cycle summary:**
```
→ Cycle: Q1 2026 Reviews
→ Total Reviews: 3
→ Average Rating: 4.0
→ Top Performer: Alice Johnson (Rating: 5.0)
→ Goal Stats: Total=3, Completed=1, Missed=1, In Progress=1
→ Goal Completion Rate: 33.3%
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.3.4 |
| Language | Java 17 |
| ORM | JPA / Hibernate 6 |
| Database | PostgreSQL 15 (H2 for dev) |
| Caching | Caffeine (in-memory, Redis-swappable) |
| Mapping | MapStruct |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven 3.9 |
| Container | Docker + Docker Compose |

---

## How to Run

### Prerequisites

| Tool | Version |
|---|---|
| Docker Desktop | 24.x+ |
| Java (local dev only) | 17 |
| Maven (local dev only) | 3.9 |
| Python (for E2E tests) | 3.8+ |
| use `.env.example` for environment activation | -- |

---

### Option A — Docker (Recommended)

Zero local setup. Uses H2 in-memory DB by default.

```bash
# 1. Copy env template (defaults work for dev)
cp .env.example .env

# 2. Build and start
docker compose up --build

# App is ready when you see:
#   Started PerformancePulseApplication in X.XXX seconds

# 3. Verify everything works — run the E2E test suite
pip install -r requirements.txt && python test_e2e.py
```

---

### Option B — Local Dev (Maven + PostgreSQL)

```bash
# 1. Start PostgreSQL (or use Docker just for the DB)
docker compose up postgres -d

# 2. Copy env and configure DB credentials
cp .env.example .env

# 3. Build and run
mvn clean package -DskipTests
java -jar target/performancepulse-0.0.1-SNAPSHOT.jar
```

---

### Option C — Production

```bash
docker compose -f docker-compose.prod.yml up --build -d
```

---

### Verify the App is Running

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive API docs (Swagger UI) |
| `http://localhost:8080/actuator/health` | Health check endpoint |
| `http://localhost:8080/api/v1/employees` | Sample API call |

---

### Running Tests

```bash
# E2E tests (covers all endpoints end-to-end)
python test_e2e.py

# Unit tests
mvn test
```

See [`TEST_E2E_README.md`](TEST_E2E_README.md) and [`QUICK_START.md`](QUICK_START.md) for more testing details.

---

## API Endpoints

Base path: `/api/v1`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/employees` | Create a new employee |
| `GET` | `/employees?department=X&minRating=Y` | Filter employees (cached) |
| `POST` | `/cycles` | Create a review cycle |
| `PATCH` | `/cycles/{id}/status` | Update cycle status (UPCOMING → ACTIVE → CLOSED) |
| `GET` | `/cycles/{id}/summary` | Get analytics summary for a cycle (cached) |
| `POST` | `/reviews` | Submit a performance review |
| `GET` | `/employees/{id}/reviews` | Get all reviews for an employee |
| `POST` | `/goals` | Create a goal for an employee in a cycle |
| `PATCH` | `/goals/{id}` | Update a goal's status or progress |

Full interactive documentation is available at **`/swagger-ui.html`** when the app is running.

---

## High-Level Design (HLD)

> 📄 Full document: [`docs/PerformanceTracker_HLD.md`](docs/PerformanceTracker_HLD.md)

### System Overview

A single-service Spring Boot application with a layered architecture:

```
HTTP Clients (HR Managers)
        │
        ▼
  REST Controllers  (Spring MVC)
        │
        ▼
  Service Layer     (Business Logic + Cache)
        │
        ▼
  Repository Layer  (JPA / Spring Data)
        │
        ▼
  PostgreSQL DB     (or H2 in dev)
```

### Core Domain Entities

```
Employee ──< PerformanceReview >── ReviewCycle
    │                                   │
    └──────────── Goal ────────────────┘
```

- **Employee** — Profile, department, manager hierarchy (self-referential FK)
- **ReviewCycle** — Named evaluation period (Q1 2026) with status lifecycle
- **PerformanceReview** — Links an employee + cycle with a 1–5 rating, reviewer, and type (SELF/PEER/MANAGER)
- **Goal** — Individual objective for employee+cycle with status tracking (PENDING / COMPLETED / MISSED / IN_PROGRESS)

### Scalability Strategy

| Concern | Solution |
|---|---|
| 500 concurrent managers | Horizontal scaling + HikariCP connection pooling (pool size: 20) |
| 100k+ reviews per cycle | DB indexes on key FK columns + optimised JPQL aggregate queries |
| Repeated analytics reads | Caffeine cache on cycle summaries and employee filter results |
| Concurrent updates | Optimistic locking (`@Version`) on all entities |

> 📄 See [`SCALING_STRATEGY.md`](SCALING_STRATEGY.md) for deep-dive scaling decisions.

---

## Low-Level Design (LLD)

> 📄 Full document: [`docs/PerformanceTracker_LLD.md`](docs/PerformanceTracker_LLD.md)

### Package Layout

```
com.hr.performancepulse/
├── config/          # AppConfig, SwaggerConfig, Cache config
├── controller/      # REST controllers (one per domain)
├── service/         # Interfaces for all services
│   └── impl/        # Concrete service implementations
├── repository/      # Spring Data JPA repositories with custom queries
├── entity/          # JPA entities (all extend AuditEntity)
├── dto/
│   ├── request/     # Incoming payloads with @Valid constraints
│   └── response/    # Outbound response shapes
├── mapper/          # MapStruct mappers (entity ↔ DTO)
├── exception/       # GlobalExceptionHandler + custom exceptions
├── enums/           # Domain enums (Department, CycleStatus, GoalStatus, etc.)
└── audit/           # AuditAwareImpl for createdBy/updatedBy population
```

### Key Design Decisions

**AuditEntity (base class)**  
All entities inherit `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, and `version` (optimistic lock) automatically via `@EntityListeners`.

**Caching**  
Two Caffeine caches are configured:
- `employeeFilter` — caches filtered employee list results; evicted on new review submission
- `cycleSummary` — caches analytics per cycle ID; evicted when cycle status changes

**Unique Constraints**  
`PerformanceReview` has a unique constraint on `(employee_id, cycle_id, reviewer_id)` — prevents duplicate reviews from the same reviewer in a single cycle.

**Database Indexes**  
Seven indexes defined in schema covering FK columns and query-heavy fields (`department`, `status`, `rating`) to support analytics without full-table scans.

**Error Handling**  
`GlobalExceptionHandler` catches custom exceptions (`ResourceNotFoundException`, `DuplicateReviewException`, `InvalidCycleStateException`, etc.) and maps them to structured `ApiResponse` payloads with appropriate HTTP status codes.

**DTO Mapping**  
MapStruct generates compile-time mappers (`EmployeeMapper`, `ReviewMapper`, `GoalMapper`, `CycleMapper`) — zero reflection overhead at runtime.

### Database Schema Summary

```sql
employees          (id, first_name, last_name, email, department, job_title,
                    manager_id, joining_date, status, + audit cols)

review_cycles      (id, name, start_date, end_date, status, + audit cols)

performance_reviews (id, employee_id, cycle_id, reviewer_id, rating, notes,
                     review_type, submitted_at, is_finalized, + audit cols)

goals              (id, employee_id, cycle_id, title, description,
                    status, target_date, + audit cols)
```

---

## Project Structure

```
Talent-Performance-Pulse/
├── src/
│   ├── main/java/com/hr/performancepulse/   # Application source
│   └── test/                                 # Unit tests
├── docs/
│   ├── PerformanceTracker_HLD.md             # High-Level Design
│   ├── PerformanceTracker_LLD.md             # Low-Level Design
│   ├── PLAN_Overview.md                      # Implementation roadmap
│   └── MLP-1_Phase-{0..3}/                  # Per-phase completion docs
├── docker/postgres/init/01_init.sql          # DB init script
├── docker-compose.yml                        # Dev setup
├── docker-compose.prod.yml                   # Production setup
├── Dockerfile                                # App container
├── pom.xml                                   # Maven build
├── test_e2e.py                               # E2E test suite
├── requirements.txt                          # Python test deps
├── QUICK_START.md                            # 5-minute test guide
├── TEST_E2E_README.md                        # Full E2E docs
├── SCALING_STRATEGY.md                       # Scaling decisions
└── .env.example                              # Environment template
```

---

## More Docs

| Document | What's inside |
|---|---|
| [`docs/PerformanceTracker_HLD.md`](docs/PerformanceTracker_HLD.md) | Full High-Level Design with architecture, data model, and scalability strategy |
| [`docs/PerformanceTracker_LLD.md`](docs/PerformanceTracker_LLD.md) | Full Low-Level Design — entities, queries, cache config, exception handling |
| [`docs/PLAN_Overview.md`](docs/PLAN_Overview.md) | 4-phase implementation roadmap with dependency graph |
| [`docs/MLP-1_Phase-3/PHASE3_COMPLETION_DOCUMENT.md`](docs/MLP-1_Phase-3/PHASE3_COMPLETION_DOCUMENT.md) | Final phase completion with all endpoint details |
| [`SCALING_STRATEGY.md`](SCALING_STRATEGY.md) | Horizontal scaling, caching, and DB optimisation decisions |
| [`TEST_E2E_README.md`](TEST_E2E_README.md) | Comprehensive E2E test documentation |
| [`QUICK_START.md`](QUICK_START.md) | 5-minute guide to running tests |
| `http://localhost:8080/swagger-ui.html` | Live interactive API documentation (app must be running) |