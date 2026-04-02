**Talent Performance Pulse**

Low-Level Design Document

Version: 1.0 \| Status: Draft \| Audience: Backend Engineers

**Table of Contents**

**1. Overview**

This Low-Level Design (LLD) document provides engineering-ready
specifications for the Talent Performance Pulse backend. It maps every
component from the High-Level Design (HLD) to concrete class
hierarchies, database schemas, service contracts, and query strategies.
It is intentionally designed with extension hooks to accommodate future
capabilities without breaking existing contracts.

  ----------- -------------------------------------------------------------
  **Scope**   Spring Boot 3.x · PostgreSQL 15 · Java 17 · JPA/Hibernate 6 ·
              Spring Cache (Caffeine) · MapStruct · Lombok

  ----------- -------------------------------------------------------------

**2. Package Structure**

The project follows a layered architecture with strict package
boundaries. Each layer depends only on the layer directly below it.

  ---------------------- ------------------------------------------------
  **Package**            **Key Classes / Notes**

  **config/**            AppConfig, CacheConfig, SecurityConfig,
                         SwaggerConfig

  **controller/**        EmployeeController, ReviewCycleController,
                         PerformanceReviewController, GoalController

  **service/**           Interfaces: EmployeeService, ReviewService,
                         CycleService, GoalService, AnalyticsService

  **service/impl/**      Concrete implementations of all service
                         interfaces

  **repository/**        EmployeeRepository, PerformanceReviewRepository,
                         ReviewCycleRepository, GoalRepository

  **entity/**            Employee, ReviewCycle, PerformanceReview, Goal,
                         AuditEntity (base)

  **dto/request/**       CreateEmployeeRequest, SubmitReviewRequest,
                         CreateCycleRequest, CreateGoalRequest

  **dto/response/**      EmployeeResponse, ReviewResponse,
                         CycleSummaryResponse, GoalResponse

  **mapper/**            EmployeeMapper, ReviewMapper, GoalMapper
                         (MapStruct)

  **exception/**         GlobalExceptionHandler,
                         ResourceNotFoundException,
                         DuplicateReviewException, etc.

  **util/**              PageableUtils, ValidationUtils,
                         DateRangeValidator

  **audit/**             AuditAware, EntityAuditListener (extensible hook
                         for audit trail)
  ---------------------- ------------------------------------------------

**3. Domain Model & Entity Design**

**3.1 Base: AuditEntity**

All entities extend this abstract class to inherit audit columns
automatically via \@EntityListeners.

  ---------------------- ------------------------------------------------
  **Field**              **Type & Notes**

  **id**                 UUID -- \@GeneratedValue(IDENTITY) primary key

  **createdAt**          LocalDateTime -- \@CreatedDate (Spring Data JPA
                         auditing)

  **updatedAt**          LocalDateTime -- \@LastModifiedDate

  **createdBy**          String -- \@CreatedBy (populated by AuditAware
                         implementation)

  **updatedBy**          String -- \@LastModifiedBy

  **version**            Long -- \@Version for optimistic locking
                         (prevents lost updates)
  ---------------------- ------------------------------------------------

  --------------- -------------------------------------------------------------
  **Enhancement   \@Version optimistic locking prevents concurrent review
  Hook**          overwrites and is the foundation for an eventual
                  event-sourcing layer.

  --------------- -------------------------------------------------------------

**3.2 Entity: Employee**

  ---------------------- ------------------------------------------------
  **Field**              **Details**

  **id**                 UUID (PK)

  **firstName /          VARCHAR(100) NOT NULL
  lastName**             

  **email**              VARCHAR(255) UNIQUE NOT NULL

  **department**         ENUM (ENGINEERING, HR, SALES, MARKETING,
                         FINANCE, OPERATIONS)

  **jobTitle**           VARCHAR(150) NOT NULL

  **managerId**          UUID (FK → Employee, nullable) --
                         self-referential for manager hierarchy

  **joiningDate**        DATE NOT NULL

  **status**             ENUM (ACTIVE, INACTIVE, ON_LEAVE) DEFAULT ACTIVE

  **reviews**            \@OneToMany(mappedBy=\'employee\', cascade=ALL,
                         orphanRemoval=true)

  **goals**              \@OneToMany(mappedBy=\'employee\', cascade=ALL,
                         orphanRemoval=true)
  ---------------------- ------------------------------------------------

  --------------- -------------------------------------------------------------
  **Enhancement   managerId enables org-tree queries (recursive CTEs) for team
  Hook**          roll-up analytics in future iterations.

  --------------- -------------------------------------------------------------

**3.3 Entity: ReviewCycle**

  ---------------------- ------------------------------------------------
  **Field**              **Details**

  **id**                 UUID (PK)

  **name**               VARCHAR(100) UNIQUE NOT NULL -- e.g., \'Q1
                         2025\'

  **startDate**          DATE NOT NULL

  **endDate**            DATE NOT NULL

  **status**             ENUM (UPCOMING, ACTIVE, CLOSED) DEFAULT UPCOMING

  **reviews**            \@OneToMany(mappedBy=\'cycle\')

  **goals**              \@OneToMany(mappedBy=\'cycle\')
  ---------------------- ------------------------------------------------

  ---------------- -------------------------------------------------------------
  **Constraint**   CHECK (endDate \> startDate) enforced at DB level. Status
                   transitions: UPCOMING → ACTIVE → CLOSED only (validated in
                   service layer).

  ---------------- -------------------------------------------------------------

**3.4 Entity: PerformanceReview**

  ---------------------- ------------------------------------------------
  **Field**              **Details**

  **id**                 UUID (PK)

  **employee**           \@ManyToOne(fetch=LAZY) -- NOT NULL

  **cycle**              \@ManyToOne(fetch=LAZY) -- NOT NULL

  **reviewer**           \@ManyToOne(fetch=LAZY) -- references Employee;
                         nullable for self-review

  **rating**             INTEGER NOT NULL -- CHECK (rating BETWEEN 1 AND
                         5)

  **notes**              TEXT -- reviewer comments

  **reviewType**         ENUM (SELF, PEER, MANAGER) -- drives analytics
                         segmentation

  **submittedAt**        LocalDateTime -- set in service layer on submit

  **isFinalized**        BOOLEAN DEFAULT false -- guards against edits
                         post-close
  ---------------------- ------------------------------------------------

  -------------- ---------------------------------------------------------------------------------
  **Unique       \@Table(uniqueConstraints =
  Constraint**   \@UniqueConstraint(columnNames={\'employee_id\',\'cycle_id\',\'reviewer_id\'}))
                 prevents duplicate reviews from the same reviewer in one cycle.

  -------------- ---------------------------------------------------------------------------------

**3.5 Entity: Goal**

  ---------------------- ------------------------------------------------
  **Field**              **Details**

  **id**                 UUID (PK)

  **employee**           \@ManyToOne(fetch=LAZY) -- NOT NULL

  **cycle**              \@ManyToOne(fetch=LAZY) -- NOT NULL

  **title**              VARCHAR(255) NOT NULL

  **description**        TEXT

  **status**             ENUM (PENDING, IN_PROGRESS, COMPLETED, MISSED)

  **dueDate**            DATE -- must be within cycle date range
                         (validated in service)

  **completedAt**        LocalDateTime -- set when status transitions to
                         COMPLETED

  **weight**             INTEGER DEFAULT 1 -- for weighted goal scoring
                         (future)
  ---------------------- ------------------------------------------------

  --------------- -------------------------------------------------------------
  **Enhancement   The weight field enables weighted goal completion scores in
  Hook**          future analytics endpoints without schema changes.

  --------------- -------------------------------------------------------------

**4. Database Schema & Indexing Strategy**

**4.1 DDL Overview**

Key schema decisions and critical constraints:

  ------------------------- ------------------------------------------------
  **Table**                 **Notable Constraints**

  **employees**             UUID PK, unique email, department + status for
                            filtered listing queries

  **review_cycles**         UUID PK, unique name, date range check
                            constraint

  **performance_reviews**   UUID PK, unique(employee_id, cycle_id,
                            reviewer_id), rating check 1--5

  **goals**                 UUID PK, FK to employees and review_cycles,
                            status enum
  ------------------------- ------------------------------------------------

**4.2 Index Strategy**

Indexes are designed specifically for the analytical and filter queries
described in the HLD scalability section.

  -------------------------- ----------------------------------- ---------------------------
  **Index Name**             **Columns**                         **Purpose**

  idx_review_cycle_id        performance_reviews(cycle_id)       Speeds up cycle summary
                                                                 aggregation over 100k+
                                                                 reviews

  idx_review_employee_id     performance_reviews(employee_id)    Employee review history
                                                                 retrieval

  idx_review_rating          performance_reviews(cycle_id,       Composite index for
                             rating DESC)                        top-performer query without
                                                                 sort pass

  idx_employee_dept_status   employees(department, status)       Department filter +
                                                                 active-only queries

  idx_goal_employee_cycle    goals(employee_id, cycle_id)        Goal count aggregation per
                                                                 employee per cycle

  idx_goal_status            goals(cycle_id, status)             Filter goals by status
                                                                 within a cycle for
                                                                 summaries

  idx_review_finalized       performance_reviews(is_finalized,   Locks analytical queries to
                             cycle_id)                           finalized data only
  -------------------------- ----------------------------------- ---------------------------

  -------------- -------------------------------------------------------------
  **N+1          All association fetches use JOIN FETCH in JPQL where batches
  Prevention**   of entities are needed. Single-entity endpoints use
                 \@EntityGraph to specify eager paths selectively without
                 global EAGER fetch type.

  -------------- -------------------------------------------------------------

**5. Repository Layer**

**5.1 EmployeeRepository**

Extends JpaRepository\<Employee, UUID\> with
JpaSpecificationExecutor\<Employee\> for dynamic filtering.

  ----------------------------------- ------------------------------------------------
  **Method Signature**                **Purpose / Query Notes**

  **findAllByDeptAndMinRating(dept,   Native SQL with GROUP BY avg subquery; used by
  minAvg, pageable)**                 GET /employees filter endpoint

  **findByIdWithReviews(id)**         JPQL JOIN FETCH employee.reviews r JOIN FETCH
                                      r.cycle -- avoids N+1 for review history
                                      endpoint

  **existsByEmail(email)**            Existence check before create to return 409
                                      instead of DB constraint violation

  **findByManagerId(managerId)**      Future: fetch all direct reports for a manager
  ----------------------------------- ------------------------------------------------

**5.2 PerformanceReviewRepository**

  ----------------------------------------------------- ------------------------------------------------
  **Method Signature**                                  **Purpose / Query Notes**

  **findByCycleIdWithEmployees(cycleId)**               JOIN FETCH review.employee -- batch load for
                                                        summary calculation

  **getAverageRatingByCycleId(cycleId)**                \@Query native: SELECT AVG(rating) FROM
                                                        performance_reviews WHERE cycle_id=?

  **getTopPerformerByCycleId(cycleId)**                 Native: GROUP BY employee_id ORDER BY
                                                        AVG(rating) DESC LIMIT 1

  **getGoalCountsByCycleId(cycleId)**                   Native JOIN with goals table; returns DTO
                                                        projection with completed/missed/total

  **existsByEmployeeIdAndCycleIdAndReviewerId(\...)**   Duplicate check before insert; throws
                                                        DuplicateReviewException if true
  ----------------------------------------------------- ------------------------------------------------

**5.3 GoalRepository**

  ------------------------------------- ------------------------------------------------
  **Method Signature**                  **Purpose / Query Notes**

  **findByEmployeeIdAndCycleId(empId,   JPQL query for an employee\'s goals in a cycle
  cycleId)**                            

  **countByStatusAndCycleId(status,     Count query for summary metrics
  cycleId)**                            

  **findByCycleIdAndStatusIn(cycleId,   Bulk status filter for cycle close processing
  statuses)**                           
  ------------------------------------- ------------------------------------------------

  -------------- -------------------------------------------------------------
  **Projection   Analytical summary queries return lightweight DTO projections
  Pattern**      (interface-based or record-based) instead of full entity
                 graphs to avoid hydration cost.

  -------------- -------------------------------------------------------------

**6. Service Layer**

**6.1 EmployeeService**

Interface + EmployeeServiceImpl annotated with \@Service
\@Transactional.

  ------------------------------------------- ------------------------------------------------
  **Method**                                  **Behaviour**

  **createEmployee(CreateEmployeeRequest)**   Validates uniqueness → maps DTO to entity →
                                              saves → returns EmployeeResponse

  **getEmployee(UUID id)**                    Fetches with reviews joined; throws
                                              ResourceNotFoundException if absent

  **listEmployees(EmployeeFilterRequest,      Builds JPA Specification from filter params;
  Pageable)**                                 passes to repository with pagination

  **updateEmployee(UUID,                      Partial update via mapped non-null fields;
  UpdateEmployeeRequest)**                    increments \@Version

  **deactivateEmployee(UUID)**                Sets status=INACTIVE; does not delete to
                                              preserve review history
  ------------------------------------------- ------------------------------------------------

**6.2 PerformanceReviewService**

  --------------------------------------- ------------------------------------------------
  **Method**                              **Behaviour**

  **submitReview(SubmitReviewRequest)**   1\. Validates cycle is ACTIVE. 2. Duplicate
                                          check. 3. Validates rating 1--5. 4. Persists. 5.
                                          Evicts cycle summary cache.

  **getReviewsByEmployee(UUID empId)**    Delegates to repository JOIN FETCH method;
                                          returns paginated list

  **finalizeReview(UUID reviewId)**       Sets isFinalized=true; prevents further edits
                                          (guards analytics integrity)

  **deleteReview(UUID)**                  Hard delete only if not finalized; throws
                                          exception otherwise
  --------------------------------------- ------------------------------------------------

**6.3 CycleService**

  ------------------------------------- ------------------------------------------------
  **Method**                            **Behaviour**

  **createCycle(CreateCycleRequest)**   Validates date range; status defaults to
                                        UPCOMING

  **activateCycle(UUID)**               UPCOMING→ACTIVE; validates no overlap with
                                        another ACTIVE cycle

  **closeCycle(UUID)**                  ACTIVE→CLOSED; marks all PENDING goals as
                                        MISSED; evicts cache

  **getCycleSummary(UUID)**             See Section 7 -- delegated to AnalyticsService
                                        with caching
  ------------------------------------- ------------------------------------------------

**6.4 AnalyticsService**

  ----------------------------- ------------------------------------------------
  **Method**                    **Behaviour**

  **buildCycleSummary(UUID      Orchestrates: avgRating + topPerformer +
  cycleId)**                    goalCounts in a single service call

  **getTopPerformers(UUID       Enhancement hook: paginated top-N performers
  cycleId, int n)**             

  **getDepartmentSummary(UUID   Enhancement hook: breaks down avg rating by
  cycleId)**                    department

  **getTrendForEmployee(UUID    Enhancement hook: rating trend across all cycles
  empId)**                      for one employee
  ----------------------------- ------------------------------------------------

  --------------- -------------------------------------------------------------
  **Transaction   All write methods in service impls use
  Boundary**      \@Transactional(rollbackFor=Exception.class). Read-only
                  methods use \@Transactional(readOnly=true) to enable
                  Hibernate read-optimizations and connection pool routing to
                  read replicas in future.

  --------------- -------------------------------------------------------------

**7. Caching Design**

Spring Cache abstraction backed by Caffeine is applied at the service
layer, not the controller, to keep cache benefits available even for
internal service-to-service calls.

  ----------------- ------------------ ----------- -------------------------
  **Cache Name**    **Key**            **TTL**     **Eviction Trigger**

  cycle-summary     cycleId            10 min      \@CacheEvict on
                                                   submitReview, closeCycle

  employee-list     dept + minRating   5 min       \@CacheEvict on
                    hash                           createEmployee,
                                                   updateEmployee

  employee-detail   empId              15 min      \@CacheEvict on update,
                                                   deactivate

  top-performers    cycleId + n        10 min      \@CacheEvict on
                                                   submitReview
  ----------------- ------------------ ----------- -------------------------

  --------------- -------------------------------------------------------------
  **Enhancement   CacheConfig is abstracted behind Spring\'s CacheManager
  Hook**          interface. Swapping from Caffeine to Redis (for distributed
                  deployments) requires changing only the CacheConfig bean ---
                  no service code changes.

  --------------- -------------------------------------------------------------

**8. Controller & API Contract**

**8.1 REST Conventions**

All controllers extend a BaseController which injects a
ResponseEntityBuilder utility for consistent envelope wrapping.

  ---------------------- ------------------------------------------------
  **Convention**         **Detail**

  **Success wrapper**    { \"status\": \"success\", \"data\": {...},
                         \"timestamp\": \"...\" }

  **Error wrapper**      { \"status\": \"error\", \"code\": \"ERR_CODE\",
                         \"message\": \"...\", \"timestamp\": \"...\" }

  **Pagination wrapper** { \"content\": \[...\], \"page\": 0, \"size\":
                         20, \"totalElements\": 450 }

  **HTTP semantics**     201 Created for POST; 200 OK for GET/PATCH; 204
                         No Content for DELETE; 409 Conflict for
                         duplicates; 404 for missing resources
  ---------------------- ------------------------------------------------

**8.2 EmployeeController -- /api/v1/employees**

  ---------- ------------------ ---------------------------------------- --------------------------
  **HTTP**   **Path**           **Request Body / Params**                **Response**

  POST       /                  CreateEmployeeRequest                    201 EmployeeResponse

  GET        /                  ?department=&minAvgRating=&page=&size=   200
                                                                         Page\<EmployeeResponse\>

  GET        /{id}              ---                                      200 EmployeeResponse (with
                                                                         reviews)

  PATCH      /{id}              UpdateEmployeeRequest (partial)          200 EmployeeResponse

  DELETE     /{id}              ---                                      204 (soft delete:
                                                                         status=INACTIVE)

  GET        /{id}/reviews      ?page=&size=                             200 Page\<ReviewResponse\>

  GET        /{id}/goals        ?cycleId= (optional)                     200 List\<GoalResponse\>
  ---------- ------------------ ---------------------------------------- --------------------------

**8.3 ReviewCycleController -- /api/v1/cycles**

  ---------- ---------------------- ------------------------- ------------------------------
  **HTTP**   **Path**               **Request / Params**      **Response**

  POST       /                      CreateCycleRequest        201 CycleResponse

  POST       /{id}/activate         ---                       200 CycleResponse

  POST       /{id}/close            ---                       200 CycleResponse

  GET        /{id}/summary          ---                       200 CycleSummaryResponse
                                                              (cached)

  GET        /{id}/top-performers   ?n=10                     200
                                                              List\<TopPerformerResponse\>
  ---------- ---------------------- ------------------------- ------------------------------

**8.4 PerformanceReviewController -- /api/v1/reviews**

  ---------- ------------------ ------------------------- ----------------------
  **HTTP**   **Path**           **Request**               **Response**

  POST       /                  SubmitReviewRequest       201 ReviewResponse

  GET        /{id}              ---                       200 ReviewResponse

  PATCH      /{id}/finalize     ---                       200 ReviewResponse

  DELETE     /{id}              ---                       204 (only if not
                                                          finalized)
  ---------- ------------------ ------------------------- ----------------------

**8.5 GoalController -- /api/v1/goals**

  ---------- ------------------ ------------------------- ----------------------
  **HTTP**   **Path**           **Request**               **Response**

  POST       /                  CreateGoalRequest         201 GoalResponse

  PATCH      /{id}/status       UpdateGoalStatusRequest   200 GoalResponse

  DELETE     /{id}              ---                       204
  ---------- ------------------ ------------------------- ----------------------

**9. DTO Specifications**

**9.1 CreateEmployeeRequest**

  ---------------------- ------------------------------------------------
  **Field**              **Validation**

  **firstName**          \@NotBlank \@Size(max=100)

  **lastName**           \@NotBlank \@Size(max=100)

  **email**              \@NotBlank \@Email \@Size(max=255)

  **department**         \@NotNull (Department enum)

  **jobTitle**           \@NotBlank \@Size(max=150)

  **managerId**          UUID (nullable)

  **joiningDate**        \@NotNull \@PastOrPresent (LocalDate)
  ---------------------- ------------------------------------------------

**9.2 SubmitReviewRequest**

  ---------------------- ------------------------------------------------
  **Field**              **Validation**

  **employeeId**         \@NotNull UUID

  **cycleId**            \@NotNull UUID

  **reviewerId**         UUID (nullable -- null implies self-review)

  **reviewType**         \@NotNull ReviewType enum

  **rating**             \@NotNull \@Min(1) \@Max(5)

  **notes**              \@Size(max=4000)
  ---------------------- ------------------------------------------------

**9.3 CycleSummaryResponse**

  ------------------------ ------------------------------------------------
  **Field**                **Type / Notes**

  **cycleId / cycleName**  UUID + String

  **totalReviews**         long

  **averageRating**        double (rounded to 2dp)

  **topPerformer**         EmployeeSummaryDTO { id, name, avgRating }

  **goalStats**            GoalStatsDTO { total, completed, missed,
                           inProgress, completionRate }

  **ratingDistribution**   Map\<Integer, Long\> -- count per rating 1..5
                           (enhancement hook for chart rendering)
  ------------------------ ------------------------------------------------

  ------------- -------------------------------------------------------------
  **Mapping**   All DTO ↔ Entity conversions are handled by MapStruct
                mappers. No manual field copying in service or controller
                code. Custom \@AfterMapping hooks handle computed fields like
                completionRate.

  ------------- -------------------------------------------------------------

**10. Exception Handling**

A single \@RestControllerAdvice class (GlobalExceptionHandler) handles
all exceptions and maps them to RFC 7807 Problem Details responses.

  ----------------------------------- --------------- -------------------------------
  **Exception Class**                 **HTTP Status** **Error Code**

  ResourceNotFoundException           404 Not Found   RESOURCE_NOT_FOUND

  DuplicateReviewException            409 Conflict    DUPLICATE_REVIEW

  InvalidCycleStateException          422             INVALID_CYCLE_STATE
                                      Unprocessable   
                                      Entity          

  ReviewFinalizedException            409 Conflict    REVIEW_FINALIZED

  InvalidRatingException              400 Bad Request INVALID_RATING

  MethodArgumentNotValidException     400 Bad Request VALIDATION_FAILED

  DataIntegrityViolationException     409 Conflict    DB_CONSTRAINT_VIOLATED

  OptimisticLockingFailureException   409 Conflict    CONCURRENT_UPDATE_CONFLICT

  Exception (catch-all)               500 Internal    INTERNAL_ERROR
                                      Server Error    
  ----------------------------------- --------------- -------------------------------

**11. Business Validation Rules**

  ---------------------- ------------------------------------------------
  **Rule**               **Logic**

  **Review submission**  Cycle must be in ACTIVE state; employee must be
                         ACTIVE; reviewer (if set) must be ACTIVE; no
                         duplicate (employee + cycle + reviewer)

  **Cycle activation**   No other cycle may be ACTIVE; start date must be
                         in future or today

  **Cycle closure**      Must be currently ACTIVE; all goals not yet
                         terminal are auto-set to MISSED

  **Goal creation**      dueDate must be within cycle.startDate ..
                         cycle.endDate; cycle must not be CLOSED

  **Employee             Cannot deactivate if employee has ACTIVE review
  deactivation**         cycle participation in current cycle

  **Rating value**       Integer 1--5 inclusive; enforced at DTO
                         (@Min/@Max) and at DB CHECK constraint

  **Review edit guard**  Once isFinalized=true, any PATCH to rating/notes
                         returns 409 ReviewFinalizedException
  ---------------------- ------------------------------------------------

**12. Configuration & Infrastructure**

**12.1 application.yml Key Properties**

  -------------------------------------------------------------- ------------------------------------------------
  **Property**                                                   **Value & Rationale**

  **spring.datasource.hikari.maximumPoolSize**                   20 -- tuned for 500 concurrent managers (with
                                                                 horizontal scaling)

  **spring.jpa.open-in-view**                                    false -- prevents accidental lazy loading in
                                                                 controller layer

  **spring.jpa.properties.hibernate.default_batch_fetch_size**   50 -- Hibernate batch loading for collections to
                                                                 reduce query count

  **spring.cache.caffeine.spec**                                 maximumSize=1000,expireAfterWrite=10m --
                                                                 overridden per-cache in CacheConfig

  **spring.data.web.pageable.max-page-size**                     100 -- guards against oversized page requests

  **server.tomcat.threads.max**                                  200 -- aligned with HikariCP pool sizing
  -------------------------------------------------------------- ------------------------------------------------

**12.2 Swagger / OpenAPI 3**

SpringDoc OpenAPI is configured in SwaggerConfig. All controllers are
tagged with \@Tag(name=\...). Responses documented with \@ApiResponse
for 200, 201, 400, 404, 409, 500. Available at /swagger-ui/index.html.

**13. Design Patterns & Rationale**

  ---------------- -------------------------------- ----------------------------
  **Pattern**      **Where Applied**                **Benefit**

  Repository       JpaRepository interfaces per     Decouples data access from
  Pattern          aggregate root                   business logic

  Strategy Pattern AnalyticsService interface with  Allows future ML-based
                   swappable impls                  ranking strategies

  Specification    JpaSpecificationExecutor for     Composable, type-safe
  Pattern          employee filter                  dynamic queries

  Factory Method   ReviewMapper.fromRequest()       Consistent entity
                   static factory                   construction

  Template Method  BaseController.buildResponse()   Consistent API envelope
                                                    across all controllers

  Observer / Event Spring ApplicationEvents on      Decouples goal auto-miss
                   cycle close                      logic; extensible for
                                                    notifications

  Decorator        CachingAnalyticsService wraps    Cache concern separated from
                   AnalyticsServiceImpl             business logic

  DTO Pattern      MapStruct mappers between layers Prevents entity leakage into
                                                    API responses
  ---------------- -------------------------------- ----------------------------

**14. Scalability Mechanisms**

**14.1 Handling 500 Concurrent Managers**

The application is designed for horizontal stateless scaling:

-   No server-side HTTP session state (JWT or session token validated
    per request)

-   HikariCP pool sized to 20 connections per instance; 500 concurrent
    users served by 3--4 replicas behind a load balancer

-   Spring Cache (Caffeine) is node-local; cycle summaries cached
    locally after first computation

-   If distributed cache is needed (multi-node consistency), CacheConfig
    bean is swapped to RedissonSpringCacheManager with no service
    changes

**14.2 Handling 100k+ Reviews Per Cycle**

-   Summary queries run as native SQL aggregations (AVG, GROUP BY) --
    never load all reviews into JVM memory

-   idx_review_cycle_id + idx_review_rating composite indexes ensure
    summary queries run in O(log n) + O(index scan)

-   getTopPerformerByCycleId uses LIMIT 1 at DB level; no in-memory sort

-   Goal counts use COUNT(\*) with status filter, not full entity
    hydration

-   Pagination enforced on all list endpoints (max 100 per page) to
    bound response sizes

**14.3 Future Scalability Hooks**

-   Read-replica routing: \@Transactional(readOnly=true) methods can be
    routed to read replica by configuring AbstractRoutingDataSource

-   Async summary generation: \@Async on AnalyticsService methods for
    non-blocking summary builds

-   Queue-based review ingestion: SubmitReviewRequest can be published
    to a Kafka topic for eventual-consistency processing under extreme
    load

**15. Extension Points for Future Enhancements**

  ------------------------ ----------------------------------------------
  **Feature**              **Hook in Current Design**

  Notification system      CycleClosedEvent / ReviewSubmittedEvent
                           (Spring ApplicationEvents) -- add listeners
                           without changing existing code

  Role-based access        SecurityConfig stub + method-level
                           \@PreAuthorize annotations ready to wire to
                           OAuth2/OIDC JWT

  Weighted goal scoring    Goal.weight field present;
                           AnalyticsService.buildCycleSummary() has
                           placeholder for weighted completion calc

  Department-level         AnalyticsService.getDepartmentSummary() stub;
  analytics                employee.department index already in place

  Employee trend reports   AnalyticsService.getTrendForEmployee() stub;
                           review table has submittedAt for time-series
                           queries

  Multi-source reviews     PerformanceReview.reviewType enum (SELF, PEER,
  (360°)                   MANAGER) already stored; analytics can segment
                           by type

  Distributed cache        CacheManager bean swap in CacheConfig; all
  (Redis)                  \@Cacheable/@CacheEvict annotations remain
                           unchanged

  Read replica routing     \@Transactional(readOnly=true) already on all
                           reads; add AbstractRoutingDataSource to
                           CacheConfig

  Export / reporting       CycleSummaryResponse.ratingDistribution map
                           ready for chart rendering or CSV export
                           service

  Audit log / event        AuditEntity base class + AuditAware; extend
  sourcing                 EntityAuditListener to publish to audit store
  ------------------------ ----------------------------------------------

**16. Testing Strategy**

  --------------- --------------------- ----------------------------------
  **Layer**       **Tool**              **Coverage Target**

  Entity /        JUnit 5 + H2          All check constraints, unique
  Constraint      in-memory             constraints, audit fields

  Repository      Spring Data Test +    Custom JPQL/native queries,
                  \@DataJpaTest +       pagination
                  Testcontainers        
                  (Postgres)            

  Service         JUnit 5 + Mockito     Business rule validation, cache
                                        eviction, state transitions

  Controller      \@WebMvcTest +        HTTP semantics, request
                  MockMvc               validation, error envelope format

  Integration     Spring Boot Test +    End-to-end happy path + critical
                  Testcontainers        failure paths

  Performance     JMeter / Gatling      500-concurrent-user simulation on
                                        /cycles/{id}/summary
  --------------- --------------------- ----------------------------------

Document prepared for Talent Performance Pulse LLD -- Version 1.0
