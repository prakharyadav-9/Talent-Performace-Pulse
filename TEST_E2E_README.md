# End-to-End Test Script - Performance Pulse API

A comprehensive Python test automation script that validates the entire Performance Pulse API workflow with all Phase 3 endpoints.

## Overview

This script tests the complete customer journey:
1. **Employee Management** - Create and retrieve employees
2. **Review Cycles** - Create review cycles and activate them
3. **Goals** - Create performance goals for employees
4. **Review Submission** - Submit peer reviews and self-reviews
5. **Review Retrieval** - Get all reviews for an employee
6. **Employee Filtering** - Filter by department and rating (with caching)
7. **Cycle Analytics** - Get comprehensive cycle summary with statistics

## Prerequisites

- **Python 3.8+** installed
- **requests library** - Install with: `pip install requests`
- **Performance Pulse API** running on `http://localhost:8080`
- **PostgreSQL database** properly initialized

## Installation

### 1. Install Python Dependencies

```bash
pip install requests
```

Or install from requirements (if you create one):

```bash
pip install -r requirements.txt
```

### 2. Start the Application

Make sure the Performance Pulse application is running:

```bash
# Option 1: Using Docker Compose
docker-compose up -d

# Option 2: Using Maven (if rebuilding)
mvn clean package
java -jar target/performancepulse-0.0.1-SNAPSHOT.jar
```

Wait for the application to be ready (typically 10-15 seconds):
```
2026-01-XX HH:MM:SS.XXX  INFO ... PerformancePulseApplication started successfully
```

## Running the Tests

### Basic Execution

```bash
python test_e2e.py
```

### With Output Logging

```bash
python test_e2e.py 2>&1 | tee test_results.log
```

### Running Specific Tests (Manual)

Edit the script to comment/uncomment specific test methods in the `run_all_tests()` method.

## Expected Output

The script provides colored, easy-to-read output:

```
======================================================================
              PERFORMANCE PULSE - END-TO-END TEST SUITE
======================================================================

→ API is healthy and responding
✓ API Health Check (1/1 passed)

======================================================================
                     2. CREATE EMPLOYEES
======================================================================

✓ Created employee 1: Alice Johnson (ID: 123)
✓ Created employee 2: Bob Smith (ID: 124)
✓ Created employee 3: Carol Williams (ID: 125)
✓ Created employee 4: David Brown (ID: 126)
→ Employees created: 4

... [more test output] ...

======================================================================
                   TEST EXECUTION SUMMARY
======================================================================

Total Tests:  XX
Passed:       ✓ XX
Failed:       ✗ 0
Success Rate: 100.0%

✓ All tests passed!
======================================================================
```

## Test Coverage

| Phase | Endpoint | Method | Test Case |
|-------|----------|--------|-----------|
| 1 | `POST /employees` | CREATE | Create 4 test employees |
| 2 | `POST /cycles` | CREATE | Create 2 review cycles |
| 2 | `PATCH /cycles/{id}/status` | UPDATE | Activate first cycle (UPCOMING → ACTIVE) |
| 2 | `POST /goals` | CREATE | Create goals for 3 employees |
| 3 | `POST /reviews` | CREATE | Submit 3 reviews (peer + self) |
| 3 | `GET /employees/{id}/reviews` | RETRIEVE | Get reviews for 2 employees |
| 3 | `GET /employees?department=X` | FILTER | Filter by department (ENGINEERING) |
| 3 | `GET /employees?minRating=X` | FILTER | Filter by minimum rating (3.0) |
| 3 | `GET /employees?department=X&minRating=Y` | FILTER | Combined filtering with caching |
| 3 | `GET /cycles/{id}/summary` | ANALYTICS | Get cycle summary with statistics |

## Test Data

### Employees Created
- **Alice Johnson** - ENGINEERING / Senior Software Engineer
- **Bob Smith** - SALES / Sales Manager  
- **Carol Williams** - ENGINEERING / Junior Engineer
- **David Brown** - HR / HR Manager

### Review Cycles
- **Q1 2026 Reviews** (ACTIVE) - Available for submissions
- **Q2 2026 Reviews** (UPCOMING) - Future cycle

### Reviews Submitted
1. Alice → Bob (Peer Review, Rating: 4)
2. Carol → Alice (Peer Review, Rating: 5)
3. David → Self (Self Review, Rating: 3)

## Features

✓ **Color-coded output** - Green for success, red for errors, blue for info
✓ **Comprehensive logging** - Clear status messages for each operation
✓ **Error handling** - Graceful handling of API errors with detailed messages
✓ **Test summary** - Final report with pass/fail counts and success rate
✓ **Realistic test data** - Uses current timestamps for unique emails
✓ **Proper JSON formatting** - Pretty-printed API responses for debugging

## Troubleshooting

### Connection Refused
```
✗ Failed to reach API: Connection refused
```
**Solution**: Ensure the application is running on port 8080
```bash
docker-compose ps  # Check if containers are running
```

### 400 Bad Request on Employee Creation
```
✗ Failed to create employee: 400
```
**Possible causes**:
- Duplicate email (script generates timestamps, shouldn't happen)
- Missing required fields
- Invalid date format (should be YYYY-MM-DD)

**Solution**: Check error message in response, verify database is initialized

### 404 Cycle Not Found
```
✗ Failed to create goal: 404
```
**Cause**: Cycle ID not found when creating goals

**Solution**: Ensure cycle creation test passed first

### Connection Timeout
```
✗ Exception: Connection timed out
```
**Cause**: Application is not responding

**Solution**: 
```bash
# Check container logs
docker-compose logs app

# Or restart containers
docker-compose restart
```

## Performance Metrics

Typical test execution times on standard hardware:
- **Full test suite**: 3-5 seconds
- **Employee creation**: ~500ms
- **Review submission**: ~300ms per review
- **Filtering queries**: ~100ms per request (~200ms with caching)

## Extending the Tests

To add more tests, follow this pattern:

```python
def test_new_feature(self):
    """Test description."""
    log_section("X. NEW TEST SECTION")
    
    try:
        response = self.session.post(f"{BASE_URL}/endpoint", json=data)
        if response.status_code == 201:
            log_success("Feature test passed")
            self.test_results["passed"] += 1
        else:
            log_error(f"Feature test failed: {response.status_code}")
            self.test_results["failed"] += 1
    except Exception as e:
        log_error(f"Exception: {str(e)}")
        self.test_results["failed"] += 1
    
    self.test_results["total"] += 1
```

Then add the call in `run_all_tests()`:
```python
self.test_new_feature()
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: password
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
        with:
          python-version: '3.10'
      
      - run: pip install requests
      - run: docker-compose up -d
      - run: sleep 15
      - run: python test_e2e.py
```

## Exit Codes

- **0**: All tests passed ✓
- **1**: One or more tests failed ✗

Use for CI/CD pipelines to determine build status.

## Security Notes

⚠️ **This is a test script, not for production use**

- Does not validate SSL certificates
- Uses hardcoded base URL and headers
- No authentication tokens (service runs without auth in dev mode)
- Test data uses realistic but random emails

For production testing:
- Use environment variables for credentials
- Implement proper authentication/authorization
- Use a dedicated test database
- Clean up test data after execution

## License

This test script is part of the Performance Pulse project.

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review application logs: `docker-compose logs app`
3. Verify database state: Connect to PostgreSQL and check tables
4. Check script output for specific error messages
