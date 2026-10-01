# Order Service

Orders of CommerceHub: creation, query, confirmation and customer cancellation. Owns `ORDER_SCHEMA`.
Product price, SKU and name are snapshotted at purchase time through the Product Service REST API.

- OpenAPI: `http://localhost:8083/q/openapi`
- Swagger UI: `http://localhost:8083/q/swagger-ui`
- Health: `http://localhost:8083/q/health`

## Endpoints

| Method | Path | Description | Success | Errors |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/orders` | Create an order with items | 201 + `Location` | 400, 503 |
| `GET` | `/api/v1/orders?customerId=` | List orders for a customer | 200 | 400 |
| `GET` | `/api/v1/orders/{id}` | Get by id | 200 | 404 |
| `POST` | `/api/v1/orders/{id}/confirm` | `CREATED -> CONFIRMED` | 200 | 404, 409 |
| `GET` | `/api/v1/orders/cancellation-reasons` | Customer-selectable reasons | 200 | |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancel while `CREATED` | 200 | 400, 404, 409 |

### Rules

- `customerId` is sent in the body until Sprint 5 (JWT).
- Each product may appear only once in an order.
- Unit price, SKU and product name are snapshotted from Product Service; totals are calculated in the backend.
- Unknown or inactive product → 400 `unknown-product`; Product Service down → 503 `product-service-unavailable`.
- Customer cancellation requires a selectable `reasonCode`; `OTHER` requires `note`.
- Cancelling while `CONFIRMED` returns 409 `order-awaiting-inventory` (reservation in progress from Sprint 4).

## Examples

Create:

```bash
curl -i -X POST http://localhost:8083/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"11111111-1111-1111-1111-111111111111","items":[{"productId":"<product-id>","quantity":2}]}'
```

Confirm:

```bash
curl -X POST http://localhost:8083/api/v1/orders/<id>/confirm
```

Cancel:

```bash
curl -X POST http://localhost:8083/api/v1/orders/<id>/cancel \
  -H 'Content-Type: application/json' \
  -d '{"reasonCode":"CHANGED_MIND"}'
```

## Persistence

- Tables `ORDERS` and `ORDER_ITEMS`, created by Flyway migration `V2__create_orders.sql`.
- Messaging tables (`OUTBOX_EVENTS`, `PROCESSED_EVENTS`) already exist from Sprint 2 (`V1`).
- Cancellation constraints are enforced in the database (`ck_orders_cancel_*`).

## Configuration

| Variable | Default | Description |
| --- | --- | --- |
| `PRODUCT_SERVICE_URL` | `http://localhost:8082` | Product Service base URL for the REST client |
| `ORDER_DB_PASSWORD` | required | Schema password |
| `DB_URL` | local Oracle JDBC URL | Datasource |

## Tests

```bash
./mvnw -pl services/order-service -am verify
```

| Class | Level |
| --- | --- |
| `unit/OrderCalculatorTest` | Totals |
| `unit/OrderStateTransitionTest` | State machine |
| `unit/OrderCancellationTest` | Cancellation rules |
| `unit/OrderApplicationServiceTest` | Create flow with mocked ProductClient |
| `api/OrderResourceTest` | HTTP contract with REST Assured on Oracle |
| `api/HealthEndpointTest` | Health and OpenAPI |
| `integration/OrderRepositoryIT` | Oracle mapping and cancellation constraints |
| `integration/ProductClientIT` | REST client against WireMock |

Oracle is started automatically by Quarkus Dev Services (Docker required).
