# Sprint 6 — Distributed observability

Goal: OpenTelemetry traces, Prometheus metrics and a Grafana dashboard over the HTTP → Order → Kafka →
Inventory → Order saga.

## Delivered

- OpenTelemetry on all four services (`quarkus-opentelemetry`). OTLP gRPC to Jaeger v2. W3C `traceparent`
  on HTTP (automatic) and Kafka (outbox column + relay header).
- Jaeger v2 `2.21.0`, Prometheus `v3.15.0` and Grafana OSS `12.2.0` in Docker Compose (always-on with
  Oracle and Kafka). No OpenTelemetry Collector (ADR 0011).
- Business metrics: orders created/confirmed/cancelled (reason), inventory reservations (outcome),
  outbox gauges, Kafka consume failures. Prometheus scrape of `/q/metrics`.
- `OutboxHealthCheck`: readiness DOWN when `OUTBOX_EVENTS` has `FAILED` rows; empty test databases stay UP.
- Tests: `MetricsEndpointTest` on every service, `ObservabilityIT`, `TracePropagationIT`, outbox health
  and `traceparent` unit tests.
- README walkthrough to reproduce a purchase and open Jaeger + Grafana. Plan v0.8. Images `0.6.0`.

## Definition of Done

| Criterion | Status | Evidence |
| --- | --- | --- |
| Create/confirm produces a trace with Order and Inventory | Done | Outbox stores `traceparent`; relay sets Kafka header; `TracePropagationIT`, `OutboxRelayTest` |
| Business metrics in Prometheus; Grafana dashboard ≥ 5 metrics | Done | `commercehub_*` counters/gauges; `infra/grafana/dashboards/commercehub.json` (7 panels) |
| An error can be located from trace/log | Done | Consume-failure counter; OTel span on outbox publish errors; JSON logs carry trace id when SDK is on |
| Docs explain how to open Jaeger and Grafana | Done | README observability walkthrough, service READMEs, ADR 0011 |

## Versions added

| Item | Value |
| --- | --- |
| User / Product / Order / Inventory images | `ghcr.io/<owner>/commercehub-*-service:0.6.0` |
| Jaeger | `cr.jaegertracing.io/jaegertracing/jaeger:2.21.0` |
| Prometheus | `prom/prometheus:v3.15.0` |
| Grafana | `grafana/grafana:12.2.0` |
