# Phase 3 Test Execution Plan - End-to-End Testing

## Document Overview

This document outlines the complete end-to-end test strategy for Phase 3 of the Performance Pulse system. It validates all APIs, user workflows, and system integration points.

---

## Test Scope

### APIs Covered

**Phase 1 & 2 (Baseline)**
- ✓ `POST /employees` - Employee creation
- ✓ `GET /employees` - Employee retrieval with pagination
- ✓ `POST /cycles` - Review cycle creation
- ✓ `PATCH /cycles/{id}/status` - Cycle status activation
- ✓ `POST /goals` - Goal creation

**Phase 3 (New)**
- ✓ `POST /reviews` - Review submission (peer and self)
- ✓ `GET /employees/{id}/reviews` - Employee review retrieval
- ✓ `GET /employees?department={dept}&minRating={x}` - Advanced filtering with caching
- ✓ `GET /cycles/{id}/summary` - Cycle analytics and summary

### Test Scenarios

#### Scenario 1: Complete Employee Lifecycle
1. Create 4 test employees with different departments
2. Verify each employee is created with correct details
3. Retrieve employee list with pagination
4. Filter by department

#### Scenario 2: Review Cycle Management
1. Create 2 review cycles (Q1 and Q2)
2. Set first cycle to ACTIVE state
3. Keep second as UPCOMING
4. Verify cycle transitions

#### Scenario 3: Goal Management
1. Create performance goals for 3 employees
2. Link goals to employee and active cycle
3. Set realistic due dates
4. Verify goal storage and retrieval

#### Scenario 4: Review Workflow
1. **Peer Review**: Alice reviews Bob (Expert perspective)
2. **Peer Review**: Carol reviews Alice (Mentee perspective)  
3. **Self Review**: David self-assessment
4. Vary ratings to test analytics (2-5 scale)

#### Scenario 5: Review Retrieval
1. Fetch all reviews for Alice (should have 2 - one from Carol, one Bob)
2. Fetch all reviews for Bob (should have 1 - from Alice)
3. Verify review metadata (reviewer, cycle, rating)

#### Scenario 6: Employee Filtering
1. Filter ENGINEERING dept → expect Alice & Carol
2. Filter minRating ≥ 3.0 → expect employees with valid ratings
3. Combined filter: ENGINEERING AND rating ≥ 3.0 → subset
4. Verify caching works for repeated queries

#### Scenario 7: Cycle Analytics
1. Get cycle summary for active Q1 cycle
2. Verify statistics:
   - Total reviews count
   - Average rating calculation
   - Top performer identification
   - Goal completion metrics
3. Verify caching of analytics

---

## Test Data Setup

### Employee Data

| # | Name | Dept | Title | Role in Tests |
|---|------|------|-------|---------------|
| 1 | Alice Johnson | ENGINEERING | Senior SWE | Reviewer & Reviewee |
| 2 | Bob Smith | SALES | Sales Manager | Reviewee |
| 3 | Carol Williams | ENGINEERING | Junior Engineer | Reviewer & Reviewee |
| 4 | David Brown | HR | HR Manager | Self-Reviewer |

### Review Cycle Data

| Cycle | Start Date | End Date | Status | Purpose |
|-------|-----------|----------|--------|---------|
| Q1 2026 | Current - 5 days | Current + 25 days | ACTIVE | Main test cycle |
| Q2 2026 | Current + 60 days | Current + 120 days | UPCOMING | Secondary cycle |

### Review Data

| Reviewer | Reviewee | Type | Rating | Comment |
|----------|----------|------|--------|---------|
| Alice | Bob | Peer | 4 | Excellent collaboration |
| Carol | Alice | Peer | 5 | Outstanding technical lead |
| David | David | Self | 3 | Good, seeking improvement |

### Goal Data

| Employee | Title | Description | Due Date | Cycle |
|----------|-------|-------------|----------|-------|
| Alice | Q1 Goal | High-quality code & test coverage | +60d | Q1 2026 |
| Bob | Q1 Goal | Sales targets | +60d | Q1 2026 |
| Carol | Q1 Goal | Q1 Performance Goal | +60d | Q1 2026 |

---

## Test Execution Steps

### Step 1: API Health Check
```
Request:  GET /employees?page=0&size=1
Expected: 200 OK or 400 (validation error)
Validates: API is running and responding
```

### Step 2: Employee Creation
```
For each employee:
  Request:  POST /employees
  Payload:  { firstName, lastName, email, department, jobTitle, joiningDate }
  Expected: 201 Created
  Extract:  employee.id for later use
  Validate: All 4 employees created successfully
```

### Step 3: Cycle Management
```
Create cycles:
  Request:  POST /cycles
  Expected: 201 Created
  Extract:  cycle.id
  
Activate first cycle:
  Request:  PATCH /cycles/{id}/status
  Payload:  { status: "ACTIVE" }
  Expected: 200 OK
  Validate: Updated status visible in response
```

### Step 4: Goal Creation
```
For employees 1-3:
  Request:  POST /goals
  Payload:  { employeeId, cycleId, title, description, dueDate }
  Expected: 201 Created
  Extract:  goal.id
  Validate: All 3 goals created
```

### Step 5: Review Submission
```
Peer Review 1:
  Request:  POST /reviews
  Payload:  { employeeId: Bob.id, cycleId, reviewerId: Alice.id, 
              reviewType: "PEER_REVIEW", rating: 4, notes }
  Expected: 201 Created
  
Peer Review 2:
  Request:  POST /reviews
  Payload:  { employeeId: Alice.id, cycleId, reviewerId: Carol.id,
              reviewType: "PEER_REVIEW", rating: 5, notes }
  Expected: 201 Created
  
Self Review:
  Request:  POST /reviews
  Payload:  { employeeId: David.id, cycleId, 
              reviewType: "SELF_REVIEW", rating: 3, notes }
  Expected: 201 Created
  
Validate: All 3 reviews submitted successfully
```

### Step 6: Review Retrieval
```
For Alice:
  Request:  GET /employees/{alice.id}/reviews?page=0&size=20
  Expected: 200 OK
  Validate: Response includes Carol's review (rating 5)
  
For Bob:
  Request:  GET /employees/{bob.id}/reviews?page=0&size=20
  Expected: 200 OK
  Validate: Response includes Alice's review (rating 4)
```

### Step 7: Advanced Filtering
```
Filter 1 - By Department:
  Request:  GET /employees?department=ENGINEERING&page=0&size=20
  Expected: 200 OK
  Validate: Only Alice & Carol returned (2 employees)
  
Filter 2 - By MinRating:
  Request:  GET /employees?minRating=3.0&page=0&size=20
  Expected: 200 OK
  Validate: Only employees with avg rating ≥ 3.0 returned
  
Filter 3 - Combined (with caching):
  Request:  GET /employees?department=ENGINEERING&minRating=2.0
  Expected: 200 OK (from cache on 2nd request)
  Validate: Results filtered by both criteria
  Validate: Response time faster on subsequent request
```

### Step 8: Cycle Summary & Analytics
```
Request:  GET /cycles/{cycle.id}/summary
Expected: 200 OK

Response should include:
  {
    "cycleName": "Q1 2026 Reviews",
    "totalReviews": 3,
    "averageRating": 4.0,
    "topPerformer": {
      "name": "Alice Johnson",
      "averageRating": 5.0
    },
    "goalStats": {
      "total": 3,
      "completed": 0,
      "inProgress": 3,
      "missed": 0,
      "completionRate": 0.0
    }
  }

Validate:
  - totalReviews = 3 (Alice→Bob, Carol→Alice, David self)
  - averageRating = (4 + 5 + 3) / 3 = 4.0
  - topPerformer = Alice (only 5-star rating)
  - goalStats reflects 3 in-progress goals
```

---

## Expected Results

### Pass Criteria

✓ **All API endpoints respond** with correct HTTP status codes  
✓ **All status codes match specification**:
  - 201 Created for POST operations
  - 200 OK for GET and PATCH operations
  - 4xx for validation errors (if any)

✓ **Data integrity**:
  - Created entities persist in database
  - Relationships are properly maintained
  - Reviews accurately linked to employees and cycles

✓ **Advanced features**:
  - Filtering returns correct subsets
  - Caching improves response time
  - Analytics calculations are accurate
  - Pagination works correctly

✓ **Test summary shows 100% pass rate**:
  - Total Tests: Expected ~20+
  - Passed: All
  - Failed: 0
  - Success Rate: 100%

### Sample Pass Output

```
======================================================================
                   TEST EXECUTION SUMMARY
======================================================================

Total Tests:  23
Passed:       ✓ 23
Failed:       ✗ 0
Success Rate: 100.0%

✓ All tests passed!

======================================================================
```

---

## Performance Expectations

| Operation | Expected Time | Allowed Time |
|-----------|---------------|--------------|
| Employee creation | 50-100ms | < 500ms |
| Cycle creation | 50-100ms | < 500ms |
| Goal creation | 50-100ms | < 500ms |
| Review submission | 60-120ms | < 500ms |
| Review retrieval | 50-100ms | < 500ms |
| Basic filtering | 80-150ms | < 500ms |
| Cached filtering | 10-50ms | < 200ms |
| Cycle summary | 100-200ms | < 1000ms |
| **Full test suite** | 2-5 seconds | < 30 seconds |

---

## Error Handling Tests

### Negative Test Cases (Optional Extensions)

1. **Duplicate Employee Email**
   - Create employee with email = existing email
   - Expected: 400 Bad Request or 409 Conflict

2. **Non-existent Employee Review**
   - Try to get reviews for non-existent employee ID
   - Expected: 404 Not Found

3. **Invalid Cycle Status**
   - Try to set cycle to invalid status
   - Expected: 400 Bad Request

4. **Missing Required Fields**
   - Create employee without email
   - Expected: 400 Bad Request with validation error

5. **Invalid Rating**
   - Submit review with rating = 10 (out of bounds)
   - Expected: 400 Bad Request

---

## Database Validation

After tests complete, verify database state:

```sql
-- Check employees created
SELECT COUNT(*) FROM employees;  -- Should be ≥ 4

-- Check reviews submitted
SELECT COUNT(*) FROM reviews;  -- Should be 3

-- Check goals created
SELECT COUNT(*) FROM goals;  -- Should be 3

-- Check cycle status
SELECT name, status FROM review_cycles ORDER BY created_at DESC LIMIT 1;
-- Should be: Q1 2026 Reviews | ACTIVE

-- Verify relationships
SELECT r.id, e.first_name, e.last_name, r.rating 
FROM reviews r
JOIN employees e ON r.employee_id = e.id
ORDER BY r.created_at;
```

---

## Success Criteria Summary

✓ **Functional**: All 10 Phase 3 API endpoints working  
✓ **Data**: Correct creation, retrieval, and persistence  
✓ **Performance**: Responses within acceptable timeframes  
✓ **Integration**: Entities correctly related  
✓ **Analytics**: Calculations accurate and cached  
✓ **Automation**: E2E script runs without manual intervention  
✓ **Documentation**: Clear instructions and expected results  

---

## Test Maintenance

### Before Each Test Run

1. Ensure application is running: `docker-compose ps`
2. Verify database initialized: Check PostgreSQL tables exist
3. Clear previous test data (optional): Truncate test tables
4. Review logs for any startup errors: `docker-compose logs app`

### After Failed Tests

1. Check API response in error message
2. Review application logs: `docker-compose logs app`
3. Verify database state with SQL queries
4. Check network connectivity: `curl http://localhost:8080/actuator/health`

### Continuous Integration

This script is designed for CI/CD pipelines:
- Returns exit code 0 on success
- Returns exit code 1 on failure
- Provides detailed logging for failure analysis
- Automatically generates unique test data (timestamps)

---

## Conclusion

This comprehensive E2E test plan ensures:
1. All Phase 3 features are working correctly
2. System integrates properly across all layers
3. Performance meets requirements
4. Data integrity is maintained
5. Business logic is correct (analytics, filtering, caching)

Regular execution of this test suite provides confidence in system quality and facilitates early detection of regressions.
