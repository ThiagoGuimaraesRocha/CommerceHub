# Event and command contracts

Contract for every message exchanged through Kafka in CommerceHub. It formalizes the order saga described in
the plan (v0.3, sections 5.4–5.6) and is the source of truth for the Java DTOs that each service copies
(no shared library, see [ADR 0001](../adr/0001-microservices-with-quarkus.md)).

Status: **accepted for Sprint 3/4**. Messages marked *future* are reserved names; their payloads may change
when the Payment Service is designed.

## Events vs commands

| Kind | Answers | Naming | Published by | Example |
| --- | --- | --- | --- | --- |
| `EVENT` | "What happened?" | Past tense | The owner of the data that changed | `OrderConfirmed` |
| `COMMAND` | "What should another service do?" | Imperative | The saga coordinator (Order Service) | `ReleaseInventory` |

A command is a request, not a fact: the receiving service may still reject it, and it answers with an event.

## Envelope

Every message, event or command, uses the same JSON envelope. The business data goes into `payload`.

```json
{
  "eventId": "7b0c9a4e-2f1d-4b8e-9c3a-5d6e7f8a9b0c",
  "eventType": "OrderConfirmed",
  "messageKind": "EVENT",
  "schemaVersion": 1,
  "occurredAt": "2026-09-29T18:30:00.000Z",
  "source": "order-service",
  "aggregateType": "Order",
  "aggregateId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "correlationId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "causationId": null,
  "payload": { }
}
```

| Field | Type | Required | Rule |
| --- | --- | --- | --- |
| `eventId` | UUID string | yes | Unique per message (events **and** commands). Deduplication key together with the consumer name: `PROCESSED_EVENTS (event_id, consumer_name)`. Generated once, when the message is written to the outbox, so redeliveries keep the same id. |
| `eventType` | string | yes | PascalCase message name from the catalog below. Consumers route on it. |
| `messageKind` | `EVENT` \| `COMMAND` | yes | Distinguishes facts from intentions. |
| `schemaVersion` | integer | yes | Starts at `1`. Increment only on breaking payload changes; adding optional fields is not breaking. |
| `occurredAt` | ISO-8601 UTC | yes | When the business fact happened (not when it was sent). |
| `source` | string | yes | Producing service: `order-service`, `inventory-service`, `payment-service`. |
| `aggregateType` | string | yes | Owner entity type: `Order`, `Inventory`, `Payment`. |
| `aggregateId` | UUID string | yes | Id of the entity that changed. Also the **Kafka record key**. |
| `correlationId` | UUID string | yes | Saga id. In CommerceHub it is always the `orderId`, so every message of one purchase can be traced together. |
| `causationId` | UUID string | no | `eventId` of the message that caused this one (`null` for the first message of the saga). |
| `payload` | object | yes | Message-specific data, defined below. |

JSON Schema: [`schemas/envelope.schema.json`](schemas/envelope.schema.json).

### Kafka record

| Part | Value |
| --- | --- |
| Key | `orderId` (keeps all messages of one order in the same partition, preserving order) |
| Value | Envelope JSON, UTF-8 |
| Header `eventType` | Same as the envelope field (lets consumers skip messages without parsing) |
| Header `traceparent` | W3C trace context, added in Sprint 6 (OpenTelemetry) |

### Data rules

- Money: JSON number with **scale 4** (`299.9000`), plus `currencyCode` (ISO 4217). Java type `BigDecimal`.
- Dates: ISO-8601 in UTC with `Z`.
- Ids: UUID strings (36 chars).
- Unknown fields must be ignored by consumers (tolerant reader), so producers can add optional fields.

## Topics

| Topic | Messages | Producer | Consumers (consumer group) |
| --- | --- | --- | --- |
| `commerce.order.events` | `OrderConfirmed`, `OrderCancelled`, `OrderCompleted` | order-service | inventory-service (`inventory-service`) |
| `commerce.inventory.events` | `InventoryReserved`, `InventoryReservationFailed`, `InventoryReleased` | inventory-service | order-service (`order-service`) |
| `commerce.inventory.commands` | `ReleaseInventory` | order-service | inventory-service (`inventory-service`) |
| `commerce.payment.commands` *(future)* | `PaymentRequested` | order-service | payment-service |
| `commerce.payment.events` *(future)* | `PaymentApproved`, `PaymentFailed` | payment-service | order-service |

Each consumed topic has a dead-letter topic `<topic>.dlq` for messages that still fail after retries
([ADR 0006](../adr/0006-order-saga-outbox-and-consumer-reliability.md)).

The `consumer_name` stored in `PROCESSED_EVENTS` is `<service>.<topic>`, for example
`inventory-service.commerce.order.events`.

## Message catalog

### `OrderConfirmed` — EVENT, `commerce.order.events`

Published when an order moves `CREATED -> CONFIRMED`. Starts the saga (`causationId = null`).

```json
{
  "orderId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "customerId": "a1b2c3d4-0000-4000-8000-000000000001",
  "items": [
    { "productId": "9d8c7b6a-1111-4222-8333-444455556666", "quantity": 2 }
  ],
  "totalAmount": 299.9000,
  "currencyCode": "BRL"
}
```

### `InventoryReserved` — EVENT, `commerce.inventory.events`

All items were reserved. Order Service moves the order `CONFIRMED -> INVENTORY_RESERVED`.

```json
{
  "orderId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "reservations": [
    { "reservationId": "c0ffee00-1234-4abc-8def-000000000001", "productId": "9d8c7b6a-1111-4222-8333-444455556666", "quantity": 2 }
  ]
}
```

### `InventoryReservationFailed` — EVENT, `commerce.inventory.events`

At least one item was unavailable; **nothing** is reserved (all-or-nothing). Order Service moves the order
`CONFIRMED -> CANCELLED` with reason `INSUFFICIENT_STOCK`.

```json
{
  "orderId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "reason": "INSUFFICIENT_STOCK",
  "unavailableItems": [
    { "productId": "9d8c7b6a-1111-4222-8333-444455556666", "requestedQuantity": 2, "availableQuantity": 1 }
  ]
}
```

`reason` values: `INSUFFICIENT_STOCK`, `UNKNOWN_PRODUCT` (product has no inventory record).

### `ReleaseInventory` — COMMAND, `commerce.inventory.commands`

Compensation: asks Inventory Service to release the reservations of an order. Sent by Order Service when an
order in `INVENTORY_RESERVED` is cancelled by the customer, or (future) when payment fails.

```json
{
  "orderId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "reason": "CUSTOMER_CANCELLED"
}
```

`reason` values: `CUSTOMER_CANCELLED`, `PAYMENT_FAILED`.

### `InventoryReleased` — EVENT, `commerce.inventory.events`

Reservations were released (idempotent: releasing an already released order publishes nothing new).

```json
{
  "orderId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "releasedItems": [
    { "productId": "9d8c7b6a-1111-4222-8333-444455556666", "quantity": 2 }
  ]
}
```

### `OrderCancelled` — EVENT, `commerce.order.events`

```json
{
  "orderId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "previousStatus": "INVENTORY_RESERVED",
  "reason": "CHANGED_MIND",
  "cancelledBy": "CUSTOMER"
}
```

| Field | Values |
| --- | --- |
| `cancelledBy` | `CUSTOMER`, `SYSTEM` |
| `reason` (customer) | `CHANGED_MIND`, `ORDERED_BY_MISTAKE`, `FOUND_BETTER_PRICE`, `DELIVERY_TIME_TOO_LONG`, `OTHER` |
| `reason` (system) | `INSUFFICIENT_STOCK`, `UNKNOWN_PRODUCT`, `PAYMENT_FAILED` |

The customer's free-text note is kept only in `ORDER_SCHEMA` and is never published.

### `OrderCompleted` — EVENT, `commerce.order.events` *(future)*

```json
{ "orderId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c" }
```

### `PaymentRequested` — COMMAND, `commerce.payment.commands` *(future)*

```json
{
  "orderId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "customerId": "a1b2c3d4-0000-4000-8000-000000000001",
  "amount": 299.9000,
  "currencyCode": "BRL"
}
```

### `PaymentApproved` / `PaymentFailed` — EVENT, `commerce.payment.events` *(future)*

```json
{ "orderId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c", "paymentId": "uuid", "reason": "CARD_DECLINED" }
```

`reason` only on `PaymentFailed`.

## Saga walkthrough

```
Client            Order Service                      Kafka                         Inventory Service
  | POST /confirm      |                                |                                  |
  |------------------->| CREATED -> CONFIRMED           |                                  |
  |                    | outbox: OrderConfirmed (E1) -->| commerce.order.events            |
  |                    |                                |--------------------------------->| dedupe E1
  |                    |                                |                                  | reserve all or nothing
  |                    |                                |<---------------------------------| outbox: InventoryReserved (E2, causation E1)
  |                    |<-------------------------------| commerce.inventory.events        |
  |                    | dedupe E2                      |                                  |
  |                    | CONFIRMED -> INVENTORY_RESERVED|                                  |
```

Failure path: Inventory publishes `InventoryReservationFailed`; Order Service moves the order to `CANCELLED`
(reason `INSUFFICIENT_STOCK`) and publishes `OrderCancelled`. No compensation is needed because nothing was
reserved.

Customer cancellation after reservation: `POST /api/v1/orders/{id}/cancel` moves `INVENTORY_RESERVED ->
CANCELLED` and writes `OrderCancelled` + `ReleaseInventory` to the outbox in the same transaction. Inventory
Service releases the stock and answers `InventoryReleased`.

## Storage

Producers write messages to `OUTBOX_EVENTS`; consumers record them in `PROCESSED_EVENTS`. Both tables exist in
`ORDER_SCHEMA` and `INVENTORY_SCHEMA`
([ADR 0006](../adr/0006-order-saga-outbox-and-consumer-reliability.md)).
