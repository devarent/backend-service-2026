# Operations runbook

## Service level objectives

| Indicator | Target | Measurement |
| --- | --- | --- |
| Availability | 99.9% monthly | Ingress 5xx rate and `/q/health/ready` |
| Read latency | p95 < 250 ms | `http_server_requests_seconds` |
| Write latency | p95 < 400 ms | `http_server_requests_seconds` |
| Error rate | < 1% of requests | 5xx / total requests |
| Revenue cache hit ratio | > 80% | Redis `keyspace_hits` vs `keyspace_misses` |

These are demo targets, not contractual SLAs.

## Health and observability

| Endpoint | Purpose |
| --- | --- |
| `/q/health/live` | Process is alive; used by liveness and startup probes |
| `/q/health/ready` | Database readiness; used by the readiness probe. Redis is intentionally excluded so a cache outage cannot remove healthy pods. |
| `/q/health` | Aggregate health with details |
| `/q/metrics` | Prometheus exposition format |
| `/q/openapi` | OpenAPI document |
| `/q/swagger-ui` | Interactive API explorer |

Key metrics:

- `http_server_requests_seconds_count` / `_sum` by method, URI, status
- `orders_created_total`, `orders_cancelled_total`
- `orders_status_transitions_total{from,to}`
- `jvm_memory_used_bytes`, `jvm_gc_pause_seconds`
- `agroal_acquire_seconds` (connection pool wait)

Every application log line should include the request id when it originates from
a request. The error envelope always includes it.

## Deploy

### Local

```bash
docker compose up --build -d
curl -fsS http://localhost:8080/q/health/ready
```

### Kubernetes

```bash
kubectl apply -k kubernetes/overlays/prod
kubectl -n order-service rollout status deployment/order-service --timeout=180s
```

Before the first deploy, create the secret (never commit real values):

```bash
kubectl -n order-service create secret generic order-service-secrets \
  --from-literal=DB_PASSWORD='<strong-password>' \
  --from-literal=REDIS_PASSWORD='<redis-password>'
```

The base manifests reference `order-service-secrets`; `secret.example.yaml` is a
template only and is not included in the Kustomize resource list.

### Rollback

```bash
kubectl -n order-service rollout undo deployment/order-service
kubectl -n order-service rollout status deployment/order-service
```

Rolling back the application does not roll back the database. Flyway migrations
are forward-only; write them with an expand/contract pattern:

1. **Expand**: add nullable columns or new tables; deploy code that writes both
   old and new shapes.
2. **Migrate**: backfill in batches, verify counts, then make the column NOT NULL.
3. **Contract**: remove old columns only after every running version no longer
   reads them.

For a failed migration, restore the pre-deploy backup or write a new corrective
migration. Do not edit an applied migration; Flyway checksums will fail.

## Runbook scenarios

### Readiness failing / database unavailable

1. Check `/q/health/ready`; inspect the `data-source` check.
2. Verify the PostgreSQL service, credentials, and network policy.
3. Check `kubectl -n order-service logs deploy/order-service --tail=200`.
4. Confirm connection-pool saturation via `agroal_acquire_seconds`.
5. If the database is healthy but the pool is exhausted, reduce per-pod pool
   size or add replicas; do not exceed PostgreSQL `max_connections`.

### Redis unavailable

The API should stay up: `RevenueCache` falls back to an in-process map, and
Redis health is intentionally excluded from the readiness group. Verify:

1. `/q/health/ready` remains UP because PostgreSQL is still reachable.
2. Check Redis logs, `REDIS_URL`/`REDIS_PASSWORD`, and the Redis exporter
   (`redis_up`, `keyspace_hits`, `keyspace_misses`).
3. Revenue requests still succeed; latency and database load may rise.
4. Restore Redis; the cache repopulates on the next request.

### Elevated 5xx

1. Group errors by `code` in logs and by URI/status in metrics.
2. Check for a recent deploy; `kubectl rollout history`.
3. If the error is `CONCURRENT_MODIFICATION`, retry with backoff after reloading.
4. If the error is a database constraint, inspect the order payload and schema.
5. Roll back if the error rate is caused by the new version.

### Elevated latency

1. Check p95/p99 and database pool metrics.
2. Inspect slow queries: `pg_stat_activity`, `pg_stat_statements`.
3. Verify the revenue endpoint is not bypassing the cache.
4. Scale replicas; HPA targets 70% CPU and 80% memory.
5. Check ingress and DNS before blaming the service.

### Order stuck in a state

Use the API, not direct SQL, to transition. The state machine rejects illegal
moves. If a stuck order must be corrected, an operator with database access
should document the change, update `status` and `version`, and add an audit note.
Prefer a small administrative endpoint with authorization in a future version.

## Backup and restore

- PostgreSQL: use the managed provider's automated backups and PITR. For a
  self-hosted demo, `pg_dump -Fc orders > orders.dump` before risky changes.
- Redis: the revenue cache is disposable; no backup is required.
- Kubernetes: manifests live in Git; secrets live in the secret manager.
- Test a restore quarterly and record the recovery time.

## Secret rotation

1. Create the new database credential in PostgreSQL with the same privileges.
2. Update the Kubernetes Secret or external secret store.
3. Restart pods gradually (`kubectl rollout restart deployment/order-service`).
4. Verify readiness and error rates.
5. Revoke the old credential after all pods use the new one.

## On-call checklist

- [ ] Acknowledge alert and note the start time.
- [ ] Check `/q/health/ready`, error rate, and recent deploys.
- [ ] Identify blast radius: all traffic, one endpoint, or one dependency.
- [ ] Mitigate (rollback, scale, failover) before root-cause analysis.
- [ ] Preserve logs and a request id for the incident record.
- [ ] Open a follow-up issue with a concrete preventive action.
