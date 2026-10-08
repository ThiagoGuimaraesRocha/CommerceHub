# ADR 0005 — Event envelope, events vs commands and topic layout

- Status: Accepted
- Date: 2026-09-29

## Context

The order saga (plan v0.3) exchanges facts (events) and requests (commands) through Kafka. Consumers must
deduplicate by `(eventId, consumerName)`, correlate by `orderId`, and stay decoupled from producers.

## Decision

- One JSON envelope for all messages: `eventId`, `eventType`, `messageKind` (`EVENT`/`COMMAND`),
  `schemaVersion`, `occurredAt`, `source`, `aggregateType`, `aggregateId`, `correlationId`, `causationId`,
  `payload`. Full contract in [`docs/events`](../events/README.md).
- `correlationId` is always the `orderId`; `causationId` links each message to the one that triggered it.
- Kafka key = `orderId`, so all messages of one order are ordered within a partition.
- Topics per domain and kind: `commerce.<domain>.events` and `commerce.<domain>.commands`.
- Message names are PascalCase (`OrderConfirmed`), replacing the `ORDER_CONFIRMED` example of plan v0.2.
- Money in messages is a JSON number with scale 4 plus `currencyCode`.
- Consumers are tolerant readers (ignore unknown fields); breaking changes bump `schemaVersion`.
- Each service keeps its own copy of the DTOs; the contract document is the shared artifact.

## Consequences

- Deduplication, tracing and debugging use the same fields in every service.
- `causationId` makes the saga reconstructable from logs. Distributed tracing (W3C `traceparent`) was added
  in Sprint 6 ([ADR 0011](0011-distributed-observability.md)).
- No schema registry is used; JSON Schema in the repository is enough for the portfolio scope.
