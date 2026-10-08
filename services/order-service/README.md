# Order Service

Orders of CommerceHub: creation, query, confirmation and customer cancellation. Owns `ORDER_SCHEMA` and
coordinates the order saga. Product price, SKU and name are snapshotted at purchase time through the
Product Service REST API. Confirm and cancel write to the transactional outbox (Sprint 4).

- OpenAPI: `http://localhost:8083/q/openapi`
- Swagger UI: `http://localhost:8083/q/swagger-ui`
- Health: `http://localhost:8083/q/health` (readiness includes `outbox`)
- Metrics: `http://localhost:8083/q/metrics`

## Endpoints

| Method | Path | Description | Success | Errors |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/orders` | Create an order with items | 201 + `Location` | 400, 401, 503 |
| `GET` | `/api/v1/orders` | List orders for the authenticated customer | 200 | 401 |
| `GET` | `/api/v1/orders/{id}` | Get by id (own orders only) | 200 | 401, 403, 404 |
| `POST` | `/api/v1/orders/{id}/confirm` | `CREATED -> CONFIRMED` and outbox `OrderConfirmed` | 200 | 401, 403, 404, 409 |
| `GET` | `/api/v1/orders/cancellation-reasons` | Customer-selectable reasons | 200 | 401 |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancel while `CREATED` or `INVENTORY_RESERVED` | 200 | 400, 401, 403, 404, 409 |

### Rules

- `customerId` comes from the JWT subject (`sub`/`upn`). It is not accepted in the request body.
- Each product may appear only once in an order.
- Unit price, SKU and product name are snapshotted from Product Service; totals are calculated in the backend.
- Unknown or inactive product → 400 `unknown-product`; Product Service down → 503 `product-service-unavailable`.
- Customer cancellation requires a selectable `reasonCode`; `OTHER` requires `note`.
- Cancelling while `CONFIRMED` returns 409 `order-awaiting-inventory` (reservation in progress).
- Cancelling while `INVENTORY_RESERVED` also writes `ReleaseInventory` to the outbox.

### Kafka

| Channel | Topic | Role |
| --- | --- | --- |
| outbox relay | `commerce.order.events` | `OrderConfirmed`, `OrderCancelled` |
| outbox relay | `commerce.inventory.commands` | `ReleaseInventory` |
| incoming `inventory-events` | `commerce.inventory.events` | `InventoryReserved` → `INVENTORY_RESERVED`; `InventoryReservationFailed` → `CANCELLED` |

## Examples

Create:

```bash
curl -i -X POST http://localhost:8083/api/v1/orders \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"items":[{"productId":"<product-id>","quantity":2}]}'
```

Confirm:

```bash
curl -X POST http://localhost:8083/api/v1/orders/<id>/confirm \
  -H "Authorization: Bearer $TOKEN"
```

Cancel:

```bash
curl -X POST http://localhost:8083/api/v1/orders/<id>/cancel \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"reasonCode":"CHANGED_MIND"}'
```

## Persistence

- Tables `ORDERS` and `ORDER_ITEMS`, created by Flyway migration `V2__create_orders.sql`.
- Messaging tables (`OUTBOX_EVENTS`, `PROCESSED_EVENTS`) already exist from Sprint 2 (`V1`).
  `V3` adds `TRACEPARENT` so the outbox relay continues the originating span on Kafka.
- Cancellation constraints are enforced in the database (`ck_orders_cancel_*`).

## Configuration

| Variable | Default | Description |
| --- | --- | --- |
| `PRODUCT_SERVICE_URL` | `http://localhost:8082` | Product Service base URL for the REST client |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:29092` | Kafka bootstrap (Compose in-cluster: `kafka:9092`) |
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
| `unit/OrderApplicationServiceTest` | Create/confirm/cancel with mocked ProductClient and outbox |
| `unit/OrderSagaServiceTest` | Inventory events advance or cancel the order |
| `unit/CustomerCancellationCompensationTest` | Cancel after `INVENTORY_RESERVED` |
| `api/OrderResourceTest` | HTTP contract with REST Assured on Oracle |
| `api/SecurityIT` | 401 without token; 403 on another customer's order |
| `api/HealthEndpointTest` | Health and OpenAPI |
| `api/MetricsEndpointTest` | Prometheus `/q/metrics` |
| `api/ObservabilityIT` | Outbox readiness and business counters |
| `api/TracePropagationIT` | W3C `traceparent` continues the HTTP span |
| `api/InventoryEventConsumerTest` | In-memory inventory events |
| `integration/OrderRepositoryIT` | Oracle mapping and cancellation constraints |
| `integration/ProductClientIT` | REST client against WireMock |
| `integration/KafkaIntegrationTest` | Real broker publishes `OrderConfirmed` |

Oracle is started automatically by Quarkus Dev Services (Docker required).
