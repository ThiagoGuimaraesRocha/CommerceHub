# Sprint 2 — Product Service

Goal: product catalog with Oracle persistence and a tested REST API.

## Delivered

- CRUD at `/api/v1/products` (create, list with filters and pagination, get, full update, soft delete).
- Unique SKU (409 `duplicate-sku`), price `>= 0` with scale 4, ISO currency with `BRL` default.
- Optimistic locking with `version` (409 `stale-version`); `@CreationTimestamp` / `@UpdateTimestamp`.
- RFC 9457 Problem Details for 400/404/409/500.
- Flyway migration `V1__create_products.sql` in `PRODUCT_SCHEMA`.
- OpenAPI + Swagger UI (`/q/openapi`, `/q/swagger-ui`).
- Tests: 7 unit (Mockito), 17 API/health (REST Assured), 5 integration on real Oracle via Dev Services.
- Postman collection and local environment (`postman/`), validated with Newman: 14 requests, 23 assertions.
- Demo data (`data/seed/products.ndjson`) loaded through the API by `scripts/seed.sh` (idempotent).
- GitHub Actions CI running `./mvnw verify` with README badge.
- ADRs 0005–0009: event envelope, saga/outbox, migrations/tests, API conventions, local OpenShift.
- Event and command contracts in `docs/events`.

## Definition of Done

| Criterion | Status | Evidence |
| --- | --- | --- |
| POST/GET/PUT/DELETE working | Done | `ProductResourceTest`, Newman run |
| Duplicate SKU returns a controlled conflict | Done | 409 `duplicate-sku` (API test + DB constraint IT) |
| Invalid price is rejected | Done | Negative and >4 decimals return 400; zero is accepted (decision 2026-09-29) |
| Persistence and reads work on local Oracle | Done | `ProductRepositoryIT` on Oracle Dev Services; Compose smoke test |
| Unit and API tests green | Done | `./mvnw verify` |
| Docker image runs without mounted source | Done | `docker compose --profile apps up -d --build` → healthy |
| Service README with endpoints and examples | Done | `services/product-service/README.md` |
| Image published to GHCR | Pending | Needs the GitHub repository push; publication is automated in Sprint 8 |

## Versions added

| Item | Value |
| --- | --- |
| AssertJ | 3.27.7 |
| GitHub Actions | `actions/checkout@v7`, `actions/setup-java@v6`, `actions/upload-artifact@v7` |
| Product Service image | `ghcr.io/<owner>/commercehub-product-service:0.2.0` |
