# ADR 0006 — Order saga, transactional outbox and consumer reliability

- Status: Accepted (implementation in Sprints 3–4)
- Date: 2026-09-29

## Context

Confirming an order must change the order row **and** publish exactly one `OrderConfirmed`. Writing to Oracle
and to Kafka are two separate systems (dual write): a crash between them loses or duplicates the event.
The same problem happens in Inventory Service when it reserves stock and publishes the result.

## Decision

### Saga (choreography, Order Service owns the order state)

```
CREATED -> CONFIRMED -> INVENTORY_RESERVED -> PAYMENT_PENDING* -> PAYMENT_APPROVED* -> COMPLETED*
CONFIRMED -> CANCELLED            (InventoryReservationFailed, reason INSUFFICIENT_STOCK)
INVENTORY_RESERVED -> CANCELLED*  (PaymentFailed -> ReleaseInventory compensation)
                                  * future, with the Payment Service
```

Transitions are validated by `OrderStateTransitionService`; an invalid transition is a 409 for HTTP calls and
an ignored (logged) message for Kafka consumers.

### Transactional outbox

- `ORDER_SCHEMA.OUTBOX_EVENTS` and `INVENTORY_SCHEMA.OUTBOX_EVENTS` store the full envelope in the **same
  transaction** as the business change.
- A scheduled relay (Quarkus Scheduler) publishes pending rows in `created_at` order and marks them as
  published. Delivery is at-least-once; consumers are idempotent, so the effect is exactly-once.

```sql
CREATE TABLE outbox_events (
  event_id       VARCHAR2(36 CHAR)  PRIMARY KEY,
  aggregate_type VARCHAR2(30 CHAR)  NOT NULL,
  aggregate_id   VARCHAR2(36 CHAR)  NOT NULL,
  event_type     VARCHAR2(60 CHAR)  NOT NULL,
  topic          VARCHAR2(120 CHAR) NOT NULL,
  payload        CLOB               NOT NULL,
  status         VARCHAR2(20 CHAR)  DEFAULT 'PENDING' NOT NULL,
  attempts       NUMBER(10,0)       DEFAULT 0 NOT NULL,
  created_at     TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  published_at   TIMESTAMP WITH TIME ZONE,
  CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING','PUBLISHED','FAILED')),
  CONSTRAINT ck_outbox_payload_json CHECK (payload IS JSON)
);
CREATE INDEX ix_outbox_pending ON outbox_events (status, created_at);
```

### Idempotent consumers

- Both Order Service and Inventory Service consume events, so **both** schemas get `PROCESSED_EVENTS`.
- The dedupe insert and the business change happen in one transaction; a duplicate primary key means
  "already processed" and the message is acknowledged without side effects.

### Error handling (SmallRye Reactive Messaging for Kafka)

- Transient errors: retry with exponential backoff (`@Retry`, max 5 attempts).
- Still failing or unparseable messages: dead-letter topic `<topic>.dlq`
  (`failure-strategy=dead-letter-queue`), with the original headers plus the error cause.
- Business rejections (for example insufficient stock) are **not** errors: they produce a failure event.

### Additional columns required by the saga

- `ORDERS.CANCELLATION_REASON VARCHAR2(40 CHAR)` — nullable, set with `CANCELLED`.

## Consequences

- No lost or duplicated business effects, even if a service crashes mid-flow.
- Adds a relay job and two tables per event-producing service; worth it and a strong portfolio topic.
- Event publication has a small delay (relay interval, 1 s by default).
