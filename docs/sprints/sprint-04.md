# Sprint 4 — Kafka + Inventory Service + idempotency

Goal: event-driven order saga with transactional outbox, idempotent consumers, all-or-nothing stock
reservation and an administrative inventory endpoint.

## Delivered

- Kafka (KRaft, `apache/kafka:3.9.1`) in Docker Compose, always-on with Oracle. Host listener
  `localhost:29092`; in-cluster `kafka:9092`.
- Order Service `0.4.0`: confirm writes `OrderConfirmed` to the outbox; customer cancel in
  `INVENTORY_RESERVED` writes `OrderCancelled` + `ReleaseInventory`. `InventoryEventConsumer` advances
  the order (`INVENTORY_RESERVED` or `CANCELLED` by the system).
- Inventory Service `0.4.0`: Flyway `V2__create_inventory.sql`, all-or-nothing reservation/release,
  `PUT/GET /api/v1/inventory/{productId}`, outbox + relay, consumers for `commerce.order.events` and
  `commerce.inventory.commands`.
- Deduplication by `(eventId, consumerName)` in `PROCESSED_EVENTS`. Technical failures retry then go to
  `<topic>.dlq`.
- Seed: `data/seed/inventory.ndjson` (by SKU); `scripts/seed.sh` resolves `productId` via Product Service
  and calls the admin API.
- Tests: unit (reservation, saga, outbox relay, compensation), API (REST Assured), in-memory consumers,
  Kafka Dev Services integration tests.
- Postman collection extended with the inventory admin API.
- Plan v0.6 (`docs/plano/CommerceHub_Plano_Base_v0.6.md`): Sprint 4 closed, Sprint 5 set as next step.

## Definition of Done

| Criterion | Status | Evidence |
| --- | --- | --- |
| Confirm publishes exactly one `OrderConfirmed` via outbox | Done | `OrderApplicationServiceTest`, `KafkaIntegrationTest` (order) |
| Inventory reserves all-or-nothing | Done | `StockReservationServiceTest`, `OrderEventConsumerTest` |
| Duplicate event does not reserve twice | Done | `IdempotencyTest`, `KafkaIntegrationTest` (inventory) |
| Stock failure → `InventoryReservationFailed` → order `CANCELLED` (`SYSTEM`) | Done | `InventorySagaServiceTest`, `OrderSagaServiceTest`, `InventoryEventConsumerTest` |
| Customer cancel in `INVENTORY_RESERVED` releases stock | Done | `CustomerCancellationCompensationTest`, `InventoryCommandConsumerTest` |
| Admin stock endpoint and seed via API | Done | `InventoryAdminResourceTest`, `scripts/seed.sh` |
| Technical failures go to DLQ after retries | Done | SmallRye `failure-strategy=dead-letter-queue`; `@Retry` on saga handlers |
| Topics/groups documented; Kafka config externalized | Done | `docs/events`, `KAFKA_BOOTSTRAP_SERVERS` |
| Integration tests with a real broker | Done | `KafkaIntegrationTest` (both services, Kafka Dev Services) |

## Versions added

| Item | Value |
| --- | --- |
| Kafka | `apache/kafka:3.9.1` (KRaft) |
| Order Service image | `ghcr.io/<owner>/commercehub-order-service:0.4.0` |
| Inventory Service image | `ghcr.io/<owner>/commercehub-inventory-service:0.4.0` |
