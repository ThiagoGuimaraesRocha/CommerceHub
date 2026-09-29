# Sprint 1 — Project foundation

Goal: an executable, repeatable and documented foundation, with no business rules yet.

## Delivered

- Maven multi-module monorepo with a parent POM (Quarkus BOM, pinned plugins, Java 21 / Maven 3.9.6+ enforced).
- Maven Wrapper (Maven 3.9.16).
- Four Quarkus services: `user-service`, `product-service`, `order-service`, `inventory-service`, each with
  REST, SmallRye Health, Oracle JDBC datasource, JSON logging in `prod`, and a health endpoint test.
- One standardized multi-stage Dockerfile (`docker/service.Dockerfile`).
- `docker-compose.yml` with Oracle (default) and the four services (profile `apps`), gated by health checks.
- Oracle init script creating `USER_SCHEMA`, `PRODUCT_SCHEMA`, `ORDER_SCHEMA`, `INVENTORY_SCHEMA` in `FREEPDB1`.
- Configuration by environment: `application.properties` + environment variables + ignored `.env`.
- `scripts/dev.sh` to run any service in dev mode with the local `.env`.
- README and ADRs 0001–0004.

## Versions frozen in this sprint

| Item | Value |
| --- | --- |
| Java | 21 |
| Quarkus platform | 3.33.3.3 (LTS stream) |
| Maven Wrapper / Maven | 3.3.4 / 3.9.16 |
| Oracle image | `gvenzl/oracle-free:23.26.3-slim-faststart@sha256:f5ff19033860d662c821cb04eb10483fa94f14f78eae252d054291ea07028093` |
| Build image | `eclipse-temurin:21.0.12.1_1-jdk-noble` |
| Runtime image | `registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24` |

Deviation from plan v0.2: the plan referenced `gvenzl/oracle-free:23.26.2-full`. The `23.26.3` patch was already
available and the `slim-faststart` flavor was chosen because it is smaller than `full` (no features needed by
CommerceHub are removed) and starts in seconds, which improves the first-run experience.

## Definition of Done

| Criterion | Status | Evidence |
| --- | --- | --- |
| A clean clone runs the documented bootstrap | Done | Fresh `git clone` + `./mvnw verify` and `docker compose --profile apps up -d --build` |
| All services compile with the Maven Wrapper | Done | `./mvnw verify` → `BUILD SUCCESS`, 8 tests green |
| Each service starts and answers the health endpoint | Done | `/q/health/ready` = `UP` on ports 8081–8084 (containers and dev mode) |
| Oracle starts in a container and is reachable by the services | Done | Readiness includes `Database connections health check = UP` |
| No secret is versioned | Done | Only `.env.example` with placeholders; `.env` in `.gitignore` |
| README lets a third person reproduce the run | Done | `README.md` → "Running locally" |
| Final sprint commit integrated into `main` | Done | Git history |

## Carried over to Sprint 2

- Choose the migration tool (recommended: Flyway through `quarkus-flyway`, one history table per schema).
- Add Hibernate ORM with Panache and the Problem Details error format.
- Add OpenAPI (`quarkus-smallrye-openapi`) so every endpoint is documented from the first CRUD.
