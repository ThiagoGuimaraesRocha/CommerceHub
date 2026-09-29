# ADR 0007 — Schema migrations and test strategy

- Status: Accepted
- Date: 2026-09-29

## Context

Each service owns an Oracle schema. Schema changes must be versioned and reproducible, and tests must prove
that persistence works on Oracle itself, not on an in-memory substitute.

## Decision

### Migrations

- Flyway (`quarkus-flyway` + `quarkus-flyway-oracle`), scripts in `src/main/resources/db/migration`,
  one history table per schema.
- Hibernate never changes the schema (`schema-management.strategy=none`).
- `quarkus.flyway.migrate-at-start` defaults to `true` so that `docker compose up` works out of the box.
  It is controlled by `FLYWAY_MIGRATE_AT_START`; on OpenShift (Sprint 7) migrations can move to a
  pre-deploy Job by setting it to `false` in the application pods.

### Tests

| Level | Naming | Runs with | Database |
| --- | --- | --- | --- |
| Unit | `*Test` in `unit/` | Surefire, JUnit 5 + Mockito, no Quarkus | none |
| API | `*Test` in `api/` | Surefire, `@QuarkusTest` + REST Assured | Oracle via Dev Services |
| Integration | `*IT` in `integration/` | Failsafe, `@QuarkusTest` | Oracle via Dev Services |

- Dev Services starts the **same pinned image** used by Docker Compose
  (`gvenzl/oracle-free:23.26.3-slim-faststart@sha256:...`), so tests and local runs share one Oracle version.
- Tests only need Docker; no manual database setup. The CI runner pre-pulls the image.
- Services without persistence yet deactivate the datasource in the `test` profile.

## Consequences

- `./mvnw verify` is the single quality gate locally and in CI.
- Oracle startup adds roughly 30 s per service test run; acceptable for the confidence gained.
