# Scaling Strategy: 500 Concurrent Managers

## Current Bottlenecks

### 1. **Database Connection Pool (Critical)**
- **Issue:** HikariCP configured with `maximum-pool-size: 20` and `minimum-idle: 5`
- **Impact:** Only ~25 concurrent requests can be served; remaining 475 managers face thread starvation & timeouts
- **Symptom:** `java.sql.SQLException: Cannot get a connection, pool error` under load

### 2. **Analytics Query Performance (Critical)**
- **Issue:** `buildCycleSummary()` executes 4-5 complex queries per request:
  - `SELECT COUNT(*)` from performance_reviews
  - `SELECT AVG(rating)` with `GROUP BY` logic
  - Native SQL for top performer with manual Map conversions
  - Goal statistics aggregations
- **Impact:** Each analytics request takes 200-500ms; 500 concurrent = 100-250 seconds of blocking DB time
- **Symptom:** Slow reports during performance season = stalled manager workflows

### 3. **Synchronous Request Handling**
- **Issue:** All I/O operations block request threads (no async/reactive patterns)
- **Impact:** Tomcat thread pool (max=200) exhausted by slow DB queries, preventing new requests
- **Calculation:** 500 requests × 300ms = 150,000ms of total block time; 200 threads → queue explosion

### 4. **Node-Local Caching (Caffeine)**
- **Issue:** Each application instance maintains separate in-memory cache
- **Impact:** With 3+ instances behind load balancer = stale/inconsistent cached data across managers
- **Symptom:** Manager A sees updated cycle summary, Manager B (routed to different instance) sees old data

### 5. **No Materialized Aggregations**
- **Issue:** Analytics queries recompute totals/averages every request from raw transactional tables
- **Impact:** Unnecessary full table scans; millions of review rows rescanned per query
- **Alternative:** Pre-compute metrics once every 5 minutes; serve O(1) lookups instead of O(n) aggregations

### 6. **Offset-Based Pagination**
- **Issue:** Employee list endpoint uses Spring Data's offset-based pagination
- **Query:** `SELECT * FROM employees ... OFFSET 1000000 LIMIT 20` (with 500M employees)
- **Impact:** Expensive sequential scan; PostgreSQL must count/skip millions of rows
- **Symptom:** List endpoints timeout when filtering by department + rating

### 7. **No Rate Limiting**
- **Issue:** No per-manager request throttling
- **Impact:** Single aggressive manager can launch 100 requests/sec → crash system for all others
- **Symptom:** Cascade failures during peak reporting periods

### 8. **Default Tomcat Configuration**
- **Issue:** `threads.max: 200` designed for single-server scenarios
- **Impact:** Can handle max 200 concurrent requests total; 500 managers = 60% request rejection rate

## Current Bottleneck
Your system's HikariCP pool is configured for **20 connections** (max) and **5 minimum-idle**. With 500 concurrent managers, this creates a critical bottleneck: only ~25 requests can be served simultaneously, causing thread starvation and request timeouts.

## Solution Architecture

### 1. **Database Connection Pooling** (Critical)
Increase HikariCP configuration in `application-prod.yml`:
```yaml
datasource:
  hikari:
    maximum-pool-size: 100        # Support concurrent load
    minimum-idle: 20              # Preemptively create connections
    connection-timeout: 30000     # More lenient under high load
    leak-detection-threshold: 60000
```

**Expected Impact:** Enables ~300 concurrent requests per instance.

### 2. **Materialized Analytics** (High Priority)
Current analytics queries are **synchronous and expensive** (200-500ms per request). Replace with pre-computed views:

- Create `cycle_analytics_summary` table with aggregated metrics
- Implement `@Scheduled` background task to refresh every 5 minutes
- Use native SQL `INSERT...ON CONFLICT` for atomic updates
- Convert O(n) aggregations to O(1) lookups

**Expected Impact:** Response time drops from 300ms → 10ms; eliminates 80% of query load.

### 3. **Distributed Caching** (High Priority)
Replace Caffeine with Redis for multi-instance consistency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**Why:** Each instance on Caffeine maintains its own cache, causing stale data. Redis centralizes the cache, ensuring consistency across 3-5 load-balanced instances.

### 4. **Rate Limiting**
Implement per-manager rate limiter (2 requests/sec) to prevent spike crashes:
- Use Google's Guava `RateLimiter` in a filter
- Extract manager ID from JWT token
- Return 429 (Too Many Requests) when exceeded

### 5. **Infrastructure Scaling**
- Deploy **3-5 application instances** behind an Nginx load balancer
- Each instance configured with 100 connections = 300-500 total concurrent capacity
- PostgreSQL with `max_connections=300` to support pool connections across instances
- Redis cluster for distributed cache invalidation

## Implementation Priority

| Phase | Task | Effort | Impact |
|-------|------|--------|--------|
| 1 | Increase HikariCP pool size | 5 min | 60% relief |
| 2 | Materialize analytics queries | 3 hours | 80% query speedup |
| 3 | Add Redis & rate limiting | 2 hours | Multi-instance ready |
| 4 | Deploy load balancer + 3 instances | 4 hours | Full 500 concurrent support |

## Expected Outcomes

- **Throughput:** From ~25 concurrent users → 500+ concurrent users
- **Latency:** Analytics from 300ms → 10ms
- **Resilience:** Rate limiting prevents cascade failures
- **Database load:** 80% reduction via query materialization

## Configuration Files to Update
- `application.yml` - HikariCP settings
- `pom.xml` - Add Redis dependency
- `AppConfig.java` - Implement Redis cache manager
- Create migration file for materialized view
- Create `AnalyticsRefreshService.java` for scheduled updates
