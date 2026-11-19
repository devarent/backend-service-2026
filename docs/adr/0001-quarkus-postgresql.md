# ADR 0001: Quarkus, PostgreSQL, and Redis for the order service

- **Status:** Accepted
- **Date:** 2026-11-19
- **Deciders:** Backend platform team
- **Context:** Portfolio backend service targeting a 2026 backend engineering role

## Context

The role asks for Java (Quarkus), C# (.NET), REST APIs, microservices,
PostgreSQL, MongoDB/Redis, Kafka/NATS, OOP/design patterns, complex SQL,
unit/integration tests, Docker/Kubernetes, Git, cloud, and AI-assisted
development. A portfolio repository must demonstrate depth, not a checklist of
technologies. It should be small enough to review in one sitting and realistic
enough to discuss trade-offs in an interview.

## Decision

Build the primary service with **Java 21 + Quarkus 3.15**, **PostgreSQL 16**,
**Redis 7**, **Flyway**, **Hibernate ORM with Panache**, **Jakarta Validation**,
**SmallRye Health/OpenAPI**, and **Micrometer/Prometheus**.

- Quarkus provides fast startup, low memory, build-time DI validation, and a
  cohesive extension set for REST, Hibernate, Flyway, Redis, health, and metrics.
- Java 21 records make DTOs and projections concise; virtual threads are
  available for blocking I/O without a reactive rewrite.
- PostgreSQL is the system of record because the order aggregate needs
  transactions, foreign keys, check constraints, and aggregate SQL.
- Redis is used only for a short-lived revenue cache. It is not the source of
  truth, and the service degrades gracefully when it is unavailable.
- Flyway owns schema changes; Hibernate validates the mapping in production and
  generates the schema only in tests.
- Panache keeps the repository layer small while still allowing a hand-written
  JPQL aggregate for revenue.

## Alternatives considered

| Alternative | Why not |
| --- | --- |
| Spring Boot 3 | Excellent choice, but Quarkus differentiates this portfolio and matches the job's Java/Quarkus emphasis |
| Micronaut | Strong runtime, smaller ecosystem for the exact extensions needed here |
| Node/TypeScript | Fast to build, but does not demonstrate Java/OOP depth required by the role |
| MongoDB as system of record | Orders need multi-row transactions and relational integrity; Redis already demonstrates NoSQL |
| Kafka as a hard dependency | Adds operational weight without changing the core order workflow; documented as an outbox extension |
| Testcontainers for every test | Docker is not guaranteed on every developer/CI runner; H2 integration tests keep the default build hermetic |

## Consequences

### Positive

- One command (`./mvnw -B verify`) proves the build without Docker.
- Business rules are testable at the service layer and over HTTP.
- The deployment story is concrete: multi-stage image, health probes, HPA, PDB,
  NetworkPolicy, ServiceMonitor, Kustomize overlays.
- The data model demonstrates constraints, indexes, optimistic locking, and an
  aggregate query.
- The error envelope and correlation id are consistent across success and failure
  paths.

### Negative / trade-offs

- H2 is not PostgreSQL. The Flyway migration is not executed in the default test
  suite; a PostgreSQL Testcontainers profile is the next hardening step.
- Single-currency simplifies revenue reporting but limits real-world use.
- Redis is optional in dev, so the fallback path is exercised more often than
  the real Redis path.
- No authentication in the demo; production must add OIDC and authorization.
- Kafka/NATS is an extension point rather than an implemented integration.

## Follow-up decisions

1. Add a transactional outbox and Kafka publisher for order events.
2. Add Testcontainers PostgreSQL to verify Flyway and Hibernate validation in CI.
3. Add OIDC resource-server security and per-customer authorization.
4. Add OpenTelemetry tracing and a Grafana dashboard.
