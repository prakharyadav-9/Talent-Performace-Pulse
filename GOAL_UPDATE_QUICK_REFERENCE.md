# Goal Update Endpoint - Quick Reference Guide

## 📋 Overview

The **PATCH /api/v1/goals/{goalId}** endpoint enables updating performance goals with status tracking and comprehensive validation.

---

## ⚡ Quick Examples

### ✓ Update Status Only
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/{goalId} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Current Title",
    "description": "Current description",
    "dueDate": "2026-02-05",
    "status": "IN_PROGRESS"
  }'
```

### ✓ Complete a Goal
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/{goalId} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Current Title",
    "description": "Successfully completed",
    "dueDate": "2026-02-05",
    "status": "COMPLETED"
  }'
```

### ✓ Mark Goal as Missed
```bash
curl -X PATCH http://localhost:8080/api/v1/goals/{goalId} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Current Title",
    "description": "Missed deadline",
    "dueDate": "2026-02-05",
    "status": "MISSED"
  }'
```

---

## 📊 Request & Response

### Request Format

**URL**: `PATCH /api/v1/goals/{goalId}`

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "title": "string",           // Required: 1-255 chars
  "description": "string",     // Optional: max 2000 chars
  "dueDate": "YYYY-MM-DD",     // Required: within cycle range
  "status": "ENUM"             // Required: PENDING|IN_PROGRESS|COMPLETED|MISSED
}
```

### Success Response (200 OK)
```json
{
  "data": {
    "id": "uuid",
    "employeeId": "uuid",
    "employeeName": "string",
    "cycleId": "uuid",
    "cycleName": "string",
    "title": "string",
    "description": "string",
    "status": "IN_PROGRESS",
    "dueDate": "2026-02-05",
    "weight": 1,
    "createdAt": "ISO8601",
    "updatedAt": "ISO8601"
  },
  "success": true,
  "message": "Goal updated successfully"
}
```

---

## ✅ Validation Rules

| Field | Rule | Error |
|-------|------|-------|
| `goalId` | Must exist | 404 Not Found |
| `title` | Cannot be blank, 1-255 chars | 400 Bad Request |
| `description` | Optional, max 2000 chars | 400 Bad Request |
| `dueDate` | Within cycle date range | 400 Bad Request |
| `status` | Valid enum value | 400 Bad Request |

---

## 🔄 Status Transitions

### Valid Transitions
```
PENDING
  ↓ ↓ ↓
  IN_PROGRESS → COMPLETED (final)
  ↓           → MISSED (final)
  ↓ → COMPLETED
  ↓ → MISSED

IN_PROGRESS
  ↓ → COMPLETED (final)
  ↓ → MISSED (final)
  ↓ → IN_PROGRESS (same, update details)
  ↓ → PENDING (revert if needed)

COMPLETED (readonly)
MISSED (readonly)
```

---

## 🆘 Error Scenarios

### Error 404: Goal Not Found
```json
{
  "success": false,
  "message": "Goal with id {goalId} not found",
  "status": 404
}
```
**Solution**: Verify goalId exists; use POST /api/v1/goals to create

---

### Error 400: Invalid Due Date
```json
{
  "success": false,
  "message": "Goal due date must be within cycle date range [2026-01-10, 2026-02-05]. Provided: 2026-03-10",
  "status": 400
}
```
**Solution**: Use date within original cycle's start and end date

---

### Error 400: Blank Title
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
**Solution**: Provide non-empty title (1-255 characters)

---

### Error 400: Invalid Status
```json
{
  "success": false,
  "message": "Invalid value for status. Allowed values: PENDING, IN_PROGRESS, COMPLETED, MISSED",
  "status": 400
}
```
**Solution**: Use one of the valid enum values

---

## 🧪 Test Workflow

### 1. Create Employee & Cycle
```bash
# Create employee
EMP=$(curl -s -X POST http://localhost:8080/api/v1/employees \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Alice","lastName":"Johnson","email":"alice@company.com","department":"ENGINEERING"}')
EMPLOYEE_ID=$(echo $EMP | jq -r '.data.id')

# Create cycle
CYCLE=$(curl -s -X POST http://localhost:8080/api/v1/cycles \
  -H "Content-Type: application/json" \
  -d '{"name":"Q1 2026","startDate":"2026-01-10","endDate":"2026-02-05"}')
CYCLE_ID=$(echo $CYCLE | jq -r '.data.id')

# Activate cycle
curl -s -X PATCH http://localhost:8080/api/v1/cycles/$CYCLE_ID/status \
  -H "Content-Type: application/json" \
  -d '{"status":"ACTIVE"}' > /dev/null
```

### 2. Create Goal
```bash
GOAL=$(curl -s -X POST http://localhost:8080/api/v1/goals \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId":"'$EMPLOYEE_ID'",
    "cycleId":"'$CYCLE_ID'",
    "title":"Q1 Leadership Goals",
    "description":"Lead team through Q1 initiatives",
    "dueDate":"2026-02-05"
  }')
GOAL_ID=$(echo $GOAL | jq -r '.data.id')
echo "Created goal: $GOAL_ID"
```

### 3. Update to IN_PROGRESS
```bash
UPDATE=$(curl -s -X PATCH http://localhost:8080/api/v1/goals/$GOAL_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Q1 Leadership Goals",
    "description":"Leading team through Q1 initiatives",
    "dueDate":"2026-02-05",
    "status":"IN_PROGRESS"
  }')
echo "Updated to IN_PROGRESS:"
echo $UPDATE | jq '.data.status'
```

### 4. Complete Goal
```bash
COMPLETE=$(curl -s -X PATCH http://localhost:8080/api/v1/goals/$GOAL_ID \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Q1 Leadership Goals",
    "description":"Successfully led team through Q1, achieved all objectives",
    "dueDate":"2026-02-05",
    "status":"COMPLETED"
  }')
echo "Goal completed:"
echo $COMPLETE | jq '.data | {status, updatedAt}'
```

---

## 🔌 Integration Points

### Cache Invalidation
- **Auto-evicts**: `cycle-summary` cache on updates
- **Effect**: Next `GET /cycles/{id}/summary` recalculates analytics
- **Latency**: <50ms cache rebuild

### Goal Statistics in Cycle Summary
```json
"goalStats": {
  "total": 3,
  "completed": 1,
  "inProgress": 1,
  "missed": 1,
  "completionRate": 33.3
}
```

---

## 📝 Notes

- ✓ All dates in `YYYY-MM-DD` format
- ✓ Status is immutable once set to COMPLETED or MISSED
- ✓ Due date cannot extend beyond cycle date range
- ✓ Updates include `updatedAt` timestamp automatically
- ✓ Cache eviction ensures analytics freshness
- ✓ Full audit trail via JPA @Audited annotations

---

## 🔗 Related Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| /api/v1/goals | POST | Create goal |
| /api/v1/goals/{goalId} | PATCH | Update goal ← YOU ARE HERE |
| /api/v1/cycles/{id}/summary | GET | View goal statistics |
| /api/v1/employees/{id}/reviews | GET | View employee reviews |

---

## 📚 Full Documentation

See **GOAL_UPDATE_ENDPOINT_SAMPLES.md** for:
- 4 complete request examples
- 5 error scenario examples
- Complete bash test workflow script
- Request format reference table
- Valid status transitions

See **PHASE3_COMPLETION_DOCUMENT.md** for:
- Full Phase 3 API documentation
- Database schema
- Service layer details
- Performance metrics
- Caching strategy
