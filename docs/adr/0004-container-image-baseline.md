# ADR 0004 — Container image baseline

- Status: Accepted
- Date: 2026-09-29

## Context

The four services have the same build and runtime needs. Images must be small, reproducible, free of build
tools and credentials, and able to run on OpenShift, which starts containers with an arbitrary non-root UID.

## Decision

- One multi-stage Dockerfile, `docker/service.Dockerfile`, parameterized by the `SERVICE` build argument and
  built from the repository root (the parent POM is required to build a module).
- Build stage: `eclipse-temurin:21.0.12.1_1-jdk-noble` running the committed Maven Wrapper, with a locked
  BuildKit cache for `~/.m2`.
- Runtime stage: `registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24`, the runtime image recommended by
  Quarkus for OpenShift. It runs as UID 185 and is compatible with arbitrary UIDs.
- Packaging: Quarkus `fast-jar` (`target/quarkus-app`), copied layer by layer (libraries first) for better
  image layer caching.
- Image name: `ghcr.io/<owner>/commercehub-<service>:<version>`.

## Consequences

- The plan document suggested `eclipse-temurin` JRE as runtime; UBI OpenJDK was chosen instead for OpenShift
  compatibility. Both remain valid; the choice can be revisited.
- Images do not depend on source code mounted as volumes.
- Digests will be recorded when images are published to GHCR by the CI pipeline.
