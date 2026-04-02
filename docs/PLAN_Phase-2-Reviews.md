# MLP-1 Phase 2: Review Management

**Status**: Phase 3 of 4  
**Prerequisite**: Phase 0 (Foundation) and Phase 1 (Employee & Cycle Management)  
**Objective**: Implement performance review submission and employee review history retrieval  
**Estimated Duration**: 35 mins  

---

## Deliverables

- [ ] `PerformanceReview` entity with all fields
- [ ] `PerformanceReviewRepository` with custom query methods
- [ ] `PerformanceReviewService` interface and implementation
- [ ] DTOs: `SubmitReviewRequest`, `ReviewResponse`
- [ ] `ReviewMapper` (MapStruct)
- [ ] `PerformanceReviewController` with endpoints:
  - `POST /api/v1/reviews` — submit a review
  - `GET /api/v1/employees/{id}/reviews` — get all reviews for an employee

---

## PerformanceReview Entity & Layer

### Entity:
```java
@Entity
@Table(
    name = "performance_reviews",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"employee_id", "cycle_id", "reviewer_id"},
        name = "uk_review_employee_cycle_reviewer"
    ),
    indexes = {
        @Index(columnList = "cycle_id", name = "idx_review_cycle_id"),
        @Index(columnList = "employee_id", name = "idx_review_employee_id"),
        @Index(columnList = "cycle_id, rating DESC", name = "idx_review_rating"),
        @Index(columnList = "is_finalized, cycle_id", name = "idx_review_finalized")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReview extends AuditEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private ReviewCycle cycle;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")  // Nullable for self-review
    private Employee reviewer;
    
    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private Integer rating;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewType reviewType;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;
    
    @Column(nullable = false)
    private Boolean isFinalized = false;
}
```

### Repository:
```java
@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {
    
    List<PerformanceReview> findByCycleIdWithEmployees(UUID cycleId);  // JOIN FETCH employee
    
    Optional<PerformanceReview> findByEmployeeIdAndCycleIdAndReviewerId(
        UUID employeeId, UUID cycleId, UUID reviewerId);
    
    List<PerformanceReview> findByEmployeeIdOrderBySubmittedAtDesc(UUID employeeId, Pageable pageable);
    
    @Query("SELECT AVG(r.rating) FROM PerformanceReview r WHERE r.cycle.id = :cycleId AND r.isFinalized = true")
    Double getAverageRatingByCycleId(@Param("cycleId") UUID cycleId);
    
    @Query(nativeQuery = true, value = """
        SELECT e.id, e.first_name, e.last_name, AVG(pr.rating) as avg_rating
        FROM performance_reviews pr
        JOIN employees e ON pr.employee_id = e.id
        WHERE pr.cycle_id = :cycleId AND pr.is_finalized = true
        GROUP BY e.id, e.first_name, e.last_name
        ORDER BY avg_rating DESC
        LIMIT 1
    """)
    TopPerformerDTO getTopPerformerByCycleId(@Param("cycleId") UUID cycleId);
    
    long countByCycleId(UUID cycleId);
}
```

### Service Methods:
- `submitReview(SubmitReviewRequest)` — 
  1. Validate cycle is ACTIVE
  2. Validate employee exists and is ACTIVE
  3. Validate reviewer (if set) exists and is ACTIVE
  4. Check for duplicate (employee + cycle + reviewer); throw 409 if exists
  5. Validate rating 1-5
  6. Set submittedAt to now
  7. Persist and evict caches
  8. Return response

- `getReviewsByEmployee(UUID empId, Pageable)` — 
  - Fetch paginated reviews ordered by submittedAt DESC
  - Include cycle details (cycle name, start/end dates)
  - Throw 404 if employee not found

---

## DTOs

### SubmitReviewRequest:
```java
@Data
public class SubmitReviewRequest {
    @NotNull
    private UUID employeeId;
    
    @NotNull
    private UUID cycleId;
    
    private UUID reviewerId;  // Optional (null = self-review)
    
    @NotNull
    private ReviewType reviewType;
    
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;
    
    @Size(max = 4000)
    private String notes;
}
```

### ReviewResponse:
```java
@Data
public class ReviewResponse {
    private UUID id;
    private UUID employeeId;
    private String employeeName;  // firstName + lastName
    private UUID cycleId;
    private String cycleName;
    private LocalDate cycleStartDate;
    private LocalDate cycleEndDate;
    private UUID reviewerId;
    private String reviewerName;
    private ReviewType reviewType;
    private Integer rating;
    private String notes;
    private LocalDateTime submittedAt;
    private Boolean isFinalized;
}
```

### TopPerformerDTO (Projection):
```java
@Data
public class TopPerformerDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private Double averageRating;
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

---

## Controller Endpoints

### POST /api/v1/reviews
- **Request**: SubmitReviewRequest
- **Response**: 201 Created + ReviewResponse
- **Validation**:
  - Cycle must be ACTIVE (422 InvalidCycleStateException if not)
  - Employee must be ACTIVE (422 if not)
  - Reviewer must be ACTIVE if provided (422 if not)
  - No duplicate (employee + cycle + reviewer); 409 DuplicateReviewException if exists
  - Rating must be 1-5 (400 InvalidRatingException if not)
- **Side Effects**: 
  - Set submittedAt = now (in service, not client)
  - Evict cycle-summary cache
  - Evict top-performers cache

### GET /api/v1/employees/{id}/reviews?page={p}&size={s}
- **Request**: Path param (employee ID) + optional pagination
- **Response**: 200 OK + Page<ReviewResponse>
- **Validation**: Employee must exist (404 if not)
- **Content**: All reviews for employee across all cycles, ordered by submittedAt DESC
- **Note**: Include full cycle details (name, start/end dates) in response

---

## Business Validation Rules

| Rule | Logic |
|------|-------|
| **Cycle Active** | Review can only be submitted for ACTIVE cycle (422) |
| **Employee Active** | Employee being reviewed must be ACTIVE status |
| **Reviewer Active** | If reviewer specified, must exist and be ACTIVE; nullable for self-review |
| **Uniqueness** | Only one review per (employee, cycle, reviewer) triplet (409 if duplicate) |
| **Rating Range** | Must be 1-5 inclusive; enforced at DTO, service, and DB CHECK constraint |
| **Submitted Timestamp** | Set in service layer (not client-provided) |
| **Finalization Guard** | Once isFinalized=true, review cannot be edited; used for analytics integrity |

---

## Exception Mapping

| Exception | HTTP | Error Code |
|-----------|------|-----------|
| InvalidCycleStateException | 422 | INVALID_CYCLE_STATE |
| ResourceNotFoundException | 404 | RESOURCE_NOT_FOUND |
| DuplicateReviewException | 409 | DUPLICATE_REVIEW |
| InvalidRatingException | 400 | INVALID_RATING |
| MethodArgumentNotValidException | 400 | VALIDATION_FAILED |

---

## Repository Queries Detail

**findByCycleIdWithEmployees(UUID cycleId)**
- JPQL: `JOIN FETCH review.employee` to eagerly load employee
- Purpose: Batch load reviews with their employees to avoid N+1 when building cycle summary

**getAverageRatingByCycleId(UUID cycleId)**
- Only includes finalized reviews for analytics integrity
- Returns Double for use in cycle summary calculation

**getTopPerformerByCycleId(UUID cycleId)**
- Native SQL with GROUP BY and aggregate
- Only includes finalized reviews
- Returns single highest-rated employee or NULL if no reviews

---

## Files to Create

| File | Type | Purpose |
|------|------|---------|
| `src/main/java/com/hr/performancepulse/entity/PerformanceReview.java` | Create | PerformanceReview entity |
| `src/main/java/com/hr/performancepulse/repository/PerformanceReviewRepository.java` | Create | Review repository |
| `src/main/java/com/hr/performancepulse/service/PerformanceReviewService.java` | Create | Review service interface |
| `src/main/java/com/hr/performancepulse/service/impl/PerformanceReviewServiceImpl.java` | Create | Review service implementation |
| `src/main/java/com/hr/performancepulse/dto/request/SubmitReviewRequest.java` | Create | Submit review request |
| `src/main/java/com/hr/performancepulse/dto/response/ReviewResponse.java` | Create | Review response |
| `src/main/java/com/hr/performancepulse/dto/TopPerformerDTO.java` | Create | Top performer projection |
| `src/main/java/com/hr/performancepulse/mapper/ReviewMapper.java` | Create | MapStruct mapper |
| `src/main/java/com/hr/performancepulse/controller/PerformanceReviewController.java` | Create | Review controller |

---

## Success Criteria

- [ ] POST /reviews submits review and returns 201
- [ ] POST /reviews returns 422 if cycle not ACTIVE
- [ ] POST /reviews returns 409 if duplicate (same employee, cycle, reviewer)
- [ ] POST /reviews returns 400 if rating not 1-5
- [ ] GET /employees/{id}/reviews returns paginated reviews for employee
- [ ] Reviews include full cycle details (name, dates)
- [ ] submittedAt is set in service (not from client)
- [ ] Cache is evicted on review submission
- [ ] Exception handler returns appropriate HTTP status codes

---

## Next Phase

Proceed to **Phase 3: Goals & Analytics** to implement goal tracking and cycle summaries.

---

## References

- LLD Section 3.4: PerformanceReview entity design
- LLD Section 5.2: PerformanceReviewRepository interface
- LLD Section 6.2: PerformanceReviewService behavior
- LLD Section 11: Business validation rules (review submission)
