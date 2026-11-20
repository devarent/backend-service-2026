# Backend-Service-2026
<sub><span style="opacity: 0.85">Java 21 + Quarkus order service: PostgreSQL, Flyway, Redis, OpenAPI, integration tests, Docker and Kubernetes.</span></sub>
<div align="center" style="font-size: 0.9em">
<img src="assets/brand-logo.svg" alt="Arya Fatthurahman" width="130px" />
</div>

[![CI](https://github.com/devarent/backend-service-2026/actions/workflows/ci.yml/badge.svg)](https://github.com/devarent/backend-service-2026/actions/workflows/ci.yml)

---


<!-- identity-block -->
<div align="center">

# backend-service-2026

<sub>Java 21 + Quarkus order service: PostgreSQL, Flyway, Redis, OpenAPI, integration tests, Docker and Kubernetes.</sub>

</div>

## At a glance

| Field | Detail |
| --- | --- |
| Repository | backend-service-2026 |
| Primary language | Java |
| Topics | docker, flyway, java, kubernetes, maven, microservices, postgresql, quarkus, redis, rest-api |
| License | MIT |
| Branch | main |
| Maintainer | [@devarent](https://github.com/devarent) |

## Overview

backend-service-2026 is a hands-on repository focused on: Java 21 + Quarkus order service: PostgreSQL, Flyway, Redis, OpenAPI, integration tests, Docker and Kubernetes.. Primary language: Java. Topics: docker, flyway, java, kubernetes, maven, microservices, postgresql, quarkus, redis, rest-api. The README below documents the project; use the table above for a quick overview.

### Global references

<div align="center">

🇮🇸 Iceland &middot; 🇳🇿 New Zealand &middot; 🇨🇭 Switzerland &middot; 🇸🇮 Slovenia &middot; 🇮🇪 Ireland &middot; 🇦🇹 Austria &middot; 🇵🇹 Portugal &middot; 🇸🇬 Singapore &middot; 🇫🇮 Finland

</div>

---

<!-- dbx-style-kit -->
<div align="center">

<img src="https://img.shields.io/badge/backend-service-2026-1f6feb?style=for-the-badge" alt="backend-service-2026" />
<img src="https://img.shields.io/badge/LANG%20Java-3178c6?style=for-the-badge" alt="LANG Java" />
<img src="https://img.shields.io/badge/STARS%200-eab308?style=for-the-badge" alt="STARS 0" />
<img src="https://img.shields.io/badge/LICENSE%20MIT-22c55e?style=for-the-badge" alt="LICENSE MIT" />

<br>

<img src="https://img.shields.io/badge/OPEN%20SOURCE-yes-22c55e?style=for-the-badge&logo=github&logoColor=white" alt="OPEN SOURCE" />
<img src="https://img.shields.io/badge/BUILT%20IN%20PUBLIC-true-6366f1?style=for-the-badge&logo=opensourceinitiative&logoColor=white" alt="BUILT IN PUBLIC" />
<img src="https://img.shields.io/badge/HANDS%20ON%20LABS-78-0ea5e9?style=for-the-badge&logo=docker&logoColor=white" alt="HANDS ON LABS" />
<img src="https://img.shields.io/badge/DOCUMENTED-yes-14b8a6?style=for-the-badge&logo=markdown&logoColor=white" alt="DOCUMENTED" />

<br>

<b>Java 21 + Quarkus order service: PostgreSQL, Flyway, Redis, OpenAPI, integration tests, Docker and Kubernetes.</b>

</div>

---
# Order Service 2026

![CI](https://github.com/aryafatthurahman/backend-service-2026/actions/workflows/ci.yml/badge.svg)
![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Quarkus](https://img.shields.io/badge/Quarkus-3.15.1-purple.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED.svg)

A production-style order-management REST API built with **Java 21** and
**Quarkus 3.15**. It is deliberately small enough to review in one sitting, but
it demonstrates the parts of a real backend that matter: transactional business
rules, a documented state machine, idempotent writes, optimistic locking, Flyway
migrations, a consistent error contract, correlation ids, metrics, OpenAPI, a
Redis cache with graceful fallback, container images, Kubernetes manifests, and
a CI pipeline with a vulnerability scan.

The service is single-currency by design (USD by default) so revenue reporting
stays coherent. No credentials, generated secrets, or real customer data are
committed.

## Contents

- [What this demonstrates](#what-this-demonstrates)
- [Architecture](#architecture)
- [Order lifecycle](#order-lifecycle)
- [API](#api)
- [Quickstart](#quickstart)
- [Configuration](#configuration)
- [Testing](#testing)
- [Data model](#data-model)
- [Error handling](#error-handling)
- [Observability](#observability)
- [Security](#security)
- [Project layout](#project-layout)
- [Container and Kubernetes](#container-and-kubernetes)
- [CI and release](#ci-and-release)
- [Design decisions](#design-decisions)
- [Documentation](#documentation)
- [License](#license)

## What this demonstrates

| Capability | Where it appears |
| --- | --- |
| **Java 21 / Quarkus 3** | Records, pattern matching, constructor injection, Quarkus REST, Panache, build-time DI validation |
| **REST API design** | Versioned `/api/v1/orders` contract, pagination, filtering, OpenAPI, idempotency, `Location` headers |
| **Microservice architecture** | Stateless API, health probes, metrics, bounded resources, horizontal scaling, container image |
| **PostgreSQL and SQL** | Flyway schema, foreign keys, check constraints, indexes, JPQL `GROUP BY` revenue projection |
| **Redis / NoSQL** | Short-lived revenue cache with a bounded in-process fallback when Redis is unavailable |
| **Kafka / NATS** | Documented transactional-outbox extension point; the core workflow does not require a broker |
| **OOP and design patterns** | Aggregate root, repository, mapper, state machine, immutable DTOs, layered packages |
| **Testing** | JUnit 5, Mockito, REST Assured, H2 PostgreSQL mode; pure domain tests plus end-to-end HTTP tests |
| **Docker and Kubernetes** | Multi-stage non-root image, Compose stack, Kustomize base/overlays, probes, HPA, PDB, NetworkPolicy, ServiceMonitor |
| **CI/CD and cloud** | GitHub Actions with Temurin 21, Maven cache, Trivy scan, non-pushing image build, optional GHCR release |
| **Git and collaboration** | Conventional commits, `CONTRIBUTING.md`, `SECURITY.md`, ADR, issue-ready documentation |
| **AI-assisted development** | Small reviewable modules, deterministic tests, and an ADR make generated changes easy to verify |

## Architecture

```mermaid
flowchart LR
    Client["Web / mobile client"] -->|"HTTPS + JSON"| Ingress["Ingress / API gateway"]
    Ingress --> API["Quarkus REST API<br/>Java 21"]

    subgraph Service["order-service"]
        API --> Filter["RequestIdFilter<br/>X-Request-Id"]
        Filter --> Resource["OrderResource<br/>validation + OpenAPI"]
        Resource --> OrderService["OrderService<br/>transactions + rules"]
        OrderService --> Repository["OrderRepository<br/>Panache + JPQL"]
        OrderService --> Mapper["OrderMapper<br/>entity to DTO"]
        OrderService --> Cache["RevenueCache"]
        OrderService --> Metrics["OrderMetrics<br/>Micrometer"]
    end

    Repository -->|"JDBC"| Postgres[("PostgreSQL 16")]
    Cache -->|"GET / SETEX"| Redis[("Redis 7")]
    Cache -.->|"fallback"| Memory[("In-process map")]
    Metrics --> Prometheus["Prometheus"]
    API -->|"/q/health"| Probes["Kubernetes probes"]

    Kafka["Kafka / NATS<br/>extension point"] -.->|"order events"| OrderService
```

The dashed Kafka/NATS box is intentional: no broker is required today. A
production extension would add a transactional outbox and publish
`OrderCreated` / `OrderStatusChanged` events after commit, keeping the
synchronous API independent of broker availability.

**Request flow**

1. The ingress terminates TLS and forwards the request.
2. `RequestIdFilter` accepts a safe `X-Request-Id` or generates a UUID, stores it
   in request scope, and echoes it on every response.
3. `OrderResource` deserialises and validates the payload with Jakarta Bean
   Validation.
4. `OrderService` opens a transaction, applies business rules, and delegates
   persistence to `OrderRepository`.
5. `OrderMapper` converts entities to immutable DTO records inside the
   transaction, so lazy collections are never serialised outside a session.
6. Failures are mapped to the standard `ErrorResponse` envelope with the same
   correlation id.
7. Micrometer records counters and HTTP/JVM metrics for Prometheus.

See [docs/architecture.md](docs/architecture.md) for layering rules, transaction
boundaries, failure modes, and scaling notes.

## Order lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> CONFIRMED
    PENDING --> CANCELLED
    CONFIRMED --> PROCESSING
    CONFIRMED --> CANCELLED
    PROCESSING --> SHIPPED
    PROCESSING --> CANCELLED
    SHIPPED --> DELIVERED
    DELIVERED --> [*]
    CANCELLED --> [*]
```

The transition table lives in `OrderStatus` and is the single source of truth.
`DELETE /api/v1/orders/{id}` is a **soft cancel**, not a row delete: PENDING,
CONFIRMED, and PROCESSING orders move to CANCELLED, while SHIPPED and DELIVERED
orders are rejected with `409 ORDER_NOT_CANCELLABLE`. Repeating a cancel is
safe. History and revenue records are retained.

## API

Base path: `/api/v1/orders`. Interactive docs: `/q/swagger-ui`; OpenAPI JSON:
`/q/openapi`.

| Method | Path | Purpose | Success | Main errors |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/orders` | Create an order; optional `Idempotency-Key` | `201`, `200` on replay | `400`, `409` |
| `GET` | `/api/v1/orders` | List summaries; `status`, `page`, `size` | `200` | `400` |
| `GET` | `/api/v1/orders/{id}` | Get one order by public UUID | `200` | `404` |
| `PATCH` | `/api/v1/orders/{id}/status` | Transition status | `200` | `409` |
| `DELETE` | `/api/v1/orders/{id}` | Soft cancel | `200` | `409` |
| `GET` | `/api/v1/orders/stats/revenue` | Revenue grouped by status | `200` | `500` |

Pagination is zero-based (`page=0`), `size` is 1-100, and `status` is an optional
`OrderStatus` value. All timestamps are ISO-8601 UTC instants.

### Create

```bash
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: order-request-0001' \
  -H 'X-Request-Id: demo-request-0001' \
  -d '{
    "customerId": "CUST-1001",
    "customerName": "Ada Lovelace",
    "customerEmail": "ada@example.com",
    "currency": "USD",
    "items": [
      {"sku": "SKU-KEYBOARD", "name": "Mechanical Keyboard", "quantity": 2, "unitPrice": 40.00},
      {"sku": "SKU-MOUSEPAD", "name": "Mouse Pad", "quantity": 1, "unitPrice": 20.00}
    ]
  }'
```

The response is `201 Created` with `Location: /api/v1/orders/{id}`, subtotal
`100.00`, tax `8.25` at the default 8.25% rate, and total `108.25`. A repeated
`Idempotency-Key` returns the original order with `200` and does not create a
second row. SKUs are normalised to uppercase and must be unique per order.

### List and get

```bash
curl 'http://localhost:8080/api/v1/orders?status=PENDING&page=0&size=20'
curl http://localhost:8080/api/v1/orders/7f5c9a2e-1b4d-4f1a-9c2e-8b7a6d5e4f3a
```

### Transition and cancel

```bash
curl -i -X PATCH http://localhost:8080/api/v1/orders/7f5c9a2e-1b4d-4f1a-9c2e-8b7a6d5e4f3a/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"CONFIRMED"}'

curl -i -X DELETE http://localhost:8080/api/v1/orders/7f5c9a2e-1b4d-4f1a-9c2e-8b7a6d5e4f3a
```

### Revenue

```bash
curl http://localhost:8080/api/v1/orders/stats/revenue
```

```json
{
  "currency": "USD",
  "totalRevenue": 162.38,
  "totalOrders": 2,
  "byStatus": [
    {"status": "CONFIRMED", "orderCount": 1, "revenue": 108.25},
    {"status": "PENDING", "orderCount": 1, "revenue": 54.13}
  ],
  "generatedAt": "2026-11-19T07:20:00Z"
}
```

Cancelled orders are excluded. The aggregate is one JPQL `GROUP BY` query and is
cached for 30 seconds by default. It may be up to the TTL stale and is not used
for billing. The full contract, including the error envelope and every field, is
in [docs/api.md](docs/api.md).

## Quickstart

### Prerequisites

- JDK 21 (Temurin is the tested distribution)
- Docker with Compose v2 for the full stack; optional for the default test suite
- No system Maven required: the wrapper pins Maven 3.9.9

### Full local stack

```bash
docker compose up --build -d
curl -fsS http://localhost:8080/q/health/ready
```

The API is at `http://localhost:8080`, PostgreSQL at `localhost:5432`, and Redis
at `localhost:6379`. The app waits for both dependencies to become healthy.
Copy `.env.example` to `.env` only if you need to change the local defaults.

### Dev mode with containerised databases

```bash
docker compose up -d postgres redis
./mvnw quarkus:dev
```

On Windows PowerShell:

```powershell
docker compose up -d postgres redis
.\mvnw.cmd quarkus:dev
```

The `%dev` profile disables Redis and uses the in-process revenue cache, so Redis
is optional during development. Hot reload and the Dev UI are available at
`http://localhost:8080/q/dev-ui`.

### Packaged JVM application

```bash
./mvnw -B -DskipTests package
java -jar target/quarkus-app/quarkus-run.jar
```

Set `DB_URL`, `DB_USER`, and `DB_PASSWORD` first. The production profile requires
them explicitly and never falls back to the local defaults.

### Smoke test

```bash
curl -fsS http://localhost:8080/q/health/live
curl -fsS http://localhost:8080/q/health/ready
curl -fsS http://localhost:8080/q/openapi | head -c 200
```

## Configuration

Configuration lives in `src/main/resources/application.properties`. The
`%dev`, `%test`, and `%prod` profiles override the defaults, and Quarkus maps
each property to an environment variable using the usual naming rules.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/orders` | JDBC URL |
| `DB_USER` | `orders` | Database user |
| `DB_PASSWORD` | `orders` | Database password; replace everywhere outside local development |
| `DB_POOL_MAX_SIZE` | `20` | Maximum JDBC pool size per pod |
| `REDIS_URL` | `redis://localhost:6379` | Redis connection URL |
| `REDIS_PASSWORD` | empty | Redis password, if required |
| `APP_CACHE_ENABLED` | `true` | Enable the revenue cache |
| `APP_CACHE_REDIS_ENABLED` | `true` | Use Redis, with local fallback on failure |
| `APP_CACHE_TTL` | `30s` | Revenue cache time to live |
| `APP_PRICING_TAX_RATE` | `0.0825` | Tax rate in decimal form, 0 to 1 |
| `APP_ORDERS_CURRENCY` | `USD` | Only orders in this ISO 4217 code are accepted |
| `APP_ORDERS_MAX_ITEMS_PER_ORDER` | `100` | Maximum line items per order |

`%prod` requires `DB_URL`, `DB_USER`, and `DB_PASSWORD`; the committed defaults
are for local development only. The typed `AppConfig` mapping validates business
settings at startup. Never commit a real `.env` or secret manifest.

## Testing

The default build is hermetic: H2 in PostgreSQL compatibility mode, Flyway
disabled, Redis disabled, and an ephemeral HTTP test port. No Docker,
PostgreSQL, or Redis is required.

| Test | Type | What it proves |
| --- | --- | --- |
| `MoneyCalculatorTest` | Plain JUnit 5 | Rounding, scale, validation, subtotal, tax, total |
| `OrderStatusTest` | Plain JUnit 5 | Transition table, terminal states, null handling |
| `OrderTest` | Plain JUnit 5 | Aggregate invariants, totals, legal/illegal transitions |
| `OrderServiceTest` | JUnit 5 + Mockito | Create, idempotency, validation, pagination, conflicts, revenue |
| `OrderResourceTest` | `@QuarkusTest` + REST Assured | HTTP statuses, headers, JSON contract, error envelope, full flows |

```bash
# Everything CI runs
./mvnw -B verify

# Fast unit tests only
./mvnw -B -Dtest='MoneyCalculatorTest,OrderStatusTest,OrderTest,OrderServiceTest' test

# REST integration test only
./mvnw -B -Dtest=OrderResourceTest test
```

On Windows PowerShell replace `./mvnw` with `.\mvnw.cmd`. Reports are written to
`target/surefire-reports/` and CI uploads them even when the job fails. See
[docs/testing.md](docs/testing.md) for the test environment, deliberate gaps,
and guidance on adding tests.

## Data model

```mermaid
erDiagram
    ORDERS ||--o{ ORDER_ITEMS : contains

    ORDERS {
        bigint id PK
        uuid public_id UK
        varchar customer_id
        varchar customer_name
        varchar customer_email
        varchar status
        varchar currency
        numeric subtotal
        numeric tax_amount
        numeric total_amount
        varchar idempotency_key UK
        bigint version
        timestamptz created_at
        timestamptz updated_at
    }

    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        varchar product_sku
        varchar product_name
        integer quantity
        numeric unit_price
        numeric line_total
    }
```

- `orders` is the aggregate root; `order_items` is owned by it with cascade and
  orphan removal.
- `public_id` is the only identifier exposed by the API; the numeric primary key
  stays internal.
- `version` is Hibernate's optimistic-lock column. Concurrent updates surface as
  `409 CONCURRENT_MODIFICATION`.
- `idempotency_key` is nullable and unique; PostgreSQL permits multiple `NULL`
  values, so orders without a key coexist.
- All money is `NUMERIC(19,2)` in PostgreSQL and `BigDecimal` in Java. Line
  totals, tax, and totals are rounded `HALF_UP` to two decimals.
- Check constraints enforce non-negative amounts, valid statuses, positive
  quantities, and `total_amount = subtotal + tax_amount`.
- Indexes cover status/created-at pagination, newest-first listing, customer
  history, and item lookup.

Flyway owns schema changes (`V1__init.sql`); Hibernate `validate` checks the
mapping in production. See [docs/data-model.md](docs/data-model.md) for the full
column list, constraints, and rounding rules.

## Error handling

Every non-2xx response uses one envelope and carries the same correlation id as
the `X-Request-Id` header:

```json
{
  "timestamp": "2026-11-19T07:15:30.123Z",
  "status": 409,
  "error": "Conflict",
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Cannot transition order from PENDING to SHIPPED",
  "path": "/api/v1/orders/7f5c9a2e-1b4d-4f1a-9c2e-8b7a6d5e4f3a/status",
  "requestId": "5f1d4c0e-8a2b-4c3d-9e1f-0a2b3c4d5e6f",
  "violations": []
}
```

Validation failures populate `violations` with `field` and `message` entries.
Common codes include `VALIDATION_ERROR`, `MALFORMED_JSON`, `NOT_FOUND`,
`INVALID_STATUS_TRANSITION`, `ORDER_NOT_CANCELLABLE`,
`CONCURRENT_MODIFICATION`, `DUPLICATE_SKU`, `UNSUPPORTED_CURRENCY`,
`INVALID_IDEMPOTENCY_KEY`, `INVALID_PAGE`, `INVALID_PAGE_SIZE`, and
`INTERNAL_ERROR`. Unexpected exceptions log the request id and return a generic
message; stack traces and SQL details never reach the client.

## Observability

| Endpoint | Purpose |
| --- | --- |
| `/q/health/live` | Liveness and startup probes |
| `/q/health/ready` | Readiness, including datasource health |
| `/q/health` | Aggregate health with details |
| `/q/metrics` | Prometheus exposition format |
| `/q/openapi` | OpenAPI document |
| `/q/swagger-ui` | Interactive API explorer |

Key metrics include `http_server_requests_seconds`, `orders_created_total`,
`orders_cancelled_total`, `orders_status_transitions_total`, JVM memory/GC, and
Agroal connection-pool timings. The optional observability stack starts
Prometheus and Grafana:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d
```

Prometheus is at `http://localhost:9090`; Grafana is at `http://localhost:3000`
(local admin credentials from `.env`). The runbook in
[docs/operations.md](docs/operations.md) covers SLOs, alerts, rollback, secret
rotation, and incident scenarios.

## Security

- Bean Validation on every request body and query parameter.
- Parameterised JPQL only; no concatenated SQL.
- Uniform error responses with correlation ids and no internal details.
- Non-root container user (`10001`), read-only root filesystem in Kubernetes,
  dropped capabilities, `allowPrivilegeEscalation: false`, and
  `RuntimeDefault` seccomp.
- `NetworkPolicy` limits ingress to the API port and egress to DNS, PostgreSQL,
  Redis, and HTTPS.
- No secrets in Git. Kubernetes uses a Secret created out of band; the committed
  `secret.example.yaml` is a template and is not part of Kustomize.
- Trivy scans the filesystem in CI. The scan is non-blocking for this portfolio
  so a new advisory does not break unrelated pull requests.

**Deliberate limitation:** the demo has no authentication or authorization.
Production must add an OIDC resource server, per-customer authorization, TLS at
the edge, and rate limiting. See [SECURITY.md](SECURITY.md) for the full model
and reporting process.

## Project layout

```text
backend-service-2026/
|-- .github/workflows/
|   |-- ci.yml                     Build, test, Trivy scan, image build (no push)
|   `-- release.yml                Optional GHCR release on v*.*.* tags
|-- docs/
|   |-- adr/0001-quarkus-postgresql.md
|   |-- api.md
|   |-- architecture.md
|   |-- data-model.md
|   |-- operations.md
|   `-- testing.md
|-- kubernetes/
|   |-- base/                      Namespace, Deployment, Service, Ingress, HPA, PDB, NetworkPolicy, ServiceMonitor
|   `-- overlays/{dev,prod}/       Kustomize overlays
|-- observability/                 Prometheus config and Grafana provisioning
|-- src/main/java/com/aryafatthurahman/backend/
|   |-- cache/                     Redis with in-process fallback
|   |-- common/                    Error envelope, exceptions, page envelope
|   |-- config/                    Typed config, request context, request id filter
|   |-- order/
|   |   |-- api/                   REST resource and exception mappers
|   |   |-- dto/                   Request/response records with validation
|   |   |-- Order.java             Aggregate root, totals, transitions
|   |   |-- OrderItem.java         Line item with computed line total
|   |   |-- OrderService.java      Transactional business rules
|   |   |-- OrderRepository.java   Panache queries and JPQL aggregate
|   |   |-- OrderMapper.java       Entity to DTO mapping
|   |   |-- OrderStatus.java       State machine transition table
|   |   |-- OrderMetrics.java      Micrometer counters
|   |   `-- CreateOrderResult.java
|   `-- pricing/MoneyCalculator.java
|-- src/main/resources/
|   |-- application.properties
|   `-- db/migration/V1__init.sql
|-- src/test/java/                 Unit and integration tests
|-- src/test/resources/            H2, no Redis, no Flyway
|-- Dockerfile                     Multi-stage, non-root runtime
|-- docker-compose.yml             API + PostgreSQL + Redis
|-- docker-compose.observability.yml
|-- .env.example
|-- CONTRIBUTING.md
|-- SECURITY.md
|-- pom.xml
`-- README.md
```

## Container and Kubernetes

The `Dockerfile` builds with Maven 3.9.9 on JDK 21, copies the Quarkus fast-jar
layout into a slim JRE image, creates uid/gid `10001`, and runs as that user. It
includes a health check and a `JAVA_OPTS` entry point. `.dockerignore` keeps the
build context small.

```bash
docker build -t order-service:local .
docker compose up --build -d
```

Kubernetes manifests use Kustomize:

```bash
kubectl kustomize kubernetes/overlays/dev     # render, do not apply
kubectl kustomize kubernetes/overlays/prod

kubectl apply -k kubernetes/base              # namespace, service account, config
kubectl -n order-service create secret generic order-service-secrets \
  --from-literal=DB_PASSWORD='<strong-password>' \
  --from-literal=REDIS_PASSWORD='<redis-password>'
kubectl apply -k kubernetes/overlays/prod
kubectl -n order-service rollout status deployment/order-service --timeout=180s
```

The base includes a Deployment with startup/liveness/readiness probes, resource
requests and limits, a non-root security context, a Service, an Ingress, an HPA,
a PodDisruptionBudget, a Prometheus `ServiceMonitor`, and a port-scoped
`NetworkPolicy`. The `prod` overlay raises replicas and the disruption budget.
Before applying, replace the example host in `base/ingress.yaml`, the database
and Redis hostnames in `base/configmap.yaml`, and the image tag in
`overlays/prod/kustomization.yaml`. The manifests assume an nginx ingress
controller, cert-manager, the Prometheus Operator, and a CNI that enforces
NetworkPolicy. See [kubernetes/README.md](kubernetes/README.md) for details.

## CI and release

`.github/workflows/ci.yml` runs on pushes and pull requests:

1. **Build and test** — Temurin JDK 21, Maven cache, `./mvnw -B -ntp verify`,
   Surefire report upload.
2. **Trivy filesystem scan** — `CRITICAL,HIGH` severities, `ignore-unfixed`, and
   `exit-code: 0` (non-blocking in this portfolio).
3. **Container image** — Buildx build of the Dockerfile, tagged with the commit
   SHA, with `push: false`.

The optional `release.yml` runs on `v*.*.*` tags, verifies the project, and
publishes a multi-arch image to GHCR using `GITHUB_TOKEN`. It does not run on
ordinary branch pushes.

## Design decisions

| Decision | Rationale |
| --- | --- |
| **Quarkus 3.15** rather than Spring Boot | Fast startup, low memory, build-time DI validation, cohesive REST/Hibernate/Flyway/Redis/health extensions |
| **PostgreSQL as the system of record** | Orders need multi-row transactions, foreign keys, check constraints, and aggregate SQL |
| **Redis only for a short-lived cache** | Keeps the source of truth relational; a cache outage degrades latency, not availability |
| **H2 for the default tests** | The build stays hermetic and Docker-free; PostgreSQL/Flyway is exercised by Compose and is a documented next hardening step |
| **Soft cancel** | Preserves audit and revenue history; financial records should not disappear |
| **Single configured currency** | Revenue aggregation stays coherent without a rates table or multi-currency grouping |
| **`Idempotency-Key` on create** | Safe client retries without duplicate orders; the unique column prevents races |
| **`@Version` optimistic locking** | Detects concurrent writes and returns `409` instead of silently overwriting |
| **No Kafka/NATS dependency** | The core workflow does not need a broker; the outbox extension is documented rather than half-implemented |
| **No authentication in the demo** | Keeps local review friction low; OIDC and authorization are explicit production follow-ups |

Trade-offs and alternatives are recorded in
[docs/adr/0001-quarkus-postgresql.md](docs/adr/0001-quarkus-postgresql.md).
The most important caveat: H2 is not PostgreSQL, so the default test suite does
not execute the Flyway migration. A Testcontainers PostgreSQL profile is the
natural next step for CI.

## Documentation

- [Architecture](docs/architecture.md) — components, layering, transactions,
  caching, failure modes, scaling, extension points.
- [API contract](docs/api.md) — every endpoint, headers, payloads, status codes,
  and error envelope.
- [Data model](docs/data-model.md) — ER diagram, columns, constraints, indexes,
  state machine, rounding, migration policy.
- [Testing](docs/testing.md) — test pyramid, commands, environment, coverage,
  deliberate gaps.
- [Operations](docs/operations.md) — SLOs, health, deploy, rollback, runbooks,
  backup, secret rotation, on-call checklist.
- [ADR 0001](docs/adr/0001-quarkus-postgresql.md) — why Quarkus, PostgreSQL,
  and Redis, and what was deliberately left out.
- [Kubernetes guide](kubernetes/README.md) — render, deploy, and production
  prerequisites.
- [Contributing](CONTRIBUTING.md) and [Security](SECURITY.md).

## License

MIT. See [LICENSE](LICENSE).

---

<!-- tech-stack-wall -->
## Tech Stack

<div align="center">

<img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" />
<img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven" />
<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
<img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
<img src="https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white" alt="Kubernetes" />

</div>

<details>
<summary><b>Full Tech Stack Wall</b></summary>
<br>

<div align="center">

<b>Languages</b><br>
<img src="https://img.shields.io/badge/Rust-dea584?style=for-the-badge&logo=rust&logoColor=black" alt="Rust" />
<img src="https://img.shields.io/badge/Go-00ADD8?style=for-the-badge&logo=go&logoColor=white" alt="Go" />
<img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
<img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="JavaScript" />
<img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" />
<img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white" alt="Python" />
<img src="https://img.shields.io/badge/C-A8B9CC?style=for-the-badge&logo=c&logoColor=black" alt="C" />
<img src="https://img.shields.io/badge/C%2B%2B-00599C?style=for-the-badge&logo=cplusplus&logoColor=white" alt="C++" />
<img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
<img src="https://img.shields.io/badge/Swift-F05138?style=for-the-badge&logo=swift&logoColor=white" alt="Swift" />
<img src="https://img.shields.io/badge/Ruby-CC342D?style=for-the-badge&logo=ruby&logoColor=white" alt="Ruby" />
<img src="https://img.shields.io/badge/PHP-777BB4?style=for-the-badge&logo=php&logoColor=white" alt="PHP" />
<img src="https://img.shields.io/badge/Lua-2C2D72?style=for-the-badge&logo=lua&logoColor=white" alt="Lua" />
<img src="https://img.shields.io/badge/Shell-4EAA25?style=for-the-badge&logo=gnubash&logoColor=white" alt="Shell" />
<br><br>
<b>Frontend &amp; Desktop</b><br>
<img src="https://img.shields.io/badge/Vue-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=black" alt="Vue" />
<img src="https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React" />
<img src="https://img.shields.io/badge/Svelte-FF3E00?style=for-the-badge&logo=svelte&logoColor=white" alt="Svelte" />
<img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white" alt="Next.js" />
<img src="https://img.shields.io/badge/Nuxt-00DC82?style=for-the-badge&logo=nuxtdotjs&logoColor=black" alt="Nuxt" />
<img src="https://img.shields.io/badge/Flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white" alt="Flutter" />
<img src="https://img.shields.io/badge/React%20Native-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React Native" />
<img src="https://img.shields.io/badge/Tauri-FFC131?style=for-the-badge&logo=tauri&logoColor=black" alt="Tauri" />
<img src="https://img.shields.io/badge/Electron-47848F?style=for-the-badge&logo=electron&logoColor=white" alt="Electron" />
<img src="https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite" />
<img src="https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white" alt="HTML5" />
<img src="https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white" alt="CSS3" />
<img src="https://img.shields.io/badge/Sass-CC6699?style=for-the-badge&logo=sass&logoColor=white" alt="Sass" />
<img src="https://img.shields.io/badge/Less-1D365D?style=for-the-badge&logo=less&logoColor=white" alt="Less" />
<img src="https://img.shields.io/badge/Tailwind%20CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" alt="Tailwind CSS" />
<img src="https://img.shields.io/badge/Bootstrap-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white" alt="Bootstrap" />
<img src="https://img.shields.io/badge/Three.js-000000?style=for-the-badge&logo=threedotjs&logoColor=white" alt="Three.js" />
<img src="https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white" alt="Node.js" />
<img src="https://img.shields.io/badge/jQuery-0769AD?style=for-the-badge&logo=jquery&logoColor=white" alt="jQuery" />
<br><br>
<b>Database &amp; Storage</b><br>
<img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
<img src="https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white" alt="SQLite" />
<img src="https://img.shields.io/badge/SQL%20Server-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white" alt="SQL Server" />
<img src="https://img.shields.io/badge/Oracle-F80000?style=for-the-badge&logo=oracle&logoColor=white" alt="Oracle" />
<img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
<img src="https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white" alt="MongoDB" />
<img src="https://img.shields.io/badge/DuckDB-FFCB00?style=for-the-badge&logo=duckdb&logoColor=black" alt="DuckDB" />
<img src="https://img.shields.io/badge/ClickHouse-FFCC00?style=for-the-badge&logo=clickhouse&logoColor=black" alt="ClickHouse" />
<img src="https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white" alt="Elasticsearch" />
<img src="https://img.shields.io/badge/Neo4j-008CC1?style=for-the-badge&logo=neo4j&logoColor=white" alt="Neo4j" />
<img src="https://img.shields.io/badge/InfluxDB-22ADF6?style=for-the-badge&logo=influxdb&logoColor=white" alt="InfluxDB" />
<img src="https://img.shields.io/badge/Cassandra-1287B1?style=for-the-badge&logo=apachecassandra&logoColor=white" alt="Cassandra" />
<img src="https://img.shields.io/badge/DynamoDB-4053D6?style=for-the-badge&logo=amazondynamodb&logoColor=white" alt="DynamoDB" />
<img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase" />
<img src="https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=black" alt="Supabase" />
<img src="https://img.shields.io/badge/MinIO-C72E49?style=for-the-badge&logo=minio&logoColor=white" alt="MinIO" />
<img src="https://img.shields.io/badge/Firebird-FC521F?style=for-the-badge" alt="Firebird" />
<img src="https://img.shields.io/badge/IBM%20DB2-1F70C1?style=for-the-badge" alt="IBM DB2" />
<img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white" alt="MariaDB" />
<img src="https://img.shields.io/badge/CockroachDB-7B52C0?style=for-the-badge" alt="CockroachDB" />
<img src="https://img.shields.io/badge/IRIS-3B82F6?style=for-the-badge" alt="IRIS" />
<img src="https://img.shields.io/badge/Databricks%20SQL-FF3621?style=for-the-badge&logo=databricks&logoColor=white" alt="Databricks SQL" />
<img src="https://img.shields.io/badge/Snowflake-29B5E8?style=for-the-badge&logo=snowflake&logoColor=white" alt="Snowflake" />
<img src="https://img.shields.io/badge/Qdrant-DC244C?style=for-the-badge" alt="Qdrant" />
<img src="https://img.shields.io/badge/Redshift-8C4FFF?style=for-the-badge&logo=amazonredshift&logoColor=white" alt="Redshift" />
<img src="https://img.shields.io/badge/Apache%20Kylin-F59E0B?style=for-the-badge" alt="Apache Kylin" />
<img src="https://img.shields.io/badge/BigQuery-4285F4?style=for-the-badge&logo=googlebigquery&logoColor=white" alt="BigQuery" />
<img src="https://img.shields.io/badge/VictoriaMetrics-3483EB?style=for-the-badge" alt="VictoriaMetrics" />
<img src="https://img.shields.io/badge/Databend-4F46E5?style=for-the-badge" alt="Databend" />
<img src="https://img.shields.io/badge/TDengine-1B8FD4?style=for-the-badge" alt="TDengine" />
<img src="https://img.shields.io/badge/Teradata-0055A6?style=for-the-badge" alt="Teradata" />
<br><br>
<b>Messaging &amp; Streaming</b><br>
<img src="https://img.shields.io/badge/Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white" alt="Kafka" />
<img src="https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white" alt="RabbitMQ" />
<img src="https://img.shields.io/badge/RocketMQ-FF6A00?style=for-the-badge&logoColor=white" alt="RocketMQ" />
<img src="https://img.shields.io/badge/Pulsar-188FFF?style=for-the-badge&logo=apachepulsar&logoColor=white" alt="Pulsar" />
<img src="https://img.shields.io/badge/Flink-E6526F?style=for-the-badge&logo=apacheflink&logoColor=white" alt="Flink" />
<br><br>
<b>DevOps &amp; Cloud Native</b><br>
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
<img src="https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white" alt="Kubernetes" />
<img src="https://img.shields.io/badge/Terraform-7B42BC?style=for-the-badge&logo=terraform&logoColor=white" alt="Terraform" />
<img src="https://img.shields.io/badge/Ansible-EE0000?style=for-the-badge&logo=ansible&logoColor=white" alt="Ansible" />
<img src="https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white" alt="Jenkins" />
<img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions" />
<img src="https://img.shields.io/badge/GitLab%20CI-FC6D26?style=for-the-badge&logo=gitlab&logoColor=white" alt="GitLab CI" />
<img src="https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white" alt="Prometheus" />
<img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white" alt="Grafana" />
<img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white" alt="Nginx" />
<img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white" alt="AWS" />
<img src="https://img.shields.io/badge/Azure-0078D4?style=for-the-badge&logo=microsoftazure&logoColor=white" alt="Azure" />
<img src="https://img.shields.io/badge/Cloudflare-F38020?style=for-the-badge&logo=cloudflare&logoColor=white" alt="Cloudflare" />
<br><br>
<b>Tools &amp; Platforms</b><br>
<img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white" alt="Git" />
<img src="https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black" alt="Linux" />
<img src="https://img.shields.io/badge/macOS-000000?style=for-the-badge&logo=apple&logoColor=white" alt="macOS" />
<img src="https://img.shields.io/badge/VS%20Code-007ACC?style=for-the-badge&logo=visualstudiocode&logoColor=white" alt="VS Code" />
<img src="https://img.shields.io/badge/Vim-019733?style=for-the-badge&logo=vim&logoColor=white" alt="Vim" />
<img src="https://img.shields.io/badge/Tmux-1BB91F?style=for-the-badge&logoColor=white" alt="Tmux" />
<img src="https://img.shields.io/badge/Homebrew-FAB316?style=for-the-badge&logo=homebrew&logoColor=black" alt="Homebrew" />
<img src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white" alt="Postman" />
<img src="https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white" alt="Figma" />
<img src="https://img.shields.io/badge/Markdown-000000?style=for-the-badge&logo=markdown&logoColor=white" alt="Markdown" />
<img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven" />
<img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle" />
<img src="https://img.shields.io/badge/CMake-064F8C?style=for-the-badge&logo=cmake&logoColor=white" alt="CMake" />

</div>
</details>

<div align="center">
<sub>Hands-on engineering, built and documented in the open.</sub>
</div>

---

<!-- dbx-connections -->
## DBX Connections

<div align="center">

<img src="https://img.shields.io/badge/Firebird_5n1b-DBX-FC521F?style=flat-square" alt="Firebird_5n1b" />
<img src="https://img.shields.io/badge/JDBCX_d5pn-DBX-6D28D9?style=flat-square" alt="JDBCX_d5pn" />
<img src="https://img.shields.io/badge/IBM%20DB2_0leg-DBX-1F70C1?style=flat-square" alt="IBM DB2_0leg" />
<img src="https://img.shields.io/badge/MariaDB_80g9-DBX-003545?style=flat-square" alt="MariaDB_80g9" />
<img src="https://img.shields.io/badge/SQL%20Server_wieh-DBX-CC2927?style=flat-square" alt="SQL Server_wieh" />
<img src="https://img.shields.io/badge/CockroachDB_mo9c-DBX-7B52C0?style=flat-square" alt="CockroachDB_mo9c" />
<img src="https://img.shields.io/badge/IRIS_cz3h-DBX-3B82F6?style=flat-square" alt="IRIS_cz3h" />
<img src="https://img.shields.io/badge/Databricks_SQL_rmue-DBX-FF3621?style=flat-square" alt="Databricks_SQL_rmue" />
<img src="https://img.shields.io/badge/Snowflake_cm_h9-DBX-29B5E8?style=flat-square" alt="Snowflake_cm_h9" />
<img src="https://img.shields.io/badge/Redis_u0tv-DBX-DC382D?style=flat-square" alt="Redis_u0tv" />
<img src="https://img.shields.io/badge/Qdrant_1r3s-DBX-DC244C?style=flat-square" alt="Qdrant_1r3s" />
<img src="https://img.shields.io/badge/Elasticsearch_zfc3-DBX-005571?style=flat-square" alt="Elasticsearch_zfc3" />
<img src="https://img.shields.io/badge/Cassandra_d8eb-DBX-1287B1?style=flat-square" alt="Cassandra_d8eb" />
<img src="https://img.shields.io/badge/MongoDB_vexq-DBX-47A248?style=flat-square" alt="MongoDB_vexq" />
<img src="https://img.shields.io/badge/Redshift_p9d7-DBX-8C4FFF?style=flat-square" alt="Redshift_p9d7" />
<img src="https://img.shields.io/badge/Apache%20Kylin_xmpz-DBX-F59E0B?style=flat-square" alt="Apache Kylin_xmpz" />
<img src="https://img.shields.io/badge/Neo4j_j3dsg-DBX-008CC1?style=flat-square" alt="Neo4j_j3dsg" />
<img src="https://img.shields.io/badge/BigQuery_homk-DBX-4285F4?style=flat-square" alt="BigQuery_homk" />
<img src="https://img.shields.io/badge/Apache%20Kafka_xpqk-DBX-231F20?style=flat-square" alt="Apache Kafka_xpqk" />
<img src="https://img.shields.io/badge/VictoriaMetrics_m3sf-DBX-3483EB?style=flat-square" alt="VictoriaMetrics_m3sf" />
<img src="https://img.shields.io/badge/InfluxDB_wuju-DBX-22ADF6?style=flat-square" alt="InfluxDB_wuju" />
<img src="https://img.shields.io/badge/Databend_dey6-DBX-4F46E5?style=flat-square" alt="Databend_dey6" />
<img src="https://img.shields.io/badge/TDengine_4c9i-DBX-1B8FD4?style=flat-square" alt="TDengine_4c9i" />
<img src="https://img.shields.io/badge/Teradata_rwlm-DBX-0055A6?style=flat-square" alt="Teradata_rwlm" />

</div>

<sub>Connection names shown in the DBX sidebar - decorative reference badges.</sub>
