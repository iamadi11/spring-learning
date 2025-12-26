# Complete Observability Guide

## Overview

This guide covers the **complete observability stack** for the e-commerce microservices platform. Learn how to monitor, trace, log, and troubleshoot distributed systems like companies such as Netflix, Uber, and Google.

## 🎯 The Three Pillars of Observability

### 1. Metrics (Prometheus + Grafana)
**What**: Numerical measurements over time
**Purpose**: Performance monitoring, alerting
**Tools**: Prometheus (collection), Grafana (visualization)

### 2. Traces (Zipkin)
**What**: Request flow across services
**Purpose**: Latency analysis, bottleneck identification
**Tools**: Zipkin, Spring Cloud Sleuth

### 3. Logs (ELK Stack)
**What**: Textual events from applications
**Purpose**: Debugging, audit trails
**Tools**: Elasticsearch, Logstash, Kibana

## 📊 1. METRICS - Prometheus & Grafana

### What is Prometheus?

**Prometheus** is a time-series database for metrics collection.

**How it Works**:
```
1. Services expose metrics at /actuator/prometheus
2. Prometheus scrapes metrics every 15 seconds
3. Metrics stored in time-series database
4. Prometheus UI or Grafana queries for visualization
```

**Architecture**:
```
Microservices          Prometheus          Grafana
     │                     │                  │
     │ /actuator/         │                  │
     │  prometheus        │                  │
     ├────────────────────→ Scrape metrics   │
     │                     │                  │
     │                     │ Store in TSDB    │
     │                     │                  │
     │                     │←─────────────────┤
     │                     │  Query metrics   │
     │                     │                  │
     │                     │  Display charts──→
```

### Metrics Types

#### 1. Counter
**What**: Monotonically increasing value (only goes up)

**Examples**:
- Total HTTP requests
- Total errors
- Total orders processed

**Usage**:
```java
@Counted(value = "orders.created", description = "Total orders created")
public Order createOrder(OrderRequest request) {
    // ...
}
```

**Prometheus Query**:
```promql
# Total requests
http_server_requests_total{service="order-service"}

# Requests per second (rate over 5 minutes)
rate(http_server_requests_total[5m])
```

#### 2. Gauge
**What**: Value that can go up or down

**Examples**:
- Current memory usage
- Active database connections
- Number of items in queue

**Usage**:
```java
@Gauge(name = "db.connections.active", description = "Active DB connections")
public int getActiveConnections() {
    return dataSource.getNumActive();
}
```

**Prometheus Query**:
```promql
# Current JVM memory usage
jvm_memory_used_bytes{service="payment-service"}

# Memory usage percentage
(jvm_memory_used_bytes / jvm_memory_max_bytes) * 100
```

#### 3. Timer
**What**: Measures duration of events

**Examples**:
- HTTP request duration
- Database query time
- External API call latency

**Usage**:
```java
@Timed(value = "payment.process", description = "Payment processing time")
public Payment processPayment(PaymentRequest request) {
    // ...
}
```

**Prometheus Query**:
```promql
# Average response time
rate(http_server_requests_seconds_sum[5m]) / 
rate(http_server_requests_seconds_count[5m])

# 99th percentile (p99) response time
histogram_quantile(0.99, 
  rate(http_server_requests_seconds_bucket[5m]))

# 95th percentile (p95)
histogram_quantile(0.95, 
  rate(http_server_requests_seconds_bucket[5m]))
```

#### 4. Histogram
**What**: Samples observations and counts them in configurable buckets

**Examples**:
- Request size distribution
- Response time distribution

**Prometheus Query**:
```promql
# Request size distribution
http_server_requests_seconds_bucket{le="0.1"}  # <= 100ms
http_server_requests_seconds_bucket{le="0.5"}  # <= 500ms
http_server_requests_seconds_bucket{le="1.0"}  # <= 1s
```

### Key Metrics to Monitor

#### Application Metrics

**HTTP Requests**:
```promql
# Total requests per service
sum(rate(http_server_requests_total[5m])) by (service)

# Error rate (4xx + 5xx)
sum(rate(http_server_requests_total{status=~"4..|5.."}[5m])) 
  by (service)

# Success rate
sum(rate(http_server_requests_total{status=~"2.."}[5m])) 
  / sum(rate(http_server_requests_total[5m]))
```

**Response Times**:
```promql
# Average response time by endpoint
avg(rate(http_server_requests_seconds_sum[5m])) by (uri)

# Slow endpoints (p99 > 1s)
histogram_quantile(0.99, 
  rate(http_server_requests_seconds_bucket[5m])) > 1
```

**Circuit Breaker**:
```promql
# Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
resilience4j_circuitbreaker_state

# Failure rate
resilience4j_circuitbreaker_failure_rate

# Buffered calls
resilience4j_circuitbreaker_buffered_calls
```

**Database**:
```promql
# Active connections
hikaricp_connections_active

# Idle connections
hikaricp_connections_idle

# Connection wait time
hikaricp_connections_acquire_seconds
```

**JVM Metrics**:
```promql
# Memory usage
jvm_memory_used_bytes / jvm_memory_max_bytes * 100

# Garbage collection time
rate(jvm_gc_pause_seconds_sum[5m])

# Thread count
jvm_threads_live_threads
```

### Grafana Dashboards

#### Dashboard 1: Service Overview

**Panels**:
1. **Request Rate**: `rate(http_server_requests_total[5m])`
2. **Error Rate**: `rate(http_server_requests_total{status=~"5.."}[5m])`
3. **Response Time (p95)**: `histogram_quantile(0.95, ...)`
4. **Active Requests**: `http_server_requests_active`

#### Dashboard 2: Circuit Breaker Monitoring

**Panels**:
1. **Circuit State**: Show CLOSED/OPEN/HALF_OPEN
2. **Failure Rate**: Track failures
3. **Call Duration**: Track call times
4. **Buffered Calls**: Queue depth

#### Dashboard 3: Database Monitoring

**Panels**:
1. **Connection Pool**: Active vs Idle
2. **Query Time**: Slow queries
3. **Deadlocks**: Track deadlocks
4. **Transaction Rate**: TPS

### Alerts in Prometheus

**Alert Rules** (`/etc/prometheus/alerts.yml`):

```yaml
groups:
  - name: application_alerts
    interval: 30s
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_total{status=~"5.."}[5m])) 
          / sum(rate(http_server_requests_total[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }}% for {{ $labels.service }}"
      
      # Slow response time
      - alert: SlowResponseTime
        expr: |
          histogram_quantile(0.99, 
            rate(http_server_requests_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow response time"
          description: "P99 latency is {{ $value }}s"
      
      # Circuit breaker open
      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state == 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Circuit breaker is open"
          description: "Circuit {{ $labels.name }} is open"
      
      # High memory usage
      - alert: HighMemoryUsage
        expr: |
          (jvm_memory_used_bytes / jvm_memory_max_bytes) * 100 > 90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
          description: "Memory usage is {{ $value }}%"
```

## 🔍 2. DISTRIBUTED TRACING - Zipkin

### What is Distributed Tracing?

**Problem**: In microservices, a single user request touches multiple services. How do you track it?

**Solution**: Distributed tracing assigns a unique Trace ID to each request and tracks it across all services.

### How It Works

```
User Request arrives
  ↓
API Gateway (Trace-ID: abc123, Span-ID: 1)
  ↓
Order Service (Trace-ID: abc123, Span-ID: 2)
  ↓
Payment Service (Trace-ID: abc123, Span-ID: 3)
  ↓
Notification Service (Trace-ID: abc123, Span-ID: 4)

All logs/metrics tagged with Trace-ID: abc123!
```

### Key Concepts

#### 1. Trace
**What**: Complete journey of a request through the system

**Example**:
```
Trace ID: abc123
Total Duration: 500ms
Services: API Gateway → Order → Payment → Notification
```

#### 2. Span
**What**: Single operation within a trace

**Example**:
```
Span ID: span-1
Name: POST /api/orders
Duration: 100ms
Service: Order Service
Parent: API Gateway span
```

#### 3. Tags
**What**: Metadata about spans

**Examples**:
```
http.method: POST
http.url: /api/orders
http.status_code: 201
service.name: order-service
error: false
```

### Spring Cloud Sleuth Integration

**Auto-Configuration**:
```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0  # Sample 100% of requests (dev)
  zipkin:
    base-url: http://localhost:9411
    sender:
      type: web
```

**Automatic Features**:
- ✅ Trace ID generation
- ✅ Span ID generation
- ✅ Propagation via HTTP headers
- ✅ Logging integration
- ✅ Zipkin reporting

**Log Output**:
```
INFO [order-service,abc123,span-2] - Processing order for user 789
INFO [payment-service,abc123,span-3] - Processing payment $99.99
INFO [notification-service,abc123,span-4] - Sending notification
```

Format: `[service-name, trace-id, span-id]`

### Zipkin UI Features

#### 1. Search Traces
- Filter by service name
- Filter by duration
- Filter by tags
- Filter by time range

#### 2. Trace Visualization
```
Timeline View:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ API Gateway (500ms)
  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ Order Service (400ms)
    ━━━━━━━━━━━━━━━━━━━━ Payment Service (200ms)
      ━━━━ Notification Service (50ms)
```

Shows:
- Service call order
- Time spent in each service
- Parallel vs sequential calls
- Bottlenecks (longest spans)

#### 3. Dependencies
Visual graph showing:
- Which services call which
- Call volume between services
- Error rates between services

### Use Cases

**1. Performance Debugging**:
```
Problem: /api/orders endpoint is slow
Solution: 
1. Find slow traces in Zipkin
2. See Payment Service taking 5s
3. Drill into Payment spans
4. Find database query taking 4.9s
5. Optimize query
```

**2. Error Tracing**:
```
Problem: User reports order failed
Solution:
1. Search Zipkin for Trace ID from logs
2. See entire request flow
3. Find Payment Service returned 500
4. See error in Payment Service logs
5. Fix payment gateway integration
```

**3. Dependency Analysis**:
```
Question: Which services depend on User Service?
Solution:
1. Open Zipkin Dependencies view
2. See: Order → User, Review → User, Notification → User
3. Now know impact of User Service downtime
```

## 📝 3. CENTRALIZED LOGGING - ELK Stack

### What is ELK?

**E**lasticsearch: Store and index logs
**L**ogstash: Process and enrich logs
**K**ibana: Visualize and search logs

### Architecture

```
Microservices → Logstash → Elasticsearch → Kibana
                   ↓            ↓             ↓
              Process logs  Store logs   Search/View
```

### How Logs Flow

```
1. Service logs to console/file
   LOG: 2024-01-01 12:00:00 [order-service] Order created: 123

2. Logstash receives logs (TCP port 5000)
   - Parses JSON
   - Extracts fields
   - Adds metadata

3. Logstash sends to Elasticsearch
   - Indexed by service, level, timestamp
   - Stored for searching

4. Kibana queries Elasticsearch
   - Search logs
   - Filter by service/level/time
   - Create visualizations
```

### Log Format

**Structured JSON Logging** (Logback):

```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "service": "order-service",
  "trace_id": "abc123",
  "span_id": "span-2",
  "thread": "http-nio-8089-exec-1",
  "logger": "com.ecommerce.order.service.OrderService",
  "message": "Order created successfully",
  "user_id": 789,
  "order_id": "ord-123",
  "amount": 99.99
}
```

**Benefits**:
- ✅ Easy to parse
- ✅ Easy to search
- ✅ Easy to filter
- ✅ Structured data

### Kibana Features

#### 1. Discover (Log Search)

**Basic Search**:
```
# All logs from order-service
service:"order-service"

# Error logs
level:"ERROR"

# Logs for specific trace
trace_id:"abc123"

# Logs with keyword
message:"payment failed"

# Combined filters
service:"payment-service" AND level:"ERROR" 
  AND timestamp:[now-1h TO now]
```

**Advanced Search** (Lucene Query):
```
# Range query
amount:[100 TO 500]

# Wildcard
message:*timeout*

# Boolean operators
(service:"order-service" OR service:"payment-service") 
  AND level:"ERROR"

# Field exists
_exists_:error_message
```

#### 2. Visualizations

**Common Visualizations**:

**1. Log Count Over Time** (Line chart):
- X-axis: Timestamp
- Y-axis: Count of logs
- Split by: Service or Level

**2. Error Rate** (Area chart):
- Filter: level="ERROR"
- Aggregation: Count per minute
- Alert if > threshold

**3. Top Error Messages** (Table):
- Aggregation: Count by error_message
- Sort by: Count descending
- Limit: Top 10

**4. Service Log Distribution** (Pie chart):
- Aggregation: Count by service
- Shows which service logs most

#### 3. Dashboards

**Operations Dashboard**:
- Total logs per minute
- Error rate per service
- Top error messages
- Slow requests (duration > 1s)
- Failed payments
- User activity heatmap

#### 4. Alerting

**Alert Rules**:
```
Alert: High Error Rate
Condition: Error count > 100 in last 5 minutes
Action: Send email, Slack notification

Alert: Payment Failures
Condition: payment_status:"FAILED" count > 10
Action: Page on-call engineer

Alert: Disk Space Low
Condition: disk_free_percent < 10
Action: Send alert
```

### Log Correlation

**Correlate logs across services using Trace ID**:

```
Kibana Search: trace_id:"abc123"

Results:
1. [API Gateway] Request received /api/orders
2. [Order Service] Creating order for user 789
3. [Order Service] Calling payment service
4. [Payment Service] Processing payment $99.99
5. [Payment Service] Payment successful
6. [Order Service] Order created successfully
7. [Notification Service] Sending confirmation email
8. [Notification Service] Email sent

Complete request flow in one view!
```

## 🎯 Observability Best Practices

### 1. Naming Conventions

**Metrics**:
```
# Pattern: service.component.action.metric_type
order.controller.create.count
payment.circuit_breaker.calls.total
user.database.query.duration
```

**Logs**:
```
# Include context
log.info("Order created", 
  "user_id", userId, 
  "order_id", orderId, 
  "amount", amount);

# Not just
log.info("Order created");
```

### 2. What to Monitor

**Golden Signals** (Google SRE):
1. **Latency**: How long requests take
2. **Traffic**: How many requests
3. **Errors**: How many fail
4. **Saturation**: How full the system is

**RED Method** (for services):
1. **Rate**: Requests per second
2. **Errors**: Error rate
3. **Duration**: Response time distribution

**USE Method** (for resources):
1. **Utilization**: % time resource busy
2. **Saturation**: Queue depth
3. **Errors**: Error count

### 3. Alert Fatigue Prevention

**Good Alerts**:
- ✅ Actionable (can fix)
- ✅ Important (affects users)
- ✅ Infrequent (not noisy)
- ✅ Clear (what's wrong)

**Bad Alerts**:
- ❌ Can't be fixed
- ❌ False positives
- ❌ Too frequent
- ❌ Vague messages

### 4. Dashboard Design

**Principles**:
1. **Most Important First**: Key metrics at top
2. **Logical Grouping**: Group related metrics
3. **Consistent Colors**: Red=bad, Green=good
4. **Right Granularity**: Match to use case
5. **Annotations**: Mark deployments, incidents

## 🚀 Getting Started

### 1. Start Observability Stack

```bash
# Start all observability tools
docker-compose up -d prometheus grafana zipkin \
  elasticsearch logstash kibana
```

### 2. Access UIs

```bash
# Prometheus
open http://localhost:9090

# Grafana (admin/admin)
open http://localhost:3000

# Zipkin
open http://localhost:9411

# Kibana
open http://localhost:5601

# Elasticsearch
curl http://localhost:9200
```

### 3. Verify Metrics

```bash
# Check service exposes metrics
curl http://localhost:8089/actuator/prometheus

# Check Prometheus scraping
# Go to Prometheus → Status → Targets
# All services should be "UP"
```

### 4. Generate Sample Data

```bash
# Create some orders (generates metrics, traces, logs)
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{"userId":789,"items":[...]}'
done
```

### 5. Explore Data

**Prometheus**:
1. Go to Graph tab
2. Enter: `http_server_requests_total`
3. Click Execute
4. See request metrics

**Zipkin**:
1. Click "Run Query"
2. See list of traces
3. Click a trace
4. See timeline visualization

**Kibana**:
1. Go to Discover
2. Select index pattern: `logs-*`
3. Search: `service:"order-service"`
4. See logs

## 📊 Sample Queries

### Prometheus Queries

```promql
# Request rate by service
sum(rate(http_server_requests_total[5m])) by (service)

# Error percentage
(sum(rate(http_server_requests_total{status=~"5.."}[5m])) 
  / sum(rate(http_server_requests_total[5m]))) * 100

# P99 latency
histogram_quantile(0.99, 
  rate(http_server_requests_seconds_bucket[5m]))

# Database connections utilization
(hikaricp_connections_active / hikaricp_connections_max) * 100

# Circuit breaker failures
sum(resilience4j_circuitbreaker_calls_total{kind="failed"}) 
  by (name)
```

### Kibana Queries

```
# All errors in last hour
level:"ERROR" AND timestamp:[now-1h TO now]

# Failed payments
service:"payment-service" AND payment_status:"FAILED"

# Slow requests (> 1s)
duration:>1000

# Specific user activity
user_id:789

# Correlated by trace
trace_id:"abc123"
```

## 📚 Advanced Topics

### 1. Custom Metrics

```java
@Component
public class OrderMetrics {
    private final Counter ordersCreated;
    private final Gauge activeOrders;
    
    public OrderMetrics(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("type", "custom")
            .register(registry);
            
        this.activeOrders = Gauge.builder("orders.active", 
                this::getActiveOrderCount)
            .description("Current active orders")
            .register(registry);
    }
}
```

### 2. Distributed Tracing Best Practices

- **Sampling**: In production, sample 1-10% of traces (not 100%)
- **Baggage**: Pass additional context in trace headers
- **Custom Spans**: Add spans for important operations
- **Error Tagging**: Tag spans with error details

### 3. Log Enrichment

- **Add MDC**: Put user_id, trace_id in Mapped Diagnostic Context
- **Structured**: Use JSON format
- **Contextual**: Include relevant business data
- **Sensitive Data**: Mask passwords, credit cards

## 🎓 Key Takeaways

1. **Metrics**: Performance trends, alerting
2. **Traces**: Request flow, bottlenecks
3. **Logs**: Detailed debugging, audit
4. **Together**: Complete system observability
5. **Monitor**: Golden Signals (Latency, Traffic, Errors, Saturation)
6. **Alert**: On symptoms, not causes
7. **Dashboard**: Show what matters
8. **Correlate**: Use Trace ID across metrics, traces, logs

---

**Congratulations!** You now have production-grade observability covering metrics, distributed tracing, and centralized logging! 🎉

