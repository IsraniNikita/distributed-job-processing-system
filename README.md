# Distributed Job Processing System

Production-style distributed asynchronous job processing system built using Java 17, Spring Boot, MySQL, and Redis.

Supports:

- asynchronous background processing
- concurrent worker execution
- retry orchestration
- dead letter queue (DLQ)
- stuck job recovery
- idempotency
- transactional outbox pattern
- distributed scheduler coordination
- priority queues
- observability and metrics

---

## Project Overview

This project simulates how real-world backend systems process asynchronous tasks reliably at scale.

Instead of processing requests synchronously inside HTTP threads, jobs are:

- persisted in MySQL
- published to Redis queues
- consumed by background workers
- retried automatically on failures
- moved to DLQ after retry exhaustion
- recovered if workers crash mid-processing



The system focuses heavily on:

- fault tolerance
- concurrency control
- distributed coordination
- reliability engineering
- eventual consistency
- worker lifecycle management

---

## Tech Stack

- Java 17: Core language
- Spring Boot: Backend framework
- Spring Data JPA: Database access
- MySQL: Persistent job storage
- Redis	Queue: infrastructure
- ExecutorService: Concurrent worker pool
- Scheduled Tasks: Retry/recovery orchestration
- Maven: Dependency management

---

## High-Level Architecture

```
Client
   ↓
REST API
   ↓
MySQL Transaction
   ↓
Transactional Outbox
   ↓
Redis Queue
   ↓
Concurrent Worker Pool
   ↓
Job Execution
   ↓
Retry / DLQ / Recovery
```

---

## Architecture Diagram
                    ┌────────────────────┐
                    │       Client       │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │    REST API        │
                    │ Spring Boot        │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │      MySQL         │
                    │  Jobs + Outbox     │
                    └─────────┬──────────┘
                              │
                Outbox Publisher Scheduler
                              │
                              ▼
                    ┌────────────────────┐
                    │       Redis        │
                    │ Priority Queues    │
                    └─────────┬──────────┘
                              │
                              ▼
              ┌─────────────────────────────┐
              │ Concurrent Worker Pool      │
              │ ExecutorService Threads     │
              └──────────┬──────────────────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
      Worker-1       Worker-2       Worker-3


---

## Core Features
1. Asynchronous Job Processing

- HTTP requests return immediately after job submission.

- Workers process jobs in background threads.

  Why?

  Prevents:

  - request blocking
  - timeout issues
  - thread exhaustion
  - slow APIs
2. Concurrent Worker Pool

   Uses:

- Executors.newFixedThreadPool(5)

- Multiple workers process jobs simultaneously.

  Why?

  Improves:

  - throughput
  - scalability
  - resource utilization
  - Problem Solved

  Without worker pools:

  - unlimited thread creation
  - memory pressure
  - CPU context switching
3. Retry System with Exponential Backoff

- Failed jobs retry automatically.

  ``` 
  Example:

  Retry 1 → 2s
  Retry 2 → 4s
  Retry 3 → 8s

  ```
  Why?

  Most distributed failures are temporary:

  - network issues
  - API timeouts
  - SMTP failures
  - Problem Solved

- Prevents transient failures from becoming permanent data loss.

### Retry Flow
```
PROCESSING
     ↓
FAILED
     ↓
RETRY_SCHEDULED
     ↓
QUEUED
     ↓
PROCESSING AGAIN
```
4. Dead Letter Queue (DLQ)

- After max retries:

   DEAD_LETTER

   Why?

   Prevents:

   - infinite retry loops
   - CPU waste
   - queue flooding
   - Real Systems

   Used heavily in:

  - Kafka
  - RabbitMQ
  - SQS
  - BullMQ
  - Celery
5. Stuck Job Recovery

- Detects jobs stuck in: PROCESSING

  due to:

  - worker crash
  - JVM termination
  - deployment interruption


### Recovery Flow
```
Worker Crash
      ↓
Timeout Exceeded
      ↓
Recovery Scheduler
      ↓
Requeue Job
```
Problem Solved

- Ensures abandoned jobs are eventually reprocessed.

6. Idempotency Protection

- Duplicate requests are prevented using: idempotencyKey

  Why?

  Prevents:

  - duplicate payments
  - duplicate emails
  - duplicate processing
  - Real Systems

  Critical in:

  - Stripe
  - Razorpay
  - PayPal
7. Priority Queue System

- Separate queues:

  high_priority_queue

  low_priority_queue

- Workers always prioritize HIGH jobs first.

  Why?

  - Critical tasks should not wait behind low-priority work.

8. Transactional Outbox Pattern

Solves:

- DB write succeeds
- Redis publish fails

Problem

- Classic distributed systems issue:

- Dual Write Problem

Solution

- Store events in:

- outbox table

- Then background publisher safely pushes to Redis.

  Why?

  - Ensures reliable event delivery.

  Used By
  - Uber
  - Airbnb
  - Shopify
  - fintech systems


### Outbox Flow
```
DB Transaction
      ↓
Save Job
      ↓
Save Outbox Event
      ↓
Commit
      ↓
Outbox Publisher
      ↓
Redis Queue
```

---

## Distributed Coordination

- Multiple application instances can run simultaneously.

- Schedulers use Redis distributed locking to prevent:

  - duplicate retry orchestration
  - duplicate recovery scans
  - duplicate outbox publishing

  Why?

  - Without coordination:
multiple schedulers may process same orchestration task simultaneously.

---

## Database Schema
jobs table:
- id: Job identifier
- type: Job type
- status: Current lifecycle state
- payload: Job data
- retry_count: Retry tracking
- max_retries: Retry limit
- next_retry_at: Delayed retry scheduling
- processing_started_at: Crash recovery
- processing_duration_ms: Metrics
- priority: Queue routing
- idempotency_key: Duplicate prevention
- correlation_id: Distributed tracing
- version: Optimistic locking
- created_at: Auditing
- updated_at: Auditing

---

## Job Lifecycle State Machine
```
QUEUED
   ↓
PROCESSING
   ↓
SUCCESS

OR

PROCESSING
   ↓
FAILED
   ↓
RETRY_SCHEDULED
   ↓
QUEUED

OR

FAILED
   ↓
DEAD_LETTER
```

---

## Concurrency Handling
- Optimistic Locking

  Uses:

  @Version

  Why?

  Prevents:

  - lost updates
  - silent overwrites
  - race condition corruption
- Atomic Job Claiming

  Workers safely claim jobs using conditional updates:
```
UPDATE jobs
SET status='PROCESSING'
WHERE id=? AND status='QUEUED'
```

Why?

Prevents multiple workers processing same job simultaneously.

---

## Observability

System includes:

- structured logging
- execution duration tracking
- metrics APIs
- correlation IDs

Metrics API
```
GET /metrics/jobs
```

Returns:

- queued jobs
- processing jobs
- successful jobs
- failed jobs

---

## API Examples

Create Job
```
POST /jobs
```

Request:
```
{
  "type": "EMAIL",
  "payload": "{\"to\":\"test@gmail.com\"}",
  "priority": "HIGH"
}
```
Get Job Status
```
GET /jobs/{id}
```

Retry Job
```
POST /jobs/{id}/retry
```

Metrics
```
GET /metrics/jobs
```

---

## Failure Scenarios Handled
- Worker crash: Recovery scheduler
- Temporary API failure: Retry system
- Permanent failure: DLQ
- Duplicate request:Idempotency
- Race condition: Optimistic locking
- Dual write inconsistency: Transactional outbox
- Multi-instance scheduler duplication: Redis distributed lock

--- 

## Scalability Considerations

Current system supports:

- concurrent worker scaling
- horizontal application scaling
- distributed orchestration
- eventual consistency

Potential future improvements:

- Kafka migration
- Kubernetes autoscaling
- Prometheus metrics
-OpenTelemetry tracing
- Virtual threads
- Circuit breakers
-Reactive workers

---

## Performance Bottlenecks

Current limitations:

- DB polling schedulers
- Redis list queue limitations
- single database bottleneck
- blocking worker model

Possible optimizations:

- Kafka/RabbitMQ
- partitioned queues
- Redis Streams
- reactive execution
- batching
- sharded workers

---

## Security Considerations

Important production concerns:

- API authentication/authorization
- payload validation
- queue poisoning prevention
- rate limiting
- secrets management
- SQL injection prevention
- audit logging

---

## How To Run

1. Create Database
```
CREATE DATABASE job_system;
```
2. Start Redis
```
redis-server
```

Verify:
```
redis-cli ping
```

Expected:
```
PONG
```
3. Configure application.yml
```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/job_system
    username: root
    password: password

  jpa:
    hibernate:
      ddl-auto: update

  data:
    redis:
      host: localhost
      port: 6379
```
4. Start Application
```
mvn spring-boot:run
```

---

## Project Structure
```
distributed-job-system/
│
├── src/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── metrics/
│   ├── outbox/
│   ├── repository/
│   ├── scheduler/
│   ├── service/
│   └── worker/
│
├── docs/
├── application-example.yml
├── pom.xml
└── .gitignore
```

---

## Distributed Systems Concepts Implemented
- asynchronous processing
- worker pools
- retries
- exponential backoff
- dead letter queues
- optimistic locking
- idempotency
- transactional outbox
- fault tolerance
- crash recovery
- distributed locking
- eventual consistency
- priority scheduling

---

## Key Engineering Learnings

This project focuses heavily on:

- distributed systems thinking
- coordination of unreliable components
- concurrency safety
- failure recovery
- eventual consistency
- scalable async processing

The primary complexity was not framework syntax, but safely coordinating:

- workers
- retries
- crashes
- queues
- distributed state transitions
