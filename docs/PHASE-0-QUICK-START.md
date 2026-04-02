# Phase 0 Quick Start Guide

**Objective**: Verify that PostgreSQL is running and Spring Boot can connect with auto-created tables.

---

## Prerequisites

- Java 17+ installed
- Docker & Docker Compose installed
- Maven 3.8+ installed
- Port 8080 and 5432 available

---

## Step 1: Build the Application

```bash
cd /path/to/Talent-Performace-Pulse

# Clean previous builds
mvn clean

# Build the JAR
mvn package -DskipTests
```

**Expected**: `target/performancepulse-0.0.1-SNAPSHOT.jar` is created

---

## Step 2: Start Docker Services

```bash
# Start PostgreSQL and Spring Boot
docker compose up -d

# Watch the logs
docker compose logs -f app

# Wait for: "Started PerformancePulseApplication in X seconds"
```

**Expected Output**:
```
postgres-15-alpine | [ ... ] LOG: database system is ready to accept connections
performance-pulse-app | Started PerformancePulseApplication in 12.345 seconds
```

---

## Step 3: Verify Tables are Created

### Option A: Using PostgreSQL CLI
```bash
# Connect to the database from command line
docker exec -it performance-pulse-postgres psql -U postgres -d performance_pulse

# List tables
\dt

# Expected: employees, review_cycles, performance_reviews, goals tables
```

### Option B: Using DBeaver or pgAdmin
1. Create a new PostgreSQL connection:
   - Host: `localhost`
   - Port: `5432`
   - Database: `performance_pulse`
   - Username: `postgres`
   - Password: `postgres`

2. Connect and view tables in the public schema

---

## Step 4: Access Swagger UI

Navigate to: **http://localhost:8080/swagger-ui.html**

**Expected**: 
- Swagger UI loads successfully
- API title shows: "Talent Performance Pulse API"
- No endpoints shown yet (they'll be added in Phase 1)

---

## Step 5: Test Health Check

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Expected Response:
# {"status":"UP","components":{...},"checks":{...}}
```

---

## Step 6: Verify Database Tables Structure

```bash
# View employees table schema
docker exec -it performance-pulse-postgres psql -U postgres -d performance_pulse -c "
  SELECT column_name, data_type, is_nullable
  FROM information_schema.columns
  WHERE table_name = 'employees'
  ORDER BY ordinal_position;
"
```

**Expected Columns**:
- id (UUID)
- first_name (varchar)
- last_name (varchar)
- email (varchar)
- department (department_enum)
- job_title (varchar)
- manager_id (UUID, nullable)
- joining_date (date)
- status (employee_status_enum, default ACTIVE)
- created_at (timestamp)
- updated_at (timestamp)
- created_by (varchar)
- updated_by (varchar)
- version (bigint)

---

## Step 7: Troubleshooting

### PostgreSQL Container Won't Start
```bash
# Check Docker logs
docker compose logs postgres

# Possible issues:
# - Port 5432 already in use: kill the process or change port in docker-compose.yml
# - No disk space: clear Docker images and volumes (docker system prune)
```

### Spring Boot Won't Connect to PostgreSQL
```bash
# Check application logs
docker compose logs app

# Verify database credentials in application.yml match docker-compose.yml
# Default: postgres/postgres on localhost:5432
```

### Tables Not Created
```bash
# Check ddl-auto setting in application.yml
# Should be: ddl-auto: create (or create-drop)

# If table already exists:
# Stop containers, delete postgres-data volume, restart
docker compose down -v
docker compose up -d
```

---

## Step 8: Verify Enum Types

```bash
# List all enum types in the database
docker exec -it performance-pulse-postgres psql -U postgres -d performance_pulse -c "
  SELECT typname, typtype, string_agg(enumlabel, ', ')
  FROM pg_type
  LEFT JOIN pg_enum ON pg_type.oid = pg_enum.enumtypid
  WHERE typtype = 'e'
  GROUP BY typname, typtype;
"

# Expected: 5 enum types (department_enum, employee_status_enum, etc.)
```

---

## Step 9: Check Cache Configuration

```bash
# Check if caching is active
curl http://localhost:8080/actuator/caches

# Expected Response: JSON with cache managers and cache names
# {
#   "cacheManagers": {
#     "cacheManager": {
#       "caches": [
#         "cycle-summary",
#         "employee-list",
#         "top-performers"
#       ]
#     }
#   }
# }
```

---

## Step 10: View Application Logs

```bash
# Filter for key startup messages
docker compose logs app | grep -E "Starting|Started|Created|Cache|Hibernate"
```

---

## Stop and Cleanup

```bash
# Stop all containers
docker compose down

# Remove data volume (for fresh start)
docker compose down -v

# Remove Docker image (if building again)
docker rmi performance-pulse:latest
```

---

## Phase 0 Success Indicators ✅

- [x] Docker containers start without errors
- [x] PostgreSQL database `performance_pulse` exists
- [x] 5 tables created: employees, review_cycles, performance_reviews, goals
- [x] 5 enum types created: department_enum, employee_status_enum, cycle_status_enum, review_type_enum, goal_status_enum
- [x] Spring Boot application runs on http://localhost:8080
- [x] Swagger UI accessible and shows correct API title
- [x] Actuator health check returns "UP"
- [x] Cache manager has 3 named caches
- [x] All audit columns present in tables (created_at, updated_at, created_by, updated_by, version)

---

## Environment Variables (Optional)

Override defaults in `docker compose up`:

```bash
# Set custom database name
POSTGRES_DB=my_custom_db docker compose up -d

# Set custom Spring profile
SPRING_PROFILES_ACTIVE=dev docker compose up -d
```

---

## Next Steps

Once Phase 0 is verified:

1. ✅ **Phase 0 Complete**
2. → Proceed to **Phase 1: Employee & Cycle Management**
   - Build Employee entity and repository
   - Build ReviewCycle entity and repository
   - Create services and DTOs
   - Build controllers with POST /employees, GET /employees, POST /cycles

---

## References

- [Spring Boot JPA Configuration](https://spring.io/guides/gs/accessing-data-jpa/)
- [Hibernate DDL Auto Options](https://hibernate.org/orm/documentation/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
- [SpringDoc OpenAPI](https://springdoc.org/)
- [Caffeine Caching](https://github.com/ben-manes/caffeine)
