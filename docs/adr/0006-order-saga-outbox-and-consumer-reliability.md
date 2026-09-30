# ADR 0006 — Order saga, cancellation, transactional outbox and consumer reliability

- Status: Accepted (tables created in Sprint 2; saga implemented in Sprints 3–4)
- Date: 2026-09-29, updated 2026-09-30

## Context

Confirming an order must change the order row **and** publish exactly one `OrderConfirmed`. Writing to Oracle
and to Kafka are two separate systems (dual write): a crash between them loses or duplicates the event.
The same happens in Inventory Service when it reserves stock and publishes the result. Orders can also be
cancelled, either by the system (business failure) or by the customer, who chooses a reason.

## Decision

### Saga (choreography, Order Service owns the order state)

```
CREATED -> CONFIRMED -> INVENTORY_RESERVED -> PAYMENT_PENDING* -> PAYMENT_APPROVED* -> COMPLETED*

CREATED            -> CANCELLED  customer request, no side effects
CONFIRMED          -> CANCELLED  system: InventoryReservationFailed (INSUFFICIENT_STOCK / UNKNOWN_PRODUCT)
INVENTORY_RESERVED -> CANCELLED  customer request, or system: PaymentFailed*  -> ReleaseInventory
                                 * future, with the Payment Service
```

- Transitions are validated by `OrderStateTransitionService`. An invalid transition is a 409 for HTTP calls
  and an ignored (logged) message for Kafka consumers.
- A customer **cannot** cancel while the order is `CONFIRMED` (reservation in progress): 409
  `order-awaiting-inventory`. This avoids racing with the Inventory Service; the customer retries once the
  order is `INVENTORY_RESERVED` or already `CANCELLED`.

### Cancellation reasons

| Code | Set by | Customer selectable | Note required |
| --- | --- | --- | --- |
| `CHANGED_MIND` | customer | yes | no |
| `ORDERED_BY_MISTAKE` | customer | yes | no |
| `FOUND_BETTER_PRICE` | customer | yes | no |
| `DELIVERY_TIME_TOO_LONG` | customer | yes | no |
| `OTHER` | customer | yes | **yes** (`cancellation_note`) |
| `INSUFFICIENT_STOCK` | system | no | no |
| `UNKNOWN_PRODUCT` | system | no | no |
| `PAYMENT_FAILED` | system | no | no |

API (Sprint 3):

- `GET /api/v1/orders/cancellation-reasons` — customer-selectable codes with a display label.
- `POST /api/v1/orders/{id}/cancel` — body `{ "reasonCode": "CHANGED_MIND", "note": "optional, max 500" }`.
  Returns the updated order; 409 for an invalid state; 400 for an unknown or system-only code, or `OTHER`
  without a note.

`ORDERS` columns (Sprint 3 migration):

```sql
cancellation_reason VARCHAR2(40 CHAR),
cancellation_note   VARCHAR2(500 CHAR),
cancelled_by        VARCHAR2(20 CHAR),
cancelled_at        TIMESTAMP WITH TIME ZONE,
CONSTRAINT ck_orders_cancel_reason CHECK (cancellation_reason IS NULL OR cancellation_reason IN
  ('CHANGED_MIND','ORDERED_BY_MISTAKE','FOUND_BETTER_PRICE','DELIVERY_TIME_TOO_LONG','OTHER',
   'INSUFFICIENT_STOCK','UNKNOWN_PRODUCT','PAYMENT_FAILED')),
CONSTRAINT ck_orders_cancelled_by CHECK (cancelled_by IS NULL OR cancelled_by IN ('CUSTOMER','SYSTEM')),
CONSTRAINT ck_orders_cancel_fields CHECK (
  (status = 'CANCELLED' AND cancellation_reason IS NOT NULL AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL)
  OR (status <> 'CANCELLED' AND cancellation_reason IS NULL AND cancellation_note IS NULL
      AND cancelled_by IS NULL AND cancelled_at IS NULL)),
CONSTRAINT ck_orders_cancel_note CHECK (cancellation_reason IS NULL OR cancellation_reason <> 'OTHER'
  OR cancellation_note IS NOT NULL)
```

The free-text note stays in `ORDER_SCHEMA`; it is **not** copied into events.

### Transactional outbox

- `OUTBOX_EVENTS` exists in `ORDER_SCHEMA` and `INVENTORY_SCHEMA` (both services publish). It stores the full
  envelope in the **same transaction** as the business change.
- A scheduled relay (Quarkus Scheduler) publishes `PENDING` rows in `created_at` order, then sets
  `PUBLISHED` + `published_at`. On error it increments `attempts` and stores `last_error`; after 10 attempts
  the row becomes `FAILED` and is reported by a health check and a metric (Sprint 6).
- Delivery is at-least-once; consumers are idempotent, so the business effect happens exactly once.

### Idempotent consumers

- `PROCESSED_EVENTS` exists in `ORDER_SCHEMA` and `INVENTORY_SCHEMA` (both services consume).
- The dedupe insert and the business change happen in one transaction; a duplicate primary key means
  "already processed" and the message is acknowledged without side effects.
- `consumer_name` = `<service>.<topic>`, for example `inventory-service.commerce.order.events`.

DDL: `services/{order,inventory}-service/src/main/resources/db/migration/V1__create_messaging_tables.sql`
(created by Flyway, verified by `MessagingTablesIT` on Oracle).

| Table | Key | Main columns | Constraints |
| --- | --- | --- | --- |
| `OUTBOX_EVENTS` | `event_id` | `aggregate_type`, `aggregate_id`, `event_type`, `message_kind`, `topic`, `message_key`, `payload` (CLOB), `status`, `attempts`, `last_error`, `created_at`, `published_at` | `payload IS JSON`; kind `EVENT`/`COMMAND`; status `PENDING`/`PUBLISHED`/`FAILED`; `PUBLISHED` requires `published_at`; index `(status, created_at)` |
| `PROCESSED_EVENTS` | `(event_id, consumer_name)` | `event_type`, `processed_at` | primary key is the dedupe rule |

### Error handling (SmallRye Reactive Messaging for Kafka)

- Transient errors: retry with exponential backoff (`@Retry`, max 5 attempts).
- Still failing or unparseable messages: dead-letter topic `<topic>.dlq`
  (`failure-strategy=dead-letter-queue`), with the original headers plus the error cause.
- Business rejections (for example insufficient stock) are **not** errors: they produce a failure event.

## Consequences

- No lost or duplicated business effects, even if a service crashes mid-flow.
- Customer cancellations are auditable (who, when, why) and enforced by database constraints.
- Adds a relay job and two tables per messaging service; worth it and a strong portfolio topic.
- Event publication has a small delay (relay interval, 1 s by default).
