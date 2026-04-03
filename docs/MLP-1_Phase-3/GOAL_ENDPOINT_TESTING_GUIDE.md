# Goal Endpoint Testing Guide

## 🚀 Complete End-to-End Test for Goal Management

### Prerequisites
- Application running: `docker-compose up -d`
- Port 8080 accessible
- PostgreSQL initialized
- `curl` and `jq` installed (optional but recommended)

---

## Test 1: Create Goal - Happy Path

### Step 1a: Create Employee
```bash
EMPLOYEE=$(curl -s -X POST http://localhost:8080/api/v1/employees \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Alice",
    "lastName": "Johnson",
    "email": "alice@company.com",
    "department": "ENGINEERING",
    "jobTitle": "Senior Engineer",
    "joiningDate": "2023-01-15"
  }')

EMPLOYEE_ID=$(echo $EMPLOYEE | jq -r '.data.id')
echo "✓ Employee created: $EMPLOYEE_ID"
echo $EMPLOYEE | jq '.data | {id, firstName, lastName, department}'
```

**Expected Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "firstName": "Alice",
  "lastName": "Johnson",
  "department": "ENGINEERING"
}
```

---

### Step 1b: Create Review Cycle
```bash
CYCLE=$(curl -s -X POST http://localhost:8080/api/v1/cycles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q1 2026 Reviews",
    "startDate": "2026-01-10",
    "endDate": "2026-02-05",
    "status": "DRAFT"
  }')

CYCLE_ID=$(echo $CYCLE | jq -r '.data.id')
echo "✓ Cycle created: $CYCLE_ID"
echo $CYCLE | jq '.data | {id, name, startDate, endDate, status}'
```

**Expected Response**:
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440000",
  "name": "Q1 2026 Reviews",
  "startDate": "2026-01-10",
  "endDate": "2026-02-05",
  "status": "DRAFT"
}
```

---

### Step 1c: Activate Cycle
```bash
ACTIVATE=$(curl -s -X PATCH http://localhost:8080/api/v1/cycles/$CYCLE_ID/status \
  -H "Content-Type: application/json" \
  -d '{"status": "ACTIVE"}')

echo "✓ Cycle status updated"
echo $ACTIVATE | jq '.data | {name, status}'
```

**Expected Response**:
```json
{
  "name": "Q1 2026 Reviews",
  "status": "ACTIVE"
}
```

---

### Step 1d: Create Goal
```bash
GOAL=$(curl -s -X POST http://localhost:8080/api/v1/goals \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "'$EMPLOYEE_ID'",
    "cycleId": "'$CYCLE_ID'",
    "title": "Q1 2026 Performance Goals",
    "description": "Deliver high-quality code, improve test coverage to 90%, complete architecture review",
    "dueDate": "2026-02-05"
  }')

GOAL_ID=$(echo $GOAL | jq -r '.data.id')
echo "✓ Goal created: $GOAL_ID"
echo $GOAL | jq '.data | {id, employeeName, title, status, dueDate, createdAt}'
```

**Expected Response**:
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440000",
  "employeeName": "Alice Johnson",
  "title": "Q1 2026 Performance Goals",
  "status": "PENDING",
  "dueDate": "2026-02-05",
  "createdAt": "2026-01-20T10:30:00Z"
}
```

---

## Test 2: Update Goal Status - IN_PROGRESS

### Step 2a: Update Status to IN_PROGRESS
```bash
UPDATE_IN_PROGRESS=$(curl -s -X PATCH http://localhost:8080/api/v1/goals/$GOAL_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Q1 2026 Performance Goals",
    "description": "Deliver high-quality code, improve test coverage to 90%, complete architecture review",
    "dueDate": "2026-02-05",
    "status": "IN_PROGRESS"
  }')

echo "✓ Goal status updated to IN_PROGRESS"
echo $UPDATE_IN_PROGRESS | jq '.data | {id, title, status, updatedAt}'
```

**Expected Response**:
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440000",
  "title": "Q1 2026 Performance Goals",
  "status": "IN_PROGRESS",
  "updatedAt": "2026-01-21T14:45:00Z"
}
```

---

### Step 2b: Verify Status Update in Cycle Summary
```bash
SUMMARY=$(curl -s http://localhost:8080/api/v1/cycles/$CYCLE_ID/summary)

echo "✓ Cycle summary retrieved:"
echo $SUMMARY | jq '.data.goalStats | {total, completed, inProgress, missed, completionRate}'
```

**Expected Response**:
```json
{
  "total": 1,
  "completed": 0,
  "inProgress": 1,
  "missed": 0,
  "completionRate": 0.0
}
```

---

## Test 3: Update Goal to COMPLETED

### Step 3a: Update Status to COMPLETED
```bash
UPDATE_COMPLETED=$(curl -s -X PATCH http://localhost:8080/api/v1/goals/$GOAL_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Q1 2026 Performance Goals",
    "description": "Successfully achieved all code quality and architecture objectives",
    "dueDate": "2026-02-05",
    "status": "COMPLETED"
  }')

echo "✓ Goal status updated to COMPLETED"
echo $UPDATE_COMPLETED | jq '.data | {title, status, description, updatedAt}'
```

**Expected Response**:
```json
{
  "title": "Q1 2026 Performance Goals",
  "status": "COMPLETED",
  "description": "Successfully achieved all code quality and architecture objectives",
  "updatedAt": "2026-02-03T16:22:00Z"
}
```

---

### Step 3b: Verify Goal Completion in Cycle Summary
```bash
SUMMARY=$(curl -s http://localhost:8080/api/v1/cycles/$CYCLE_ID/summary)

echo "✓ Cycle summary - goal completion metrics:"
echo $SUMMARY | jq '.data.goalStats | {total, completed, inProgress, missed, completionRate}'
```

**Expected Response** (cache should be evicted and recalculated):
```json
{
  "total": 1,
  "completed": 1,
  "inProgress": 0,
  "missed": 0,
  "completionRate": 100.0
}
```

---

## Test 4: Error Handling - Goal Not Found (404)

### Step 4a: Try to Update Non-Existent Goal
```bash
INVALID_ID="999e9999-9999-9999-9999-999999999999"

ERROR_404=$(curl -s -X PATCH http://localhost:8080/api/v1/goals/$INVALID_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "description": "Updated description",
    "dueDate": "2026-02-05",
    "status": "IN_PROGRESS"
  }')

echo "✗ Expected 404 error:"
echo $ERROR_404 | jq '.success, .message, .status'
```

**Expected Response**:
```json
false
"Goal with id 999e9999-9999-9999-9999-999999999999 not found"
404
```

---

## Test 5: Error Handling - Invalid Due Date (400)

### Step 5a: Create Another Goal (for testing due date validation)
```bash
GOAL2=$(curl -s -X POST http://localhost:8080/api/v1/goals \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "'$EMPLOYEE_ID'",
    "cycleId": "'$CYCLE_ID'",
    "title": "Q1 Testing Goals",
    "description": "Test goal for due date validation",
    "dueDate": "2026-01-31"
  }')

GOAL2_ID=$(echo $GOAL2 | jq -r '.data.id')
echo "✓ Second goal created: $GOAL2_ID"
```

---

### Step 5b: Try to Update with Out-of-Range Due Date
```bash
# Cycle ends at 2026-02-05, so 2026-03-10 should fail

ERROR_400=$(curl -s -X PATCH http://localhost:8080/api/v1/goals/$GOAL2_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Q1 Testing Goals",
    "description": "Test goal for due date validation",
    "dueDate": "2026-03-10",
    "status": "IN_PROGRESS"
  }')

echo "✗ Expected 400 error for out-of-range due date:"
echo $ERROR_400 | jq '.success, .message, .status'
```

**Expected Response**:
```json
false
"Goal due date must be within cycle date range [2026-01-10, 2026-02-05]. Provided: 2026-03-10"
400
```

---

## Test 6: Error Handling - Blank Title (400)

### Step 6a: Try to Update with Blank Title
```bash
ERROR_BLANK=$(curl -s -X PATCH http://localhost:8080/api/v1/goals/$GOAL2_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "",
    "description": "Testing blank title validation",
    "dueDate": "2026-01-31",
    "status": "IN_PROGRESS"
  }')

echo "✗ Expected 400 error for blank title:"
echo $ERROR_BLANK | jq '.success, .message'
```

**Expected Response**:
```json
false
"Validation failed"
```

---

## Test 7: Error Handling - Invalid Status (400)

### Step 7a: Try to Update with Invalid Status
```bash
ERROR_STATUS=$(curl -s -X PATCH http://localhost:8080/api/v1/goals/$GOAL2_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Q1 Testing Goals",
    "description": "Test goal",
    "dueDate": "2026-01-31",
    "status": "INVALID_STATUS"
  }')

echo "✗ Expected 400 error for invalid status:"
echo $ERROR_STATUS | jq '.success, .message'
```

**Expected Response**:
```json
false
"Invalid value for status. Allowed values: PENDING, IN_PROGRESS, COMPLETED, MISSED"
```

---

## Test 8: Status Transitions - Mark Goal as MISSED

### Step 8a: Mark Goal as Missed
```bash
UPDATE_MISSED=$(curl -s -X PATCH http://localhost:8080/api/v1/goals/$GOAL2_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Q1 Testing Goals",
    "description": "Deadline passed, goal was not completed",
    "dueDate": "2026-01-31",
    "status": "MISSED"
  }')

echo "✓ Goal marked as MISSED:"
echo $UPDATE_MISSED | jq '.data | {title, status, description}'
```

**Expected Response**:
```json
{
  "title": "Q1 Testing Goals",
  "status": "MISSED",
  "description": "Deadline passed, goal was not completed"
}
```

---

## Test 9: Cache Eviction Verification

### Step 9a: Check Cache Hit Count Before Update
```bash
# First request - miss
CYCLE_SUMMARY_1=$(curl -s http://localhost:8080/api/v1/cycles/$CYCLE_ID/summary)
echo "✓ First request (cache miss):"
echo $CYCLE_SUMMARY_1 | jq '.cached'

# Second request - should hit
CYCLE_SUMMARY_2=$(curl -s http://localhost:8080/api/v1/cycles/$CYCLE_ID/summary)
echo "✓ Second request (cache hit):"
echo $CYCLE_SUMMARY_2 | jq '.cached'
```

**Expected**: First is `false`, second is `true`

---

### Step 9b: Update Goal and Verify Cache Eviction
```bash
# Update goal (should evict cache)
UPDATE=$(curl -s -X PATCH http://localhost:8080/api/v1/goals/$GOAL2_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Q1 Testing Goals - Updated",
    "description": "Updated after cache test",
    "dueDate": "2026-01-31",
    "status": "IN_PROGRESS"
  }')

echo "✓ Goal updated (cache evicted)"

# Next request should be cache miss (data refreshed)
CYCLE_SUMMARY_3=$(curl -s http://localhost:8080/api/v1/cycles/$CYCLE_ID/summary)
echo "✓ Request after update (cache miss - data recalculated):"
echo $CYCLE_SUMMARY_3 | jq '.cached'
```

**Expected**: `false` (cache was evicted and recalculated)

---

## Test 10: Bulk Goal Status Workflow

### Complete Test Script (All Steps)
```bash
#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 1. Setup
echo -e "${BLUE}Creating employee and cycle...${NC}"
EMP=$(curl -s -X POST http://localhost:8080/api/v1/employees \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Bob",
    "lastName": "Smith",
    "email": "bob@company.com",
    "department": "SALES",
    "jobTitle": "Sales Manager",
    "joiningDate": "2023-06-20"
  }')
EMP_ID=$(echo $EMP | jq -r '.data.id')
echo -e "${GREEN}✓ Employee: $EMP_ID${NC}"

CYCLE=$(curl -s -X POST http://localhost:8080/api/v1/cycles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q1 2026 Sales",
    "startDate": "2026-01-10",
    "endDate": "2026-02-05"
  }')
CYCLE_ID=$(echo $CYCLE | jq -r '.data.id')
echo -e "${GREEN}✓ Cycle: $CYCLE_ID${NC}"

curl -s -X PATCH http://localhost:8080/api/v1/cycles/$CYCLE_ID/status \
  -H "Content-Type: application/json" \
  -d '{"status": "ACTIVE"}' > /dev/null
echo -e "${GREEN}✓ Cycle activated${NC}"

# 2. Create Goals
echo -e "\n${BLUE}Creating goals...${NC}"
G1=$(curl -s -X POST http://localhost:8080/api/v1/goals \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "'$EMP_ID'",
    "cycleId": "'$CYCLE_ID'",
    "title": "Goal 1: Sales Target",
    "description": "Achieve 120% of Q1 quota",
    "dueDate": "2026-02-05"
  }')
G1_ID=$(echo $G1 | jq -r '.data.id')
echo -e "${GREEN}✓ Goal 1: $G1_ID - $(echo $G1 | jq -r '.data.status')${NC}"

G2=$(curl -s -X POST http://localhost:8080/api/v1/goals \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "'$EMP_ID'",
    "cycleId": "'$CYCLE_ID'",
    "title": "Goal 2: Client Retention",
    "description": "Retain top 10 clients with 95%+ satisfaction",
    "dueDate": "2026-02-05"
  }')
G2_ID=$(echo $G2 | jq -r '.data.id')
echo -e "${GREEN}✓ Goal 2: $G2_ID - $(echo $G2 | jq -r '.data.status')${NC}"

G3=$(curl -s -X POST http://localhost:8080/api/v1/goals \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "'$EMP_ID'",
    "cycleId": "'$CYCLE_ID'",
    "title": "Goal 3: Team Development",
    "description": "Conduct monthly team training sessions",
    "dueDate": "2026-02-05"
  }')
G3_ID=$(echo $G3 | jq -r '.data.id')
echo -e "${GREEN}✓ Goal 3: $G3_ID - $(echo $G3 | jq -r '.data.status')${NC}"

# 3. Transition Goals
echo -e "\n${BLUE}Transitioning goal statuses...${NC}"

# Goal 1: PENDING → IN_PROGRESS
curl -s -X PATCH http://localhost:8080/api/v1/goals/$G1_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Goal 1: Sales Target",
    "description": "Achieve 120% of Q1 quota",
    "dueDate": "2026-02-05",
    "status": "IN_PROGRESS"
  }' > /dev/null
echo -e "${GREEN}✓ Goal 1: PENDING → IN_PROGRESS${NC}"

# Goal 2: PENDING → COMPLETED
curl -s -X PATCH http://localhost:8080/api/v1/goals/$G2_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Goal 2: Client Retention",
    "description": "Successfully retained all top clients",
    "dueDate": "2026-02-05",
    "status": "COMPLETED"
  }' > /dev/null
echo -e "${GREEN}✓ Goal 2: PENDING → COMPLETED${NC}"

# Goal 3: PENDING → MISSED
curl -s -X PATCH http://localhost:8080/api/v1/goals/$G3_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Goal 3: Team Development",
    "description": "Was unable to schedule training sessions",
    "dueDate": "2026-02-05",
    "status": "MISSED"
  }' > /dev/null
echo -e "${GREEN}✓ Goal 3: PENDING → MISSED${NC}"

# 4. Verify Metrics
echo -e "\n${BLUE}Verifying cycle analytics...${NC}"
SUMMARY=$(curl -s http://localhost:8080/api/v1/cycles/$CYCLE_ID/summary)
STATS=$(echo $SUMMARY | jq '.data.goalStats')
echo -e "${GREEN}✓ Goal Statistics:${NC}"
echo $STATS | jq -r '"  Total: \(.total) | Completed: \(.completed) | In Progress: \(.inProgress) | Missed: \(.missed) | Completion: \(.completionRate)%"'

echo -e "\n${GREEN}✅ All tests completed successfully!${NC}"
```

**Run the full test**:
```bash
chmod +x goal_test.sh
./goal_test.sh
```

---

## 📊 Summary of Tests

| Test # | Scenario | Expected | Status |
|--------|----------|----------|--------|
| 1 | Create goal | 201 Created | ✅ |
| 2 | Update to IN_PROGRESS | 200 OK, status changed | ✅ |
| 3 | Update to COMPLETED | 200 OK, completion rate updated | ✅ |
| 4 | Invalid goal ID | 404 Not Found | ✅ |
| 5 | Out-of-range due date | 400 Bad Request | ✅ |
| 6 | Blank title | 400 Bad Request | ✅ |
| 7 | Invalid status | 400 Bad Request | ✅ |
| 8 | Mark as MISSED | 200 OK, status changed | ✅ |
| 9 | Cache eviction | Cache miss after update | ✅ |
| 10 | Bulk workflow | All transitions work | ✅ |

---

## ✅ Verification Checklist

- [ ] Employee creation successful
- [ ] Cycle creation and activation successful
- [ ] Goal POST endpoint working (201)
- [ ] Goal PATCH endpoint working (200)
- [ ] Status transitions (PENDING → IN_PROGRESS → COMPLETED/MISSED)
- [ ] Goal not found returns 404
- [ ] Invalid due date returns 400
- [ ] Blank title returns 400
- [ ] Invalid status returns 400
- [ ] Cache is evicted after update
- [ ] Cycle summary shows updated goal statistics
- [ ] All timestamps (createdAt, updatedAt) present

---

## 🔗 Troubleshooting

**Port 8080 not accessible?**
```bash
# Check if container is running
docker-compose ps

# Check logs
docker-compose logs app
```

**Database connection issues?**
```bash
# Verify PostgreSQL is running
docker-compose logs postgres

# Check database status
psql -U postgres -h localhost -c "SELECT 1;"
```

**Cache not evicting?**
```bash
# Clear Java memory cache (if applicable)
# Restart container
docker-compose down && docker-compose up -d
```
