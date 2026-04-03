# Talent Performance Pulse – Backend Starter

Spring Boot 3 · Java 17 · Docker · PostgreSQL · H2 (dev)

The containerised starter project for the HR Performance Tracking backend.
Architecture and package structure are pre-aligned with the LLD so every
module can be dropped in without restructuring.

---

## Quick Start

### Prerequisites

| Tool | Minimum Version |
|------|-----------------|
| Docker Desktop | 24.x |
| Java (local dev only) | 17 |
| Maven (local dev only) | 3.9 |

---

### Option A – Docker (recommended, zero local setup)

```bash
# 1. Clone
git clone <repo-url> && cd performancepulse

# 2. Copy env template (defaults are fine for dev)
cp .env.example .env

# 3. Build and start (H2 in-memory, no Postgres needed)
docker compose up --build

# App is ready when you see:
#   Started PerformancePulseApplication in X.XXX seconds
```

**Verify it's running:**

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}

curl http://localhost:8080/api/v1/hello
# → {"status":"success","data":{"greeting":"Hello from Talent Performance Pulse!","activeProfile":"dev",...}}
```

---

### Option B – Local Maven (fastest feedback loop)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

### Option C – Prod Stack (Spring Boot + PostgreSQL + pgAdmin)

```bash
# Set real credentials in .env first, then:
docker compose --profile prod up -d --build

# Services:
#   App     → http://localhost:8080
#   pgAdmin → http://localhost:5050  (admin@pulse.local / admin)
```

---

## Endpoints

### Swagger UI (interactive docs)
```
http://localhost:8080/swagger-ui/index.html
```

### REST Endpoints

| Method | URL | Description | Body |
|--------|-----|-------------|------|
| `GET` | `/api/v1/hello` | Default greeting + active profile | — |
| `POST` | `/api/v1/hello` | Personalised greeting | `{"name":"Alice","message":"optional"}` |
| `GET` | `/actuator/health` | Container health check | — |
| `GET` | `/actuator/metrics` | JVM + cache metrics | — |
| `GET` | `/actuator/caches` | Caffeine cache stats | — |
| `GET` | `/h2-console` | H2 web console (dev only) | — |

### Sample cURL Calls

```bash
# GET – default greeting
curl -s http://localhost:8080/api/v1/hello | jq

# POST – personalised greeting
curl -s -X POST http://localhost:8080/api/v1/hello \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","message":"Great platform!"}' | jq

# POST – validation failure (blank name → 400)
curl -s -X POST http://localhost:8080/api/v1/hello \
  -H "Content-Type: application/json" \
  -d '{"name":""}' | jq
```

---

## Project Structure

```
performancepulse/
├── src/main/java/com/hr/performancepulse/
│   ├── PerformancePulseApplication.java   ← entry point
│   ├── audit/
│   │   └── AuditAwareImpl.java            ← @CreatedBy / @LastModifiedBy
│   ├── config/
│   │   ├── AppConfig.java                 ← Caffeine CacheManager bean
│   │   └── SwaggerConfig.java             ← OpenAPI 3 metadata
│   ├── controller/
│   │   ├── BaseController.java            ← shared response envelope helpers
│   │   └── HelloController.java           ← GET + POST /api/v1/hello
│   ├── dto/
│   │   ├── request/HelloRequest.java      ← validated request body
│   │   └── response/
│   │       ├── ApiResponse.java           ← standard envelope {status,data,timestamp}
│   │       └── HelloResponse.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java    ← @RestControllerAdvice (LLD §10)
│   │   ├── ResourceNotFoundException.java
│   │   ├── DuplicateReviewException.java
│   │   ├── InvalidCycleStateException.java
│   │   └── ReviewFinalizedException.java
│   └── service/
│       ├── HelloService.java              ← interface
│       └── impl/HelloServiceImpl.java     ← @Service @Transactional
│
├── src/main/resources/
│   └── application.yml                    ← base + dev + prod profiles
│
├── src/test/java/com/hr/performancepulse/
│   └── controller/HelloControllerTest.java ← MockMvc integration tests
│
├── docker/
│   └── postgres/init/01_init.sql          ← DB init hook for prod DDL
│
├── Dockerfile                             ← multi-stage builder → slim runtime
├── docker-compose.yml                     ← dev (H2) + prod profile (Postgres + pgAdmin)
├── .dockerignore
├── .env.example
└── pom.xml
```

**Pre-created empty packages** (ready for LLD modules):

```
entity/       ← Employee, ReviewCycle, PerformanceReview, Goal (LLD §3)
repository/   ← JpaRepository + custom queries (LLD §5)
mapper/       ← MapStruct DTO↔Entity mappers (LLD §9)
util/         ← PageableUtils, DateRangeValidator (LLD §12)
```

---

## Running Tests

```bash
# All tests (uses H2 via dev profile automatically)
mvn test

# Single test class
mvn test -Dtest=HelloControllerTest
```

---

## LLD Extension Roadmap

Follow this order when implementing the LLD modules:

| Step | What to Add | LLD Section |
|------|-------------|-------------|
| 1 | `AuditEntity` base class | §3.1 |
| 2 | `Employee` entity + `EmployeeRepository` | §3.2, §5.1 |
| 3 | `EmployeeService` + `EmployeeController` | §6.1, §8.2 |
| 4 | `ReviewCycle` entity + `CycleService` | §3.3, §6.3 |
| 5 | `PerformanceReview` entity + `ReviewService` | §3.4, §6.2 |
| 6 | `Goal` entity + `GoalService` | §3.5, §6.4 |
| 7 | `AnalyticsService` + cycle summary endpoint | §6.4, §8.3 |
| 8 | Caching (`@Cacheable`) on analytics | §7 |
| 9 | `EmployeeMapper`, `ReviewMapper` (MapStruct) | §9 |
| 10 | DB indexes migration scripts | §4.2 |
| 11 | JWT auth wiring (`AuditAwareImpl` update) | §15 |
| 12 | Swap Caffeine → Redis (one bean change) | §7, §15 |

---

## Environment Variables Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` = H2 · `prod` = PostgreSQL |
| `DB_URL` | `jdbc:postgresql://postgres:5432/performancepulse` | JDBC URL (prod only) |
| `DB_USER` | `pulse_user` | Database username (prod only) |
| `DB_PASSWORD` | `changeme` | Database password (prod only) |
| `PGADMIN_PASSWORD` | `admin` | pgAdmin UI password (prod only) |
| `JAVA_OPTS` | see Dockerfile | JVM flags |

---

## Useful Docker Commands

```bash
# Tail app logs
docker compose logs -f app

# Shell into running container
docker exec -it performance-pulse-app sh

# Rebuild only the app image (after code change)
docker compose up --build app

# Full teardown including volumes
docker compose --profile prod down -v

# Check Caffeine cache stats
curl http://localhost:8080/actuator/metrics/cache.gets | jq
```
