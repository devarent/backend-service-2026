# Contributing

Thanks for considering a contribution. This repository is a portfolio service,
but it follows the same workflow as a production codebase.

## Before you start

- JDK 21 (Temurin is the tested distribution).
- Docker with Compose v2 is optional for the default test suite and required for
  the full local stack.
- No system Maven is required; use the checked-in Maven wrapper.

## Development loop

```bash
# Fast unit tests
./mvnw -B -Dtest='*Test' test

# Full verification, exactly what CI runs
./mvnw -B verify

# Run the API against PostgreSQL and Redis
docker compose up -d postgres redis
./mvnw quarkus:dev
```

On Windows PowerShell, replace `./mvnw` with `.\mvnw.cmd`.

## Pull requests

1. Keep changes focused; do not mix refactoring with behaviour changes.
2. Add or update tests for every behaviour change.
3. Run `./mvnw -B verify` before opening the pull request.
4. Update the README and `docs/` when the API, configuration, or operations
   change.
5. Never commit credentials, tokens, private keys, or real customer data.
6. Use the existing package structure and constructor injection. Do not add
   field injection or static mutable state.

## Commit messages

Use imperative, scoped messages, for example:

```text
feat(order): reject duplicate SKUs on create
fix(cache): fall back to memory when Redis is unavailable
docs(api): document the idempotency-key contract
```

## Code style

- Java 21, four-space indentation, 120-column soft limit.
- Records for immutable DTOs and projections.
- `BigDecimal` for money; never `double`.
- Business rules belong in the service or domain, not the REST resource.
- Public API errors use the `ErrorResponse` envelope and a correlation id.

Formatting is enforced by review rather than a formatter plugin so the build
stays dependency-light. `.editorconfig` describes the expected whitespace.

## Reporting bugs and security issues

Use GitHub issues for functional bugs. Do **not** open a public issue for a
security vulnerability; follow [SECURITY.md](SECURITY.md).
