# Inventory Service

Stock balances and reservations of CommerceHub. Owns `INVENTORY_SCHEMA`. The service is the only writer of
available/reserved quantities; Order Service never touches this schema.

- OpenAPI: `http://localhost:8084/q/openapi`
- Swagger UI: `http://localhost:8084/q/swagger-ui`
- Health: `http://localhost:8084/q/health`

## Endpoints

| Method | Path | Description | Success | Errors |
| --- | --- | --- | --- | --- |
| `PUT` | `/api/v1/inventory/{productId}` | Create or set absolute available quantity (idempotent) | 200 | 400 |
| `GET` | `/api/v1/inventory/{productId}` | Available and reserved quantities | 200 | 404 |

The administrative API is open until Sprint 5 (then `ADMIN` role). Demo data is loaded through it by
`scripts/seed.sh`, never written straight into Oracle.

### Kafka

| Channel | Topic | Role |
| --- | --- | --- |
| incoming `order-events` | `commerce.order.events` | Reserve on `OrderConfirmed` (group `inventory-service`) |
| incoming `inventory-commands` | `commerce.inventory.commands` | Release on `ReleaseInventory` |
| outbox relay | `commerce.inventory.events` | `InventoryReserved` / `InventoryReservationFailed` / `InventoryReleased` |

Reservation is all-or-nothing. A duplicate `eventId` for the same consumer is a no-op.
`ReleaseInventory` for an already released order publishes nothing new.

## Examples

Set stock:

```bash
curl -X PUT http://localhost:8084/api/v1/inventory/<product-id> \
  -H 'Content-Type: application/json' \
  -d '{"availableQuantity":50}'
```

Get stock:

```bash
curl http://localhost:8084/api/v1/inventory/<product-id>
```

Unknown product:

```json
{
  "type": "urn:commercehub:problem:inventory-item-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "No inventory record for product ...",
  "instance": "/api/v1/inventory/..."
}
```

## Persistence

- `INVENTORY_ITEMS` and `STOCK_RESERVATIONS`, Flyway `V2__create_inventory.sql`.
- Messaging tables (`OUTBOX_EVENTS`, `PROCESSED_EVENTS`) from Sprint 2 (`V1`).

## Configuration

| Variable | Default | Description |
| --- | --- | --- |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:29092` | Kafka bootstrap (Compose in-cluster: `kafka:9092`) |
| `INVENTORY_DB_PASSWORD` | required | Schema password |
| `DB_URL` | local Oracle JDBC URL | Datasource |

## Tests

```bash
./mvnw -pl services/inventory-service -am verify
```

| Class | Level |
| --- | --- |
| `unit/InventoryServiceTest` | Admin set/get stock |
| `unit/StockReservationServiceTest` | All-or-nothing reserve/release |
| `unit/InventorySagaServiceTest` | Outbox replies and idempotency |
| `api/InventoryAdminResourceTest` | HTTP contract on Oracle |
| `api/OrderEventConsumerTest` | In-memory `OrderConfirmed` |
| `api/InventoryCommandConsumerTest` | In-memory `ReleaseInventory` |
| `api/IdempotencyTest` | Duplicate event, one reservation |
| `integration/KafkaIntegrationTest` | Real broker (Quarkus Kafka Dev Services) |

Oracle (and Kafka for the broker IT) are started by Quarkus Dev Services (Docker required).
