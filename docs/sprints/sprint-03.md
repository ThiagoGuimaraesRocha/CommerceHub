# Sprint 3 — Order Service

Goal: create, query, confirm and cancel orders with Product Service REST integration. No Kafka in this sprint.

## Delivered

- CRUD-style order API at `/api/v1/orders` (create, get, list by customer, confirm, cancel).
- `GET /api/v1/orders/cancellation-reasons` with customer-selectable codes.
- Product snapshot (price, SKU, name) via MicroProfile REST Client + Fault Tolerance
  (`@Timeout(2s)`, `@Retry`, `@CircuitBreaker`).
- Backend totals with money scale 4; unique product per order.
- Flyway `V2__create_orders.sql` (`ORDERS`, `ORDER_ITEMS`) with cancellation CHECK constraints.
- Problem Details for 400/404/409/503; OpenAPI + Swagger UI.
- Tests: unit (calculator, transitions, cancellation, application), API (REST Assured),
  integration (Oracle + WireMock ProductClient).
- Postman collection extended with the order flow.
- Image tag `0.3.0`; Compose wires `PRODUCT_SERVICE_URL=http://product-service:8080`.
- Plan v0.5 (`docs/plano/CommerceHub_Plano_Base_v0.5.md`): Sprint 3 closed, class list aligned with the
  implementation, Sprint 4 set as next step.

## Definition of Done

| Criterion | Status | Evidence |
| --- | --- | --- |
| Order with N items can be created and queried | Done | `OrderResourceTest`, Newman |
| Price, SKU and name stored as snapshot | Done | create API test + repository IT |
| Total calculated in backend | Done | `OrderCalculatorTest`, API scale-4 assertion |
| Product Service accessed only via REST | Done | `ProductClient` |
| Unknown/inactive product → 400; unavailable → 503 | Done | `OrderResourceTest`, `ProductClientIT` |
| Confirm validates `CREATED -> CONFIRMED`; invalid → 409 | Done | `OrderResourceTest` |
| Customer cancel in `CREATED` with reason; `OTHER` needs note; system reason rejected | Done | unit + API tests |
| Cancellation constraints enforced by DB | Done | `OrderRepositoryIT` |
| Unit and integration tests green; image runs clean | Done | `./mvnw verify`, Compose image `0.3.0` |
| Postman collection covers order flow | Done | `postman/CommerceHub.postman_collection.json` |

## Versions added

| Item | Value |
| --- | --- |
| Order Service image | `ghcr.io/<owner>/commercehub-order-service:0.3.0` |
| WireMock (test) | 3.13.1 |
