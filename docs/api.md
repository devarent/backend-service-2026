# API contract

Base URL: `http://localhost:8080` (local) or `https://orders.example.com`
(deployed). All endpoints are under `/api/v1/orders`.

Interactive OpenAPI: `/q/swagger-ui`  
Machine-readable OpenAPI: `/q/openapi`

## Conventions

| Item | Value |
| --- | --- |
| Content type | `application/json` |
| IDs | Public order id is a UUID; database ids are never exposed |
| Money | JSON number with two decimal places, `BigDecimal`/`NUMERIC(19,2)` |
| Timestamps | ISO-8601 UTC instants |
| Pagination | `page` is zero-based, `size` is 1-100, default `page=0&size=20` |
| Correlation | `X-Request-Id` is echoed or generated and returned on every response |
| Idempotency | Optional `Idempotency-Key` header on `POST /api/v1/orders` |

## Error envelope

Every non-2xx response uses the same shape:

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

Validation failures populate `violations`:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "violations": [
    {"field": "customerEmail", "message": "must be a well-formed email address"},
    {"field": "items", "message": "must not be empty"}
  ]
}
```

Common codes: `VALIDATION_ERROR`, `MALFORMED_JSON`, `NOT_FOUND`,
`INVALID_STATUS_TRANSITION`, `ORDER_NOT_CANCELLABLE`, `CONCURRENT_MODIFICATION`,
`DUPLICATE_SKU`, `UNSUPPORTED_CURRENCY`, `INVALID_IDEMPOTENCY_KEY`,
`INVALID_PAGE`, `INVALID_PAGE_SIZE`, `INTERNAL_ERROR`.

## Create order

`POST /api/v1/orders`

Headers: `Content-Type: application/json`, optional `Idempotency-Key`
(8-128 characters: letters, digits, `.`, `_`, `:`, `-`).

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

`201 Created` with `Location: /api/v1/orders/{id}`. Subtotal is `100.00`, tax
`8.25` at the default 8.25% rate, total `108.25`.

A repeated idempotency key returns `200 OK` with the original order and does not
create a second row. A different payload with the same key is not applied; the
original order wins.

```json
{
  "id": "7f5c9a2e-1b4d-4f1a-9c2e-8b7a6d5e4f3a",
  "customerId": "CUST-1001",
  "customerName": "Ada Lovelace",
  "customerEmail": "ada@example.com",
  "status": "PENDING",
  "currency": "USD",
  "subtotal": 100.00,
  "taxAmount": 8.25,
  "totalAmount": 108.25,
  "items": [
    {"sku": "SKU-KEYBOARD", "name": "Mechanical Keyboard", "quantity": 2, "unitPrice": 40.00, "lineTotal": 80.00},
    {"sku": "SKU-MOUSEPAD", "name": "Mouse Pad", "quantity": 1, "unitPrice": 20.00, "lineTotal": 20.00}
  ],
  "version": 0,
  "createdAt": "2026-11-19T07:15:30.123Z",
  "updatedAt": "2026-11-19T07:15:30.123Z"
}
```

Business validation: at least one item, at most 100, unique SKUs, quantity
1-1000, unit price at least 0.01 with at most two decimals, and the request
currency must match the configured currency (`USD` by default).

## List orders

`GET /api/v1/orders?status=PENDING&page=0&size=20`

`status` is optional; `page` is zero-based; `size` is 1-100.

```bash
curl 'http://localhost:8080/api/v1/orders?status=PENDING&page=0&size=2'
```

```json
{
  "content": [
    {
      "id": "7f5c9a2e-1b4d-4f1a-9c2e-8b7a6d5e4f3a",
      "customerId": "CUST-1001",
      "status": "PENDING",
      "currency": "USD",
      "totalAmount": 108.25,
      "itemCount": 2,
      "createdAt": "2026-11-19T07:15:30.123Z"
    }
  ],
  "page": 0,
  "size": 2,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## Get order

`GET /api/v1/orders/{id}`

```bash
curl http://localhost:8080/api/v1/orders/7f5c9a2e-1b4d-4f1a-9c2e-8b7a6d5e4f3a
```

Returns `200` with the full order, or `404 NOT_FOUND`.

## Transition status

`PATCH /api/v1/orders/{id}/status`

```bash
curl -i -X PATCH http://localhost:8080/api/v1/orders/7f5c9a2e-1b4d-4f1a-9c2e-8b7a6d5e4f3a/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"CONFIRMED"}'
```

Allowed transitions:

```text
PENDING -> CONFIRMED | CANCELLED
CONFIRMED -> PROCESSING | CANCELLED
PROCESSING -> SHIPPED | CANCELLED
SHIPPED -> DELIVERED
DELIVERED, CANCELLED -> terminal
```

An illegal transition returns `409 INVALID_STATUS_TRANSITION`. A no-op
transition to the current status returns `200` without incrementing the version.

## Cancel order (soft delete)

`DELETE /api/v1/orders/{id}`

```bash
curl -i -X DELETE http://localhost:8080/api/v1/orders/7f5c9a2e-1b4d-4f1a-9c2e-8b7a6d5e4f3a
```

Moves PENDING, CONFIRMED, or PROCESSING orders to `CANCELLED` and returns the
updated order with `200`. Repeating the call is safe. SHIPPED and DELIVERED
orders return `409 ORDER_NOT_CANCELLABLE`. Rows are retained for audit and
revenue history.

## Revenue statistics

`GET /api/v1/orders/stats/revenue`

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

Cancelled orders are excluded. The aggregate is computed with a single JPQL
`GROUP BY` query and cached for 30 seconds by default. The response may be up to
the TTL stale; it is not used for billing.

## HTTP status summary

| Status | When |
| --- | --- |
| 200 | Successful read, update, cancel, idempotent replay, stats |
| 201 | New order created |
| 400 | Validation failure, malformed JSON, bad pagination/filter, business validation |
| 404 | Unknown order or route |
| 405 | Method not allowed |
| 409 | Invalid transition, non-cancellable order, optimistic-lock conflict |
| 415 | Unsupported content type |
| 500 | Unexpected server error (correlation id included) |
