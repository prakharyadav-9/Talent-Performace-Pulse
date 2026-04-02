# MLP-1 Phase 1: Employee & Cycle Management

**Status**: Phase 2 of 4  
**Prerequisite**: Phase 0 (Foundation & Infrastructure)  
**Objective**: Build employee and cycle entities with CRUD operations and filtering  
**Estimated Duration**: 40 mins  

---

## Deliverables

- [ ] `Employee` entity with all fields
- [ ] `EmployeeRepository` with custom query methods
- [ ] `EmployeeService` interface and implementation
- [ ] DTOs: `CreateEmployeeRequest`, `EmployeeResponse`
- [ ] `EmployeeMapper` (MapStruct)
- [ ] `EmployeeController` with endpoints:
  - `POST /api/v1/employees` — create employee
  - `GET /api/v1/employees?department={d}&minRating={r}` — list with filtering
- [ ] `ReviewCycle` entity with all fields
- [ ] `ReviewCycleRepository` with custom query methods
- [ ] `ReviewCycleService` interface and implementation
- [ ] DTOs: `CreateCycleRequest`, `CycleResponse`
- [ ] `CycleMapper` (MapStruct)
- [ ] `ReviewCycleController` with endpoint:
  - `POST /api/v1/cycles` — create cycle

---

## Employee Entity & Layer

### Entity:
```java
@Entity
@Table(name = "employees", indexes = @Index(columnList = "department, status", name = "idx_employee_dept_status"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends AuditEntity {
    @Column(length = 100, nullable = false)
    private String firstName;
    
    @Column(length = 100, nullable = false)
    private String lastName;
    
    @Column(length = 255, unique = true, nullable = false)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;
    
    @Column(length = 150, nullable = false)
    private String jobTitle;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;
    
    @Column(nullable = false)
    private LocalDate joiningDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerformanceReview> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Goal> goals = new ArrayList<>();
}
```

### Repository:
```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    boolean existsByEmail(String email);
    Optional<Employee> findByIdWithReviews(UUID id);  // JOIN FETCH with reviews
    List<Employee> findByManagerId(UUID managerId);
}
```

### Service Methods:
- `createEmployee(CreateEmployeeRequest)` — validate email uniqueness, persist, return response
- `listEmployees(Department, Double minRating, Pageable)` — filter and paginate with @Cacheable
- `getEmployee(UUID)` — fetch with reviews eager-loaded, throw 404 if not found

### DTO: CreateEmployeeRequest:
```java
@Data
public class CreateEmployeeRequest {
    @NotBlank @Size(max = 100)
    private String firstName;
    
    @NotBlank @Size(max = 100)
    private String lastName;
    
    @NotBlank @Email @Size(max = 255)
    private String email;
    
    @NotNull
    private Department department;
    
    @NotBlank @Size(max = 150)
    private String jobTitle;
    
    private UUID managerId;  // Optional
    
    @NotNull @PastOrPresent
    private LocalDate joiningDate;
}
```

### DTO: EmployeeResponse:
```java
@Data
public class EmployeeResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private Department department;
    private String jobTitle;
    private UUID managerId;
    private LocalDate joiningDate;
    private EmployeeStatus status;
    private Double averageRating;  // Computed from reviews
}
```

---

## ReviewCycle Entity & Layer

### Entity:
```java
@Entity
@Table(
    name = "review_cycles",
    uniqueConstraints = @UniqueConstraint(columnNames = "name")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCycle extends AuditEntity {
    @Column(length = 100, unique = true, nullable = false)
    private String name;
    
    @Column(nullable = false)
    private LocalDate startDate;
    
    @Column(nullable = false)
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CycleStatus status = CycleStatus.UPCOMING;
    
    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerformanceReview> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Goal> goals = new ArrayList<>();
}
```

### Repository:
```java
@Repository
public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, UUID> {
    Optional<ReviewCycle> findByName(String name);
    Optional<ReviewCycle> findByStatus(CycleStatus status);
}
```

### Service Methods:
- `createCycle(CreateCycleRequest)` — validate date range (endDate > startDate) and name uniqueness, persist, return response
- `getCycle(UUID)` — fetch by ID, throw 404 if not found
- `isActiveCycleExists()` — check if any cycle is ACTIVE (used by review service)

### DTO: CreateCycleRequest:
```java
@Data
public class CreateCycleRequest {
    @NotBlank @Size(max = 100)
    private String name;
    
    @NotNull
    private LocalDate startDate;
    
    @NotNull
    private LocalDate endDate;
    
    // Validation: endDate must be > startDate
}
```

### DTO: CycleResponse:
```java
@Data
public class CycleResponse {
    private UUID id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private CycleStatus status;
    private LocalDateTime createdAt;
}
```

---

## Controller Endpoints

### POST /api/v1/employees
- **Request**: CreateEmployeeRequest
- **Response**: 201 Created + EmployeeResponse
- **Validation**: Email must be unique (409 if duplicate)

### GET /api/v1/employees?department={d}&minRating={r}&page={p}&size={s}
- **Response**: 200 OK + Page<EmployeeResponse>
- **Filters**: 
  - `department` (optional): Department enum value
  - `minRating` (optional): Minimum average rating from reviews
- **Caching**: @Cacheable("employee-list") with TTL 5 mins
- **Note**: Uses JPA Specification for dynamic filtering or native SQL with GROUP BY for minRating

### POST /api/v1/cycles
- **Request**: CreateCycleRequest
- **Response**: 201 Created + CycleResponse
- **Validation**: 
  - Name must be unique (409 if duplicate)
  - endDate > startDate (400 if invalid)

---

## Business Rules

| Rule | Details |
|------|---------|
| Email Uniqueness | Duplicate emails return 409 Conflict |
| Department | Must be valid enum (ENGINEERING, HR, SALES, MARKETING, FINANCE, OPERATIONS) |
| Joining Date | Must be today or in the past (@PastOrPresent) |
| Cycle Date Range | endDate must be > startDate; enforced at DB level with CHECK constraint |
| Cycle Name Uniqueness | Return 409 if duplicate |
| Average Rating Calculation | Mean of all PerformanceReview.rating values for an employee across all cycles |

---

## Files to Create

| File | Type | Purpose |
|------|------|---------|
| `src/main/java/com/hr/performancepulse/entity/Employee.java` | Create | Employee entity |
| `src/main/java/com/hr/performancepulse/entity/ReviewCycle.java` | Create | ReviewCycle entity |
| `src/main/java/com/hr/performancepulse/repository/EmployeeRepository.java` | Create | Employee repository |
| `src/main/java/com/hr/performancepulse/repository/ReviewCycleRepository.java` | Create | ReviewCycle repository |
| `src/main/java/com/hr/performancepulse/service/EmployeeService.java` | Create | Employee service interface |
| `src/main/java/com/hr/performancepulse/service/impl/EmployeeServiceImpl.java` | Create | Employee service implementation |
| `src/main/java/com/hr/performancepulse/service/ReviewCycleService.java` | Create | ReviewCycle service interface |
| `src/main/java/com/hr/performancepulse/service/impl/ReviewCycleServiceImpl.java` | Create | ReviewCycle service implementation |
| `src/main/java/com/hr/performancepulse/dto/request/CreateEmployeeRequest.java` | Create | Create employee request |
| `src/main/java/com/hr/performancepulse/dto/request/CreateCycleRequest.java` | Create | Create cycle request |
| `src/main/java/com/hr/performancepulse/dto/response/EmployeeResponse.java` | Create | Employee response |
| `src/main/java/com/hr/performancepulse/dto/response/CycleResponse.java` | Create | Cycle response |
| `src/main/java/com/hr/performancepulse/mapper/EmployeeMapper.java` | Create | MapStruct mapper for Employee |
| `src/main/java/com/hr/performancepulse/mapper/CycleMapper.java` | Create | MapStruct mapper for ReviewCycle |
| `src/main/java/com/hr/performancepulse/controller/EmployeeController.java` | Create | Employee controller (2 endpoints) |
| `src/main/java/com/hr/performancepulse/controller/ReviewCycleController.java` | Create | ReviewCycle controller (1 endpoint) |

---

## Success Criteria

- [ ] POST /employees creates employee and returns 201
- [ ] POST /employees returns 409 for duplicate email
- [ ] GET /employees filters by department and minRating (cached)
- [ ] POST /cycles creates cycle and returns 201
- [ ] POST /cycles returns 409 for duplicate name
- [ ] POST /cycles returns 400 if endDate <= startDate
- [ ] Mapper correctly computes averageRating from reviews

---

## Next Phase

Proceed to **Phase 2: Review Management** to implement review submission and employee review history retrieval.

---

## References

- LLD Section 3.2: Employee entity design
- LLD Section 3.3: ReviewCycle entity design
- LLD Section 5.1: EmployeeRepository interface
- LLD Section 6.1: EmployeeService behavior
