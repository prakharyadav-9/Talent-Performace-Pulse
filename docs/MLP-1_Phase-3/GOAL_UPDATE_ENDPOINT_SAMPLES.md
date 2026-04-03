# Goal Update Endpoint Documentation

## PATCH /api/v1/goals/{goalId} - Update Goal

**Purpose**: Update an existing performance goal including title, description, due date, and status.

**Status Code**: 200 OK (Success), 404 Not Found (Goal doesn't exist), 400 Bad Request (Validation error), 422 Unprocessable Entity (Business rule violation)

---

## Sample Request 1: Update Goal Status Only

**Request**:
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/323e4567-e89b-12d3-a456-426614174002 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Q1 2026 Performance Goals",
    "description": "Deliver high-quality code, improve test coverage to 90%, complete architecture review",
    "dueDate": "2026-02-05",
    "status": "IN_PROGRESS"
  }'
```

**Response** (200 OK):
```json
{
  "data": {
    "id": "323e4567-e89b-12d3-a456-426614174002",
    "employeeId": "123e4567-e89b-12d3-a456-426614174000",
    "employeeName": "Alice Johnson",
    "cycleId": "223e4567-e89b-12d3-a456-426614174001",
    "cycleName": "Q1 2026 Reviews",
    "title": "Q1 2026 Performance Goals",
    "description": "Deliver high-quality code, improve test coverage to 90%, complete architecture review",
    "status": "IN_PROGRESS",
    "dueDate": "2026-02-05",
    "weight": 1,
    "createdAt": "2026-01-15T10:30:00Z",
    "updatedAt": "2026-01-20T14:45:00Z"
  },
  "success": true,
  "message": "Goal updated successfully"
}
```

---

## Sample Request 2: Update to COMPLETED Status

**Request**:
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/323e4567-e89b-12d3-a456-426614174002 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Q1 2026 Performance Goals",
    "description": "Deliver high-quality code, improve test coverage to 90%, complete architecture review",
    "dueDate": "2026-02-05",
    "status": "COMPLETED"
  }'
```

**Response** (200 OK):
```json
{
  "data": {
    "id": "323e4567-e89b-12d3-a456-426614174002",
    "employeeId": "123e4567-e89b-12d3-a456-426614174000",
    "employeeName": "Alice Johnson",
    "cycleId": "223e4567-e89b-12d3-a456-426614174001",
    "cycleName": "Q1 2026 Reviews",
    "title": "Q1 2026 Performance Goals",
    "description": "Deliver high-quality code, improve test coverage to 90%, complete architecture review",
    "status": "COMPLETED",
    "dueDate": "2026-02-05",
    "weight": 1,
    "createdAt": "2026-01-15T10:30:00Z",
    "updatedAt": "2026-02-03T16:22:00Z"
  },
  "success": true,
  "message": "Goal updated successfully"
}
```

---

## Sample Request 3: Update to MISSED Status

**Request**:
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/323e4567-e89b-12d3-a456-426614174002 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Q1 2026 Performance Goals",
    "description": "Deliver high-quality code, improve test coverage to 90%, complete architecture review",
    "dueDate": "2026-02-05",
    "status": "MISSED"
  }'
```

**Response** (200 OK):
```json
{
  "data": {
    "id": "323e4567-e89b-12d3-a456-426614174002",
    "employeeId": "123e4567-e89b-12d3-a456-426614174000",
    "employeeName": "Alice Johnson",
    "cycleName": "Q1 2026 Reviews",
    "title": "Q1 2026 Performance Goals",
    "status": "MISSED",
    "dueDate": "2026-02-05",
    "updatedAt": "2026-02-06T09:15:00Z"
  },
  "success": true
}
```

---

## Sample Request 4: Update Multiple Fields

**Request** (Change title, description, due date, and status):
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/323e4567-e89b-12d3-a456-426614174002 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Q1 Updated Goals - Extended",
    "description": "Deliver high-quality code, improve test coverage to 95%, complete full architecture review, and provide documentation",
    "dueDate": "2026-02-15",
    "status": "IN_PROGRESS"
  }'
```

**Response** (200 OK):
```json
{
  "data": {
    "id": "323e4567-e89b-12d3-a456-426614174002",
    "employeeId": "123e4567-e89b-12d3-a456-426614174000",
    "employeeName": "Alice Johnson",
    "title": "Q1 Updated Goals - Extended",
    "description": "Deliver high-quality code, improve test coverage to 95%, complete full architecture review, and provide documentation",
    "dueDate": "2026-02-15",
    "status": "IN_PROGRESS",
    "updatedAt": "2026-01-22T10:30:00Z"
  },
  "success": true
}
```

---

## Error Scenarios

### Error 1: Goal Not Found (404)

**Request**:
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/999e9999-9999-9999-9999-999999999999 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "description": "Updated description",
    "dueDate": "2026-02-05",
    "status": "IN_PROGRESS"
  }'
```

**Response** (404 Not Found):
```json
{
  "success": false,
  "message": "Goal with id 999e9999-9999-9999-9999-999999999999 not found",
  "status": 404
}
```

---

### Error 2: Due Date Outside Cycle Range (400)

**Request** (due date after cycle end date):
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/323e4567-e89b-12d3-a456-426614174002 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "description": "Updated description",
    "dueDate": "2026-03-10",
    "status": "IN_PROGRESS"
  }'
```

**Response** (400 Bad Request):
```json
{
  "success": false,
  "message": "Goal due date must be within cycle date range [2026-01-10, 2026-02-05]. Provided: 2026-03-10",
  "status": 400
}
```

---

### Error 3: Missing Required Field (400)

**Request** (missing status):
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/323e4567-e89b-12d3-a456-426614174002 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "description": "Updated description",
    "dueDate": "2026-02-05"
  }'
```

**Response** (400 Bad Request):
```json
{
  "success": false,
  "message": "Field 'status' is required",
  "status": 400,
  "errors": [
    {
      "field": "status",
      "message": "must not be null"
    }
  ]
}
```

---

### Error 4: Invalid Status Value (400)

**Request** (invalid status enum):
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/323e4567-e89b-12d3-a456-426614174002 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "description": "Updated description",
    "dueDate": "2026-02-05",
    "status": "INVALID_STATUS"
  }'
```

**Response** (400 Bad Request):
```json
{
  "success": false,
  "message": "Invalid value for status. Allowed values: PENDING, IN_PROGRESS, COMPLETED, MISSED",
  "status": 400
}
```

---

### Error 5: Blank Title (400)

**Request** (empty title):
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/323e4567-e89b-12d3-a456-426614174002 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "",
    "description": "Updated description",
    "dueDate": "2026-02-05",
    "status": "IN_PROGRESS"
  }'
```

**Response** (400 Bad Request):
```json
{
  "success": false,
  "message": "Validation failed",
  "status": 400,
  "errors": [
    {
      "field": "title",
      "message": "Title cannot be blank"
    }
  ]
}
```

---

## Request Format Reference

### UpdateGoalRequest Fields

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `title` | String | ✓ Yes | 1-255 chars, non-blank | Goal title/name |
| `description` | String | ✗ Optional | Max 2000 chars | Detailed goal description |
| `dueDate` | Date (YYYY-MM-DD) | ✓ Yes | Within cycle date range | Goal deadline |
| `status` | Enum | ✓ Yes | PENDING, IN_PROGRESS, COMPLETED, MISSED | Goal status |

---

## Valid Goal Status Values

| Status | Description | Use Case |
|--------|-------------|----------|
| `PENDING` | Goal created but not started | Initial state |
| `IN_PROGRESS` | Goal is actively being worked on | Work has begun |
| `COMPLETED` | Goal successfully achieved | Goal finished successfully |
| `MISSED` | Goal deadline passed without completion | Goal not achieved |

---

## Validation Rules

✓ Goal must exist (404 if not)  
✓ Title cannot be blank and must be 1-255 characters  
✓ Description optional but max 2000 characters  
✓ Due date must be within cycle start and end date (400 if outside)  
✓ Status must be valid GoalStatus enum value (400 if invalid)  
✓ Cache is evicted on successful update (cycle-summary cache)

---

## Complete Test Workflow (Bash Script)

```bash
#!/bin/bash

BASE_URL="http://localhost:8080/api/v1"
CONTENT_TYPE="Content-Type: application/json"

# 1. Create a goal first
echo "Creating employee and cycle..."
EMP=$(curl -s -X POST $BASE_URL/employees \
  -H "$CONTENT_TYPE" \
  -d '{
    "firstName": "Bob",
    "lastName": "Smith",
    "email": "bob@example.com",
    "department": "SALES",
    "jobTitle": "Sales Manager",
    "joiningDate": "2023-03-20"
  }')
EMPLOYEE_ID=$(echo $EMP | jq -r '.data.id')

CYCLE=$(curl -s -X POST $BASE_URL/cycles \
  -H "$CONTENT_TYPE" \
  -d '{
    "name": "Q1 2026",
    "startDate": "2026-01-10",
    "endDate": "2026-02-05"
  }')
CYCLE_ID=$(echo $CYCLE | jq -r '.data.id')

curl -s -X PATCH $BASE_URL/cycles/$CYCLE_ID/status \
  -H "$CONTENT_TYPE" \
  -d '{"status": "ACTIVE"}' > /dev/null

echo "Creating goal..."
GOAL=$(curl -s -X POST $BASE_URL/goals \
  -H "$CONTENT_TYPE" \
  -d '{
    "employeeId": "'$EMPLOYEE_ID'",
    "cycleId": "'$CYCLE_ID'",
    "title": "Q1 Sales Targets",
    "description": "Achieve 120% of Q1 quota",
    "dueDate": "2026-02-05"
  }')
GOAL_ID=$(echo $GOAL | jq -r '.data.id')
echo "✓ Goal created: $GOAL_ID"

# 2. Update goal status to IN_PROGRESS
echo ""
echo "Updating goal to IN_PROGRESS..."
UPDATE=$(curl -s -X PATCH $BASE_URL/goals/$GOAL_ID \
  -H "$CONTENT_TYPE" \
  -d '{
    "title": "Q1 Sales Targets",
    "description": "Achieve 120% of Q1 quota",
    "dueDate": "2026-02-05",
    "status": "IN_PROGRESS"
  }')
echo $UPDATE | jq '.data | {id, status, updatedAt}'

# 3. Update goal to COMPLETED
echo ""
echo "Completing goal..."
COMPLETED=$(curl -s -X PATCH $BASE_URL/goals/$GOAL_ID \
  -H "$CONTENT_TYPE" \
  -d '{
    "title": "Q1 Sales Targets - COMPLETED",
    "description": "Successfully achieved 120% of Q1 quota",
    "dueDate": "2026-02-05",
    "status": "COMPLETED"
  }')
echo $COMPLETED | jq '.data | {id, status, title, updatedAt}'

echo ""
echo "✓ Goal workflow completed!"
```

---

## Integration with Cycle Summary

When a goal status is updated, the **cycle-summary cache** is automatically evicted. This ensures that:
- Goal completion rate in cycle summary is always up-to-date
- Top performer calculations reflect latest goal status changes
- Next cycle summary request will recalculate all metrics

**Example**: If you update a goal from `IN_PROGRESS` to `COMPLETED`, the cycle summary's `goalStats.completionRate` will be recalculated on the next request.

---

## Notes

- All dates must be in `YYYY-MM-DD` format
- Status changes are tracked in the `updatedAt` timestamp
- Updates trigger cache eviction for performance (10-minute TTL)
- Invalid status values will be rejected with detailed error message
- Due date CANNOT be changed outside the original cycle's date range
