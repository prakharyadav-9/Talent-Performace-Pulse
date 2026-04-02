# MLP-1 Phase 0: Foundation & Infrastructure

**Status**: Phase 1 of 4  
**Objective**: Initialize database schema, configure Spring Boot application, and establish base infrastructure  
**Estimated Duration**: 30 mins  

---

## Deliverables

- [ ] PostgreSQL database initialization script (DDL) with all tables and indexes
- [ ] `application.yml` with database, JPA, cache, and logging configuration
- [ ] Maven dependencies (`pom.xml`) update with Spring Boot 3.x, PostgreSQL, MapStruct, Lombok, SpringDoc
- [ ] `AuditEntity` abstract base class with audit columns and optimistic locking
- [ ] `AuditAwareImpl` for Spring Data JPA audit awareness
- [ ] `ApiResponse<T>` wrapper for consistent API responses
- [ ] All custom exception classes (ResourceNotFoundException, DuplicateReviewException, InvalidCycleStateException, etc.)
- [ ] `GlobalExceptionHandler` for centralized error handling
- [ ] `SwaggerConfig` for SpringDoc OpenAPI documentation
- [ ] `CacheConfig` for Caffeine cache setup

---

## Database Schema

### Tables:
1. **employees** — id (UUID), firstName, lastName, email (unique), department (ENUM), jobTitle, managerId (FK), joiningDate, status (ENUM, default ACTIVE), createdAt, updatedAt, createdBy, updatedBy, version
2. **review_cycles** — id (UUID), name (unique), startDate, endDate, status (ENUM, default UPCOMING), createdAt, updatedAt, createdBy, updatedBy, version
3. **performance_reviews** — id (UUID), employee_id (FK), cycle_id (FK), reviewer_id (FK, nullable), rating (CHECK 1-5), notes, reviewType (ENUM), submittedAt, isFinalized (BOOLEAN), createdAt, updatedAt, createdBy, updatedBy, version, unique(employee_id, cycle_id, reviewer_id)
4. **goals** — id (UUID), employee_id (FK), cycle_id (FK), title, description, status (ENUM), dueDate, completedAt (nullable), weight (default 1), createdAt, updatedAt, createdBy, updatedBy, version

### Indexes:
- `idx_review_cycle_id` on performance_reviews(cycle_id)
- `idx_review_employee_id` on performance_reviews(employee_id)
- `idx_review_rating` on performance_reviews(cycle_id, rating DESC)
- `idx_employee_dept_status` on employees(department, status)
- `idx_goal_employee_cycle` on goals(employee_id, cycle_id)
- `idx_goal_status` on goals(cycle_id, status)
- `idx_review_finalized` on performance_reviews(is_finalized, cycle_id)

---

## Enums to Create

- **Department**: ENGINEERING, HR, SALES, MARKETING, FINANCE, OPERATIONS
- **EmployeeStatus**: ACTIVE, INACTIVE, ON_LEAVE
- **CycleStatus**: UPCOMING, ACTIVE, CLOSED
- **ReviewType**: SELF, PEER, MANAGER
- **GoalStatus**: PENDING, IN_PROGRESS, COMPLETED, MISSED

---

## Files to Create/Modify

| File | Type | Purpose |
|------|------|---------|
| `pom.xml` | Modify | Add Spring Boot 3.x, JPA, PostgreSQL, MapStruct, Lombok, SpringDoc OpenAPI, Caffeine |
| `src/main/resources/application.yml` | Create | Database, JPA, cache, pagination config |
| `docker-compose.yml` | Verify | PostgreSQL 15 service |
| `docker/postgres/init/01_init.sql` | Create | Complete DDL with all tables and indexes |
| `src/main/java/com/hr/performancepulse/entity/AuditEntity.java` | Create | Base audit class |
| `src/main/java/com/hr/performancepulse/audit/AuditAwareImpl.java` | Create | Audit awareness implementation |
| `src/main/java/com/hr/performancepulse/config/AppConfig.java` | Create | Bean configuration for MapStruct, etc. |
| `src/main/java/com/hr/performancepulse/config/CacheConfig.java` | Create | Caffeine cache manager |
| `src/main/java/com/hr/performancepulse/config/SwaggerConfig.java` | Create | SpringDoc OpenAPI configuration |
| `src/main/java/com/hr/performancepulse/dto/response/ApiResponse.java` | Create | Standard response envelope |
| `src/main/java/com/hr/performancepulse/enums/*.java` | Create | All enum classes (Department, EmployeeStatus, etc.) |
| `src/main/java/com/hr/performancepulse/exception/*.java` | Create | All exception classes |
| `src/main/java/com/hr/performancepulse/exception/GlobalExceptionHandler.java` | Create | @RestControllerAdvice |

---

## Key Configuration (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/performance_pulse
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 20
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        default_batch_fetch_size: 50
  cache:
    type: caffeine
  data:
    web:
      pageable:
        max-page-size: 100

logging:
  level:
    root: INFO
    org.hibernate.SQL: DEBUG
```

---

## Success Criteria

- [ ] PostgreSQL container starts and initializes schema
- [ ] Spring Boot application starts without dependency errors
- [ ] Swagger UI accessible at `/swagger-ui.html`
- [ ] All tables and indexes created in database
- [ ] GlobalExceptionHandler catches and maps exceptions correctly

---

## Next Phase

Proceed to **Phase 1: Employee & Cycle Management** to build core entities and endpoints.

---

## References

- LLD Section 3: Domain Model & Entity Design
- LLD Section 4: Database Schema & Indexing
- LLD Section 10: Exception Handling
- LLD Section 12: Configuration
