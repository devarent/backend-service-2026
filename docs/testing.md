# Testing strategy

## Goals

- Business rules are tested without HTTP or a database where possible.
- The REST contract is tested end to end against a real Quarkus runtime.
- The default build requires no Docker, PostgreSQL, or Redis.
- CI runs the same command a developer runs locally: `./mvnw -B verify`.

## Test pyramid

| Layer | Class | Runtime | What it proves |
| --- | --- | --- | --- |
| Unit | `MoneyCalculatorTest` | Plain JUnit 5 | Rounding, scale, validation, subtotal, tax, total |
| Unit | `OrderStatusTest` | Plain JUnit 5 | Transition table and terminal states |
| Unit | `OrderTest` | Plain JUnit 5 | Aggregate invariants, totals, legal and illegal transitions |
| Unit | `OrderServiceTest` | JUnit 5 + Mockito | Service orchestration, idempotency, pagination, conflicts, revenue aggregation |
| Integration | `OrderResourceTest` | `@QuarkusTest` + REST-assured + H2 | HTTP statuses, headers, JSON contract, validation envelope, full order flows |

The service test mocks `OrderRepository`, `RevenueCache`, `OrderMetrics`, and the
typed config. This is practical because Panache static methods are awkward to
mock; the integration test covers the real Panache/Hibernate path.

## Running

```bash
# everything, exactly what CI runs
./mvnw -B verify

# unit tests only (fast)
./mvnw -B -Dtest='MoneyCalculatorTest,OrderStatusTest,OrderTest,OrderServiceTest' test

# REST integration test only
./mvnw -B -Dtest=OrderResourceTest test

# one method
./mvnw -B -Dtest=OrderResourceTest#createReturns201WithComputedTotalsAndEchoesRequestId test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd -B verify
```

Reports are written to `target/surefire-reports/`. CI uploads that directory as
an artifact even when the job fails.

## Test environment

`src/test/resources/application.properties` shadows the main configuration and
is self-contained:

- H2 in PostgreSQL mode: `jdbc:h2:mem:orders;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`
- Hibernate `drop-and-create`
- Flyway disabled
- Redis Dev Services and Redis health disabled
- Revenue cache disabled for deterministic assertions
- Ephemeral HTTP port (`quarkus.http.test-port=0`) so a developer's service on
  8081 cannot break the build

`OrderResourceTest` keeps tests independent by using unique customer ids and
idempotency keys, and by asserting relative invariants (at least one order,
non-negative revenue) rather than a fixed global row count. The in-memory
database is recreated for every Maven test JVM. Tests share one Quarkus
instance, so assertions that depend on the exact total number of rows would be
order-dependent and are avoided.

## What is covered

- 201 create with `Location` and computed subtotal/tax/total
- Idempotent replay returns 200 and the same order id
- Request-id echo and generated request id
- 400 validation envelope with field violations
- 400 invalid status filter, page, and page size
- 404 structured error for unknown UUIDs
- 200 get, list pagination/filtering
- 409 invalid status transition
- 200 cancel and idempotent repeated cancel
- 409 cancellation of a shipped order
- Revenue aggregate excludes cancelled orders
- Service-level duplicate SKU, currency, idempotency-key, and pagination rules
- Money rounding and domain invariants

## Deliberate gaps

- No Testcontainers PostgreSQL test. Flyway is exercised by the Docker Compose
  stack and can be verified with `docker compose run --rm flyway` or by starting
  the app against PostgreSQL. Adding Testcontainers is a natural next step when
  Docker is guaranteed on CI runners.
- No load test in the repository. A k6 script can target `POST /orders` and
  `GET /stats/revenue`; the HPA and resource requests are sized for a demo.
- No mutation testing or coverage gate. Coverage is useful, but a hard percentage
  gate on a small portfolio service encourages low-value tests.

## Adding a test

1. Put pure domain tests in the matching package under `src/test/java`.
2. For HTTP behaviour, extend the `@QuarkusTest` class and use a unique
   idempotency key per test.
3. Assert the error envelope (`status`, `code`, `requestId`) rather than only the
   status code.
4. Keep tests independent: clean state or use unique business keys.
5. Run `./mvnw -B verify` before opening a pull request.
