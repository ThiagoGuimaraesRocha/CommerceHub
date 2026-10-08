# ADR 0008 — HTTP API conventions

- Status: Accepted
- Date: 2026-09-29

## Decision

| Topic | Convention |
| --- | --- |
| Paths | Versioned: `/api/v1/<resource>` |
| Errors | RFC 9457 Problem Details, `application/problem+json`, `type` = `urn:commercehub:problem:<code>`, plus `violations[]` for 400 |
| Status codes | 201 + `Location` on create, 204 on delete, 400 validation/malformed body, 401 missing/invalid token, 403 insufficient role or not owner, 404 unknown id, 409 duplicate or stale version, 500 unexpected |
| Money | `BigDecimal`, stored as `NUMBER(19,4)`, always serialized with **scale 4** (`349.9000`); input accepts up to 4 decimals, more is rejected with 400. Zero is a valid price |
| Currency | ISO 4217 code, default `BRL` |
| Timestamps | ISO-8601 UTC; `created_at` via `@CreationTimestamp`, `updated_at` via `@UpdateTimestamp` |
| Concurrency | Optimistic locking: responses expose `version`; `PUT` must send it back, mismatch returns 409 `stale-version` |
| Delete | Soft delete for catalog data (`DELETE` sets `active=false`), so orders keep valid references |
| Pagination | `page` (0-based) and `size` (1–100) query params; response `{items, page, size, totalItems, totalPages}` |
| Unknown JSON fields | Rejected with 400 (strict requests); event consumers are tolerant instead (ADR 0005) |
| Documentation | OpenAPI at `/q/openapi`, Swagger UI at `/q/swagger-ui` (enabled in every profile) |

### Service-to-service calls (Sprint 3)

- MicroProfile REST Client with SmallRye Fault Tolerance on `ProductClient`:
  `@Timeout(2s)`, `@Retry(maxRetries = 2, only on connection errors/5xx)`, `@CircuitBreaker`.
  404 from Product Service is a business error (400 "unknown product"), not retried.
  Open circuit or timeout returns 503 `product-service-unavailable`.

### Identity (Sprint 5)

- `customerId` comes from the JWT `sub`/`upn` (both set to `user_id`). It is not accepted on
  `POST /api/v1/orders`. See [ADR 0010](0010-demo-jwt.md).
- Catalog GET remains public. Catalog writes, inventory admin and user writes require `ADMIN`.
  Order endpoints require an authenticated customer; a caller may only read/confirm/cancel their own orders.

## Consequences

- Clients handle one error format for every service.
- Postman collection and OpenAPI stay aligned with the same conventions.
