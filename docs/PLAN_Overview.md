# MLP-1 Implementation Plan Overview

**Total Phases**: 4  
**Total Estimated Duration**: 150 minutes (~2.5 hours)  
**Scope**: Build the Talent Performance Pulse MVP with 6 required REST endpoints

---

## Implementation Roadmap

```
Phase 0: Foundation & Infrastructure (30 mins)
    ↓
Phase 1: Employee & Cycle Management (40 mins)
    ↓
Phase 2: Review Management (35 mins)
    ├─ Depends on: Phase 0, Phase 1
    ↓
Phase 3: Goals & Analytics (45 mins)
    └─ Depends on: Phase 0, Phase 1, Phase 2
```

---

## Required Endpoints Summary

| Phase | Endpoint | Method | Purpose |
|-------|----------|--------|---------|
| 1 | `/api/v1/employees` | POST | Create employee |
| 1 | `/api/v1/employees` | GET | Filter employees by department and minRating (cached) |
| 1 | `/api/v1/cycles` | POST | Create review cycle |
| 2 | `/api/v1/reviews` | POST | Submit performance review (evicts cache) |
| 2 | `/api/v1/employees/{id}/reviews` | GET | Get all reviews for an employee |
| 3 | `/api/v1/cycles/{id}/summary` | GET | Get cycle summary with metrics (cached) |

**Additional Supporting Endpoint (Phase 3)**:
- POST `/api/v1/goals` — Create goal (required for cycle summary calculations)

---

## Phase Breakdown

### **Phase 0: Foundation & Infrastructure**

**What Gets Built**:
- Database schema (4 tables: employees, review_cycles, performance_reviews, goals)
- 7 database indexes for query performance
- Spring Boot configuration (application.yml)
- All 5 enum types (Department, EmployeeStatus, CycleStatus, ReviewType, GoalStatus)
- Base infrastructure (AuditEntity, GlobalExceptionHandler, Swagger, Cache config)

**Why This Phase**:
- All other phases depend on database and base classes
- Unblocks all 3 subsequent phases

**Time**: 30 mins

---

### **Phase 1: Employee & Cycle Management**

**What Gets Built**:
- Employee entity (with audit fields, version for optimistic locking)
- ReviewCycle entity (with audit fields, version)
- 2 repositories with custom query methods
- 2 services (EmployeeService, ReviewCycleService)
- 4 DTOs (CreateEmployeeRequest, EmployeeResponse, CreateCycleRequest, CycleResponse)
- 2 mappers (EmployeeMapper, CycleMapper)
- 2 controllers with 3 endpoints:
  - POST /employees
  - GET /employees?department=X&minRating=Y (cached)
  - POST /cycles

**Dependencies**: Phase 0

**Why Order**:
- These are base entities that other entities depend on
- Employee must exist before creating reviews and goals
- Cycle must exist before submitting reviews

**Time**: 40 mins

---

### **Phase 2: Review Management**

**What Gets Built**:
- PerformanceReview entity (unique constraint on employee+cycle+reviewer, 4 indexes)
- PerformanceReviewRepository (with 4 custom methods for analytics)
- PerformanceReviewService (with business logic validation)
- 2 DTOs (SubmitReviewRequest, ReviewResponse)
- 1 projection DTO (TopPerformerDTO for queries)
- 1 mapper (ReviewMapper)
- 1 controller with 2 endpoints:
  - POST /reviews (with validation, cache eviction)
  - GET /employees/{id}/reviews (paginated)

**Dependencies**: Phase 0, Phase 1 (needs Employee and ReviewCycle)

**Key Business Logic**:
- Validate cycle is ACTIVE (422 if not)
- Validate employee exists and is ACTIVE (422 if not)
- Check for duplicate (employee+cycle+reviewer); return 409 if exists
- Validate rating 1-5 (400 if invalid)
- Evict cycle-summary and top-performers caches when review submitted

**Time**: 35 mins

---

### **Phase 3: Goals & Analytics**

**What Gets Built**:
- Goal entity (with 2 indexes for efficient queries)
- GoalRepository (with projection for goal stats)
- GoalService (basic CRUD for goal creation)
- AnalyticsService (orchestrates cycle summary calculation with @Cacheable)
- 4 DTOs (CreateGoalRequest, GoalResponse, GoalStatsDTO, CycleSummaryResponse)
- 1 projection DTO (EmployeeSummaryDTO)
- 2 mappers (GoalMapper, and updates to existing mappers)
- 2 controllers:
  - GoalController (POST /goals)
  - ReviewCycleController update (GET /{id}/summary)

**Dependencies**: Phase 0, Phase 1, Phase 2

**Key Features**:
- Get cycle summary with:
  - Average rating (rounded to 2 decimals)
  - Top performer (highest average rating)
  - Goal statistics (total, completed, missed, completion rate)
- 10-minute cache TTL with intelligent eviction
- Employee filtering by department and minRating (5-minute cache)

**Time**: 45 mins

---

## Entity Relationship Diagram (Summary)

```
Employee
  ├─ OneToMany: PerformanceReview (as employee)
  ├─ OneToMany: Goal
  ├─ ManyToOne: Employee (as manager, self-referential, optional)
  └─ OneToMany: Employee (as managed employees)

ReviewCycle
  ├─ OneToMany: PerformanceReview
  └─ OneToMany: Goal

PerformanceReview
  ├─ ManyToOne: Employee (as employee)
  ├─ ManyToOne: ReviewCycle
  └─ ManyToOne: Employee (as reviewer, optional for self-review)

Goal
  ├─ ManyToOne: Employee
  └─ ManyToOne: ReviewCycle
```

---

## Key Design Patterns Used

### **1. Layered Architecture**
- Controllers → Services → Repositories → Entities
- Clear separation of concerns

### **2. DTOs for API Contracts**
- Request DTOs with validation (@NotNull, @Email, etc.)
- Response DTOs with computed fields (@AfterMapping)
- Projection DTOs for efficient queries

### **3. MapStruct Mappers**
- No manual field copying
- Computed fields via @AfterMapping (averageRating, fullName)

### **4. JPA Specifications**
- Dynamic filtering on Employee list (department, minRating)
- No hardcoded WHERE clauses

### **5. Caching Strategy**
- @Cacheable on expensive read operations
- @CacheEvict on write operations
- Separate TTLs for different cache keys

### **6. Optimistic Locking**
- @Version on all AuditEntity subclasses
- Prevents concurrent update conflicts

### **7. Custom Exceptions**
- ResourceNotFoundException (404)
- DuplicateReviewException (409)
- InvalidCycleStateException (422)
- InvalidRatingException (400)
- Centralized handling via @RestControllerAdvice

### **8. Audit Trail**
- createdAt, updatedAt, createdBy, updatedBy (auto-populated)
- AuditAware implementation for current user context

---

## Validation Strategy

### **At DTO Level** (Request validation):
- @NotNull, @NotBlank, @Size, @Email
- @Min/@Max for rating (1-5)
- @PastOrPresent for dates

### **At Service Level** (Business logic):
- Cycle must be ACTIVE to submit review
- Employee must be ACTIVE
- Reviewer (if set) must be ACTIVE
- No duplicate (employee + cycle + reviewer)
- dueDate within cycle range

### **At Database Level** (Constraints):
- UNIQUE (email)
- UNIQUE (cycle_name)
- UNIQUE (employee_id, cycle_id, reviewer_id)
- CHECK (rating BETWEEN 1 AND 5)
- CHECK (end_date > start_date)

---

## Caching & Eviction

| Cache | TTL | Key | Eviction Triggers |
|-------|-----|-----|-------------------|
| `cycle-summary` | 10 mins | cycleId | submitReview, createGoal, updateGoalStatus |
| `employee-list` | 5 mins | dept+minRating | createEmployee, updateEmployee |
| `top-performers` | 10 mins | cycleId | submitReview |

---

## HTTP Status Codes

| Status | Trigger | Controller Method |
|--------|---------|-------------------|
| 201 Created | POST successful | create* methods |
| 200 OK | GET/PATCH successful | get*, list* methods |
| 400 Bad Request | Invalid input (validation failed, date range invalid) | any method |
| 404 Not Found | Resource not found | get*, detail methods |
| 409 Conflict | Duplicate data or finalization guard | submitReview, create* with uniqueness |
| 422 Unprocessable Entity | Invalid business state (cycle not ACTIVE) | submitReview, cycle operations |
| 500 Internal Server Error | Unhandled exception | GlobalExceptionHandler |

---

## Testing Sequence (After Implementation)

1. **Phase 0**: Verify database creation, Swagger UI accessibility
2. **Phase 1**: 
   - POST /employees → expect 201
   - POST /employees (duplicate email) → expect 409
   - GET /employees → expect 200 with pagination
   - POST /cycles → expect 201
3. **Phase 2**:
   - POST /reviews (invalid cycle state) → expect 422
   - POST /reviews → expect 201
   - POST /reviews (duplicate) → expect 409
   - GET /employees/{id}/reviews → expect 200 with list
4. **Phase 3**:
   - POST /goals (invalid date range) → expect 400
   - POST /goals → expect 201
   - GET /cycles/{id}/summary → expect 200 with summary, cached
   - Verify cache eviction on review submission

---

## File Count Summary

| Phase | Entity/Service Files | DTO Files | Controller Files | Other |
|-------|---------------------|-----------|-----------------|-------|
| 0 | 1 (AuditEntity) | 1 (ApiResponse) | 0 | Config (4), Exception (7), Enum (5) |
| 1 | 2 (Employee, Cycle) | 4 (Requests/Responses) | 2 (Controllers) | Mapper (2), Repository (2) |
| 2 | 1 (PerformanceReview) | 3 (Request/Responses) | 1 (Controller) | Mapper (1), Repository (1) |
| 3 | 1 (Goal) + 1 (Service) | 4 (Request/Responses) | 1 (Goal) + mod (Cycle) | Mapper (1), Repository (1) |
| **Total** | **~10 files** | **~15 files** | **5-6 files** | **30+ files** |

---

## Key Checkpoints

### **After Phase 0**:
- ✅ Database initialized and accessible
- ✅ Spring Boot starts without errors
- ✅ All enums available for use

### **After Phase 1**:
- ✅ POST /employees works
- ✅ GET /employees filtering works and is cached
- ✅ POST /cycles works
- ✅ Employee filtering returns correct averageRating

### **After Phase 2**:
- ✅ POST /reviews validates cycle state (422)
- ✅ POST /reviews prevents duplicates (409)
- ✅ POST /reviews validates rating (400)
- ✅ GET /employees/{id}/reviews returns paginated list
- ✅ Cache is evicted on review submission

### **After Phase 3 (Complete)**:
- ✅ POST /goals validates date range (400)
- ✅ GET /cycles/{id}/summary returns full metrics
- ✅ Summary is cached and served from cache on subsequent calls
- ✅ All 6 required endpoints working end-to-end

---

## Constraints Adherence (Per MLP-1)

✅ **"Do not add anything extra"**:
- Only 6 required endpoints + 1 supporting goal creation
- No additional endpoints (update, delete, activate, close, etc.)
- No tests included

✅ **"Build only the endpoints mentioned"**:
- POST /employees ✓
- POST /reviews ✓
- GET /employees/{id}/reviews ✓
- POST /cycles ✓
- GET /cycles/{id}/summary ✓
- GET /employees?department={d}&minRating={r} ✓

✅ **"Clean structure, validation, error handling"**:
- Layered architecture with clear separation
- DTO validation + Service validation + DB constraints
- Centralized exception handling with proper HTTP status codes

✅ **"Remove extra files, clean structures"**:
- No HelloController or test classes
- Only essential configuration files
- Maven pom.xml includes only required dependencies

---

## Success Metrics

By the end of all 4 phases:
- 6 REST endpoints fully functional
- 4 entity tables with proper indexing
- 3 services handling business logic
- Caching strategy implemented with intelligent eviction
- Exception handling covering all error cases
- Request/response validation at multiple layers
- API documentation via Swagger

**Ready for**: Integration testing, manual testing, or future enhancements (Phase 0 supports extensions)

---

## Document References

- [Phase 0: Foundation & Infrastructure](PLAN_Phase-0-Foundation.md)
- [Phase 1: Employee & Cycle Management](PLAN_Phase-1-Employee-Cycle.md)
- [Phase 2: Review Management](PLAN_Phase-2-Reviews.md)
- [Phase 3: Goals & Analytics](PLAN_Phase-3-Goals-Analytics.md)

For detailed implementation notes and code examples, refer to the individual phase documents.
