# ADR 0001 — Microservices with Java 21 and Quarkus in a Maven monorepo

- Status: Accepted
- Date: 2026-09-29

## Context

CommerceHub must demonstrate microservices, synchronous (REST) and asynchronous (Kafka) integration, and
deployment on OpenShift. The domain is small, so the architecture must stay honest: few services, each with a
clear responsibility, and no shared domain library.

## Decision

- Four services: `user-service`, `product-service`, `order-service`, `inventory-service`.
  Payment and Notification are future extensions and do not block the MVP.
- Java 21 (LTS) and Quarkus from an LTS stream (3.33.x at the time of this decision).
- One Git repository with a Maven parent POM that imports the Quarkus BOM and pins plugin versions.
  Each service is an independent Maven module with its own dependencies, configuration, tests and image.
- The Maven Wrapper is committed so every machine uses the same Maven version.
- Base package per service: `com.commercehub.<service>`.

## Consequences

- One `./mvnw verify` builds and tests everything; one service can be built with `-pl services/<name> -am`.
- Versions of Quarkus and plugins are upgraded in one place.
- The parent POM contains only build configuration. Event DTOs will be copied per service (contract-first)
  instead of being shared, to preserve service autonomy.
