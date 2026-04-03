# Quick Start Guide - Running E2E Tests

## 5-Minute Setup

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Start Application
```bash
docker-compose up -d
```

Wait for startup (~15 seconds):
```bash
docker-compose logs -f app | grep "started successfully"
```

### 3. Run Tests
```bash
python test_e2e.py
```

## Expected Results

✓ All tests pass with colored output showing:
- Employee creation (4 employees)
- Cycle creation and activation
- Goal creation (3 goals)
- Review submission (3 reviews)
- Review retrieval
- Employee filtering by department and rating
- Cycle summary with analytics

## Files Created

| File | Purpose |
|------|---------|
| `test_e2e.py` | Main test script (670+ lines) |
| `TEST_E2E_README.md` | Comprehensive documentation |
| `requirements.txt` | Python dependencies |
| `QUICK_START.md` | This file |

## Common Issues

**ImportError: No module named 'requests'**
```bash
pip install requests
```

**Connection refused**
```bash
docker-compose up -d  # Ensure containers are running
docker-compose ps
```

**API taking long to start**
```bash
# Check logs
docker-compose logs app

# Wait longer before running tests
sleep 30
python test_e2e.py
```

## What Gets Tested

✓ **Employees** - CRUD operations  
✓ **Cycles** - Create, activate, retrieve  
✓ **Goals** - Create with employee/cycle references  
✓ **Reviews** - Peer and self reviews  
✓ **Filtering** - Department and rating filters with caching  
✓ **Analytics** - Cycle summary with performance metrics  

## Script Features

- **Automated test sequence** - No manual steps
- **Colored output** - Easy to read results
- **Error handling** - Graceful failure messages
- **Test summary** - Pass/fail counts
- **Realistic data** - Uses timestamps for unique values
- **Comprehensive logging** - Debug-friendly output

## Next Steps

1. **Review output** - Check for any failed tests
2. **Check logs** - `docker-compose logs app`
3. **Verify DB** - Connect to PostgreSQL to inspect data
4. **Scale tests** - Extend with additional test cases as needed

## Exit Codes

```bash
python test_e2e.py
echo $?  # 0 = success, 1 = failure
```

---

**For detailed documentation, see [TEST_E2E_README.md](TEST_E2E_README.md)**
