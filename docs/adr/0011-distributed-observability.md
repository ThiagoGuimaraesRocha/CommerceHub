# ADR 0011 — Distributed observability (OpenTelemetry, Jaeger v2, Prometheus, Grafana)

- Status: Accepted
- Date: 2026-10-08

## Context

Sprint 6 needs a local, reproducible way to follow one order across HTTP (Order → Product) and Kafka
(Order ↔ Inventory) without an external SaaS account. Jaeger 1.x reached end of life on 31/12/2025.

## Decision

- Every service uses `quarkus-opentelemetry` and exports traces over OTLP gRPC (`4317`) to **Jaeger v2**
  (`cr.jaegertracing.io/jaegertracing/jaeger:2.21.0`). Jaeger v2 has a native OTLP receiver, so there is
  **no OpenTelemetry Collector** in the local stack.
- HTTP instrumentation is automatic (incoming REST and outgoing REST Client). W3C `traceparent` is the
  propagator.
- Kafka published by the transactional outbox is **not** a SmallRye `@Outgoing` channel. The originating
  span is stored on `OUTBOX_EVENTS.TRACEPARENT` when the row is written and restored by `OutboxRelay`,
  which sets the Kafka `traceparent` header. Consumers join that trace through SmallRye + OpenTelemetry.
- Metrics stay on Prometheus scrape of `/q/metrics` (`quarkus-micrometer-registry-prometheus`). The OTel
  SDK does not export metrics or logs (`quarkus.otel.metrics.exporter=none`).
- Grafana OSS (`grafana/grafana:12.2.0`) is provisioned with Prometheus and Jaeger datasources and a
  CommerceHub dashboard (orders, reservations, outbox, HTTP, consume failures).
- Jaeger, Prometheus and Grafana start with Oracle and Kafka (always-on). Application containers remain
  on the `apps` profile.
- Tests disable the OTel SDK (`%test.quarkus.otel.sdk.disabled=true`) except `TracePropagationIT`, which
  uses an in-process exporter-none profile. An empty outbox keeps readiness UP; `FAILED` rows flip
  `OutboxHealthCheck` to DOWN.

## Consequences

- `docker compose up -d` is enough for `quarkus:dev` to export to `http://localhost:4317`.
- Confirming an order should show a single Jaeger trace that includes Order Service, Product Service
  (REST snapshot) and Inventory Service (Kafka).
- Grafana admin credentials live in `.env`, not in the README.
- Image tags are pinned; digests are not invented when the registry does not publish one in-tree.
