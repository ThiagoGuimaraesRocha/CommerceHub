# ADR 0003 — Local environment with Docker Compose

- Status: Accepted
- Date: 2026-09-29

## Context

Anyone cloning the repository (including potential clients) must be able to run the platform with a few
commands and without prior knowledge. OpenShift is the target platform, but it is not required for day-to-day
development.

## Decision

- A root `docker-compose.yml` describes the local infrastructure.
- Oracle starts by default (`docker compose up -d`), which supports running services in Quarkus dev mode.
- The four application containers live in the `apps` profile (`docker compose --profile apps up -d --build`).
- Kafka was added in Sprint 4. Jaeger v2, Prometheus and Grafana were added in Sprint 6 (no OpenTelemetry
  Collector; see [ADR 0011](0011-distributed-observability.md)).
- Quarkus Dev Services are disabled: the infrastructure is always the explicit Compose file, so dev mode and
  containers behave the same way.
- Compose health checks gate startup: services start only after Oracle is healthy, and each service reports
  healthy only when `/q/health/ready` (including the database check) is `UP`.
- Third-party image tags are pinned; `latest` is not used.

## Consequences

- One documented bootstrap path for every machine.
- The Compose file is not a production artifact; OpenShift manifests are created in Sprint 7.
