# MLP-1 Phase 3: Goals & Analytics

**Status**: Phase 4 of 4 (Final)  
**Prerequisite**: Phase 0-2 (All previous phases)  
**Objective**: Implement goal tracking and cycle summary analytics with caching  
**Estimated Duration**: 45 mins  

---

## Deliverables

- [ ] `Goal` entity with all fields
- [ ] `GoalRepository` with custom query methods
- [ ] `GoalService` interface and implementation (basic CRUD for summary support)
- [ ] DTOs: `CreateGoalRequest`, `GoalResponse`, `GoalStatsDTO`
- [ ] `GoalMapper` (MapStruct)
- [ ] `GoalController` with endpoint:
  - `POST /api/v1/goals` — create goal (supports cycle summary calculations)
- [ ] `AnalyticsService` interface and implementation
- [ ] DTOs: `CycleSummaryResponse`, `EmployeeSummaryDTO`
- [ ] Complete `ReviewCycleController` endpoint:
  - `GET /api/v1/cycles/{id}/summary` — cycle summary with caching
- [ ] Cache configuration and eviction strategy

---

## Goal Entity & Layer

### Entity:
```java
@Entity
@Table(
    name = "goals",
    indexes = {
        @Index(columnList = "employee_id, cycle_id", name = "idx_goal_employee_cycle"),
        @Index(columnList = "cycle_id, status", name = "idx_goal_status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Goal extends AuditEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private ReviewCycle cycle;
    
    @Column(length = 255, nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.PENDING;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Column
    private LocalDateTime completedAt;
    
    @Column(nullable = false)
    private Integer weight = 1;  // Enhancement hook: for weighted scoring
}
```

### Repository:
```java
@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {
    
    List<Goal> findByEmployeeIdAndCycleId(UUID employeeId, UUID cycleId);
    
    List<Goal> findByCycleIdAndStatusIn(UUID cycleId, List<GoalStatus> statuses);
    
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN status = 'MISSED' THEN 1 ELSE 0 END) as missed,
            SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as in_progress
        FROM goals
        WHERE cycle_id = :cycleId
    """)
    GoalStatsDTO getGoalStatsByCycleId(@Param("cycleId") UUID cycleId);
}
```

### Service Methods:
- `createGoal(CreateGoalRequest)` — 
  1. Validate employee exists and is ACTIVE
  2. Validate cycle exists (should not be CLOSED)
  3. Validate dueDate is within cycle.startDate .. cycle.endDate
  4. Persist with status = PENDING, weight = 1
  5. Return response

- `getGoalsForCycle(UUID cycleId)` — fetch all goals for cycle

---

## Goal DTOs

### CreateGoalRequest:
```java
@Data
public class CreateGoalRequest {
    @NotNull
    private UUID employeeId;
    
    @NotNull
    private UUID cycleId;
    
    @NotBlank @Size(max = 255)
    private String title;
    
    @Size(max = 4000)
    private String description;
    
    @NotNull
    private LocalDate dueDate;
    
    // Validation: dueDate must be >= cycle.startDate AND <= cycle.endDate
}
```

### GoalResponse:
```java
@Data
public class GoalResponse {
    private UUID id;
    private UUID employeeId;
    private UUID cycleId;
    private String title;
    private String description;
    private GoalStatus status;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private Integer weight;
}
```

### GoalStatsDTO (Projection):
```java
@Data
public class GoalStatsDTO {
    private long total;
    private long completed;
    private long missed;
    private long inProgress;
    
    public double getCompletionRate() {
        if (total == 0) return 0.0;
        return (double) completed / total * 100;
    }
}
```

---

## AnalyticsService & CycleSummary

### AnalyticsService Interface:
```java
public interface AnalyticsService {
    CycleSummaryResponse buildCycleSummary(UUID cycleId);
}
```

### AnalyticsServiceImpl Implementation:
```java
@Service
@Transactional(readOnly = true)
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {
    
    private final PerformanceReviewRepository reviewRepository;
    private final GoalRepository goalRepository;
    private final ReviewCycleRepository cycleRepository;
    
    @Cacheable(value = "cycle-summary", key = "#cycleId")
    public CycleSummaryResponse buildCycleSummary(UUID cycleId) {
        // 1. Fetch cycle
        ReviewCycle cycle = cycleRepository.findById(cycleId)
            .orElseThrow(() -> new ResourceNotFoundException("Cycle not found"));
        
        // 2. Calculate average rating (only finalized reviews)
        Double avgRating = reviewRepository.getAverageRatingByCycleId(cycleId);
        
        // 3. Get top performer
        TopPerformerDTO topPerformer = reviewRepository.getTopPerformerByCycleId(cycleId);
        
        // 4. Get goal stats
        GoalStatsDTO goalStats = goalRepository.getGoalStatsByCycleId(cycleId);
        
        // 5. Build response
        return CycleSummaryResponse.builder()
            .cycleId(cycleId)
            .cycleName(cycle.getName())
            .startDate(cycle.getStartDate())
            .endDate(cycle.getEndDate())
            .totalReviews(reviewRepository.countByCycleId(cycleId))
            .averageRating(avgRating != null ? Math.round(avgRating * 100.0) / 100.0 : 0.0)
            .topPerformer(topPerformer != null ? 
                EmployeeSummaryDTO.builder()
                    .id(topPerformer.getId())
                    .name(topPerformer.getFullName())
                    .averageRating(Math.round(topPerformer.getAverageRating() * 100.0) / 100.0)
                    .build() 
                : null)
            .goalStats(goalStats)
            .generatedAt(LocalDateTime.now())
            .build();
    }
}
```

### CycleSummaryResponse DTO:
```java
@Data
@Builder
public class CycleSummaryResponse {
    private UUID cycleId;
    private String cycleName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalReviews;
    private Double averageRating;  // Rounded to 2 decimal places
    private EmployeeSummaryDTO topPerformer;  // Highest average rating
    private GoalStatsDTO goalStats;  // Total, completed, missed, inProgress, completionRate
    private LocalDateTime generatedAt;
}
```

### EmployeeSummaryDTO:
```java
@Data
@Builder
public class EmployeeSummaryDTO {
    private UUID id;
    private String name;  // firstName + lastName
    private Double averageRating;  // Rounded to 2 decimal places
}
```

---

## Cache Configuration & Eviction

### CacheConfig (in Phase 0 or update):
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        Map<String, Caffeine<Object, Object>> specs = Map.of(
            "cycle-summary", 
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(500),
            "employee-list",
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(100),
            "top-performers",
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(200)
        );
        
        specs.forEach((name, caffeine) -> 
            cacheManager.registerCustomCache(name, caffeine.build()));
        
        return cacheManager;
    }
}
```

### Cache Eviction Decorators:

Add to **PerformanceReviewServiceImpl.submitReview()**:
```java
@Transactional
@CacheEvict(value = {"cycle-summary", "top-performers"}, allEntries = false, key = "#result.cycleId")
public ReviewResponse submitReview(SubmitReviewRequest request) {
    // ... existing logic
}
```

Add to **GoalServiceImpl.createGoal()** and **updateGoalStatus()**:
```java
@Transactional
@CacheEvict(value = "cycle-summary", allEntries = false, key = "#result.cycleId")
public GoalResponse createGoal(CreateGoalRequest request) {
    // ... existing logic
}
```

Add to **EmployeeServiceImpl.listEmployees()**:
```java
@Transactional(readOnly = true)
@Cacheable(value = "employee-list", key = "T(java.util.Objects).hash(#department, #minRating)")
public Page<EmployeeResponse> listEmployees(Department department, Double minRating, Pageable pageable) {
    // ... existing logic
}
```

---

## ReviewCycleController Completion

### GET /api/v1/cycles/{id}/summary
```java
@GetMapping("/{id}/summary")
public ResponseEntity<CycleSummaryResponse> getCycleSummary(@PathVariable UUID id) {
    CycleSummaryResponse summary = analyticsService.buildCycleSummary(id);
    return ResponseEntity.ok(summary);
}
```

- **Response**: 200 OK + CycleSummaryResponse
- **Caching**: Result cached for 10 minutes (via @Cacheable in AnalyticsService)
- **Cache Invalidation**: Evicted when:
  - New review submitted for the cycle
  - New goal created/updated for the cycle
  - Cycle closes

---

## GoalController Endpoint

### POST /api/v1/goals
- **Request**: CreateGoalRequest
- **Response**: 201 Created + GoalResponse
- **Validation**:
  - Employee must exist and be ACTIVE (422 if not)
  - Cycle must exist and not be CLOSED (422 if not)
  - dueDate must be >= cycle.startDate AND <= cycle.endDate (400 if invalid)
- **Side Effects**: Evict cycle-summary cache
- **Default Values**: 
  - status = PENDING
  - weight = 1

---

## Business Rules

| Rule | Logic |
|------|-------|
| **Date Range** | dueDate must be within [cycle.startDate, cycle.endDate] (400 if invalid) |
| **Cycle Not Closed** | Cannot create goals for CLOSED cycles (422) |
| **Employee Active** | Employee must be ACTIVE status |
| **Completion Rate** | (completed / total) × 100; returns 0 if total = 0 |
| **Average Rating** | Mean of finalized reviews only; rounded to 2 decimal places |
| **Top Performer** | Employee with highest average rating in cycle; NULL if no reviews |

---

## Files to Create/Modify

| File | Type | Purpose |
|------|------|---------|
| `src/main/java/com/hr/performancepulse/entity/Goal.java` | Create | Goal entity |
| `src/main/java/com/hr/performancepulse/repository/GoalRepository.java` | Create | Goal repository |
| `src/main/java/com/hr/performancepulse/service/GoalService.java` | Create | Goal service interface |
| `src/main/java/com/hr/performancepulse/service/impl/GoalServiceImpl.java` | Create | Goal service implementation |
| `src/main/java/com/hr/performancepulse/service/AnalyticsService.java` | Create | Analytics service interface |
| `src/main/java/com/hr/performancepulse/service/impl/AnalyticsServiceImpl.java` | Create | Analytics service implementation |
| `src/main/java/com/hr/performancepulse/dto/request/CreateGoalRequest.java` | Create | Create goal request |
| `src/main/java/com/hr/performancepulse/dto/response/GoalResponse.java` | Create | Goal response |
| `src/main/java/com/hr/performancepulse/dto/GoalStatsDTO.java` | Create | Goal stats projection |
| `src/main/java/com/hr/performancepulse/dto/response/CycleSummaryResponse.java` | Create | Cycle summary response |
| `src/main/java/com/hr/performancepulse/dto/EmployeeSummaryDTO.java` | Create | Employee summary DTO |
| `src/main/java/com/hr/performancepulse/mapper/GoalMapper.java` | Create | MapStruct mapper |
| `src/main/java/com/hr/performancepulse/controller/GoalController.java` | Create | Goal controller |
| `src/main/java/com/hr/performancepulse/controller/ReviewCycleController.java` | Modify | Add GET /{id}/summary endpoint |
| `src/main/java/com/hr/performancepulse/service/impl/PerformanceReviewServiceImpl.java` | Modify | Add @CacheEvict to submitReview() |
| `src/main/java/com/hr/performancepulse/service/impl/EmployeeServiceImpl.java` | Modify | Add @Cacheable to listEmployees() |
| `src/main/java/com/hr/performancepulse/config/CacheConfig.java` | Create/Modify | Define cache manager and specs |

---

## Success Criteria

- [ ] POST /goals creates goal and validates date range
- [ ] POST /goals returns 422 if cycle is CLOSED
- [ ] POST /goals returns 400 if dueDate outside cycle range
- [ ] GET /cycles/{id}/summary returns valid CycleSummaryResponse with all metrics
- [ ] Summary includes:
  - Correct average rating (rounded to 2 decimals)
  - Top performer with correct rating
  - Goal stats: total, completed, missed, inProgress, completionRate
- [ ] Summary response is cached for 10 minutes
- [ ] Cache is evicted on review submission or goal creation
- [ ] Ratings are rounded to 2 decimal places in all responses
- [ ] Employee filtering by department and minRating works correctly (cached)

---

## Final Validation (All 4 Phases)

### All Required Endpoints Implemented:
1. ✅ POST /api/v1/employees — create employee
2. ✅ GET /api/v1/employees?department={d}&minRating={r} — filter employees (cached)
3. ✅ POST /api/v1/cycles — create cycle
4. ✅ POST /api/v1/reviews — submit review (cache eviction)
5. ✅ GET /api/v1/employees/{id}/reviews — employee review history
6. ✅ GET /api/v1/cycles/{id}/summary — cycle summary (cached)

### Additional Features:
- All DTOs with proper validation
- All entities with audit fields and optimistic locking
- MapStruct mappers for all entity ↔ DTO conversions
- Centralized exception handling with HTTP status codes
- Caching strategy with TTLs and eviction triggers
- Business rule validation at service and DB levels
- Paginated results for list endpoints

---

## Integration Testing Flow

After all 4 phases, test the complete workflow:

1. Create Employee → returns UUID
2. Create ReviewCycle (UPCOMING status) → returns UUID
3. Get Employees with filters → returns paginated list (cached)
4. Create Goal (for employee in cycle) → returns UUID
5. Create another Employee (for reviewer)
6. POST /reviews (submit review) → cache evicted
7. GET /employees/{id}/reviews → returns all reviews
8. GET /cycles/{id}/summary → returns cached summary with:
   - Average rating
   - Top performer
   - Goal completion rate (1 completed / 1 total = 100%)

---

## Notes

- **Phase 0** must be completed first for all infrastructure
- **Phases 1-3** can begin after Phase 0
- **Phases 1 and 2** are independent once Phase 0 is done (both need Employee & Cycle)
- **Phase 3** depends on both Phase 1 and Phase 2 being complete
- Tests can be added after all endpoints are functional (not in MLP-1 scope)

---

## References

- LLD Section 3.5: Goal entity design
- LLD Section 5.3: GoalRepository methods
- LLD Section 6.4: AnalyticsService behavior
- LLD Section 7: Caching design
- LLD Section 8.3: ReviewCycleController summary endpoint
- LLD Section 9.3: CycleSummaryResponse DTO
