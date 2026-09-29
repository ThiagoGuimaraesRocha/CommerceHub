# CommerceHub

**Event-driven e-commerce microservices platform built with Java 21, Quarkus, Oracle and Apache Kafka.**

CommerceHub is a public portfolio project. The business domain is intentionally small (customers, products,
orders and inventory) so that the focus stays on engineering: service boundaries, REST and event-driven
integration, idempotent consumers, automated tests, containers, OpenShift, observability and CI/CD/GitOps.

> All code, data and names in this repository are fictional and were created exclusively for this project.

## Status

| Sprint | Scope | Status |
| --- | --- | --- |
| S1 | Foundation: monorepo, Maven/Quarkus, Oracle, Docker Compose, ADRs | Done |
| S2 | Product Service: CRUD, Oracle persistence, tests, image | Planned |
| S3 | Order Service: orders, items, REST integration with Product Service | Planned |
| S4 | Kafka + Inventory Service: events, stock reservation, idempotency | Planned |
| S5 | User Service + JWT | Planned |
| S6 | Observability: OpenTelemetry, Jaeger, Prometheus, Grafana | Planned |
| S7 | OpenShift deployment | Planned |
| S8 | CI/CD (GitHub Actions, Argo CD, Jenkinsfile) and portfolio polish | Planned |

Sprint notes live in [`docs/sprints`](docs/sprints) and architecture decisions in [`docs/adr`](docs/adr).

## Architecture

```
                 Client
                   |
     OpenShift Route / future API Gateway
                   |
   +---------------+----------------+
   |               |                |
User Service  Product Service  Order Service ---- Kafka ----> Inventory Service
   |               |                |                               |
USER_SCHEMA   PRODUCT_SCHEMA   ORDER_SCHEMA                  INVENTORY_SCHEMA
   \_______________\________________\_______________________________/
                          Oracle Database Free
```

- **Database per service**: each service owns one Oracle schema and never reads another service's tables.
- **Explicit contracts**: services talk through versioned HTTP APIs and JSON events on Kafka.
- **Configuration from the environment**: no credentials in Git; local secrets live in an ignored `.env` file.

| Service | Responsibility | Schema | Dev port | Container port |
| --- | --- | --- | --- | --- |
| `user-service` | Customers and demo JWT authentication | `USER_SCHEMA` | 8081 | 8080 |
| `product-service` | Catalog, prices and categories | `PRODUCT_SCHEMA` | 8082 | 8080 |
| `order-service` | Orders, items and order lifecycle | `ORDER_SCHEMA` | 8083 | 8080 |
| `inventory-service` | Stock, reservations, idempotent event consumption | `INVENTORY_SCHEMA` | 8084 | 8080 |

## Tech stack (Sprint 1 baseline)

| Component | Version |
| --- | --- |
| Java | 21 (LTS) |
| Quarkus | 3.33.3.3 (LTS stream) |
| Maven (via wrapper) | 3.9.16 |
| Oracle Database Free | `gvenzl/oracle-free:23.26.3-slim-faststart` |
| Build image | `eclipse-temurin:21.0.12.1_1-jdk-noble` |
| Runtime image | `registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24` |

## Repository layout

```
.
├── pom.xml                       # parent POM (Quarkus BOM, plugin versions, Java 21 enforcement)
├── mvnw / .mvn/                  # Maven Wrapper
├── services/
│   ├── user-service/
│   ├── product-service/
│   ├── order-service/
│   └── inventory-service/
├── docker/service.Dockerfile     # single multi-stage Dockerfile shared by every service
├── docker-compose.yml            # Oracle (default) + application containers (profile "apps")
├── infra/oracle/init/            # creates one schema per service on the first Oracle start
├── scripts/dev.sh                # runs a service in Quarkus dev mode with the .env variables
└── docs/
    ├── adr/                      # architecture decision records
    └── sprints/                  # sprint notes and Definition of Done evidence
```

## Prerequisites

- JDK 21+ (the Maven Wrapper downloads Maven itself)
- Docker Engine 24+ with Docker Compose v2
- About 3 GB of free RAM for Oracle Database Free

## Running locally

### 1. Configure local secrets

```bash
cp .env.example .env
# edit .env and replace every "change-me" value
```

`.env` is ignored by Git. Oracle only reads these passwords on its **first** start; if you change them later,
recreate the volume with `docker compose down -v`.

### 2. Build and test

```bash
./mvnw verify
```

Unit/API tests run without a database: the datasource is disabled in the `test` profile.

### 3a. Run everything in containers

```bash
docker compose --profile apps up -d --build
docker compose --profile apps ps     # wait until every service is "healthy"
```

### 3b. Or run Oracle in Docker and a service in Quarkus dev mode (live reload)

```bash
docker compose up -d oracle
./scripts/dev.sh product-service     # or user-service, order-service, inventory-service
```

### 4. Check health

```bash
curl http://localhost:8082/q/health/ready
```

```json
{
  "status": "UP",
  "checks": [
    { "name": "Database connections health check", "status": "UP", "data": { "<default>": "UP" } }
  ]
}
```

| Endpoint | Purpose |
| --- | --- |
| `/q/health/live` | Liveness: the process is running |
| `/q/health/ready` | Readiness: includes the Oracle connection check |
| `/q/health` | Both |

### Stopping

```bash
docker compose --profile apps down       # keep Oracle data
docker compose --profile apps down -v    # also delete the Oracle volume
```

## Configuration

Every service reads its configuration from `application.properties`, overridable by environment variables.

| Variable | Default | Used by |
| --- | --- | --- |
| `DB_URL` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` | all services |
| `<SERVICE>_DB_USERNAME` | `<service>_schema` | matching service |
| `<SERVICE>_DB_PASSWORD` | none (required) | matching service and Oracle init |
| `ORACLE_PASSWORD` | none (required) | Oracle `SYS`/`SYSTEM` |
| `ORACLE_HOST_PORT` | `1521` | host port mapped to Oracle |
| `GHCR_OWNER` | `local` | image name prefix `ghcr.io/<owner>/commercehub-<service>` |

Logs are plain text in dev/test and structured JSON in the `prod` profile (containers).

## Conventions

- Branches: protected `main`, work on `feature/*`, merge through pull requests.
- Commits: [Conventional Commits](https://www.conventionalcommits.org/).
- Image tags are always pinned; `latest` is never used.
