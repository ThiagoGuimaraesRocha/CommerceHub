# CommerceHub

[![CI](https://github.com/ThiagoGuimaraesRocha/CommerceHub/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/ThiagoGuimaraesRocha/CommerceHub/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-007396)
![Quarkus 3.33 LTS](https://img.shields.io/badge/Quarkus-3.33%20LTS-4695EB)
![Oracle Database Free](https://img.shields.io/badge/Oracle-Database%20Free%2023-F80000)

**Event-driven e-commerce microservices platform built with Java 21, Quarkus, Oracle and Apache Kafka.**

CommerceHub is a public portfolio project. The business domain is intentionally small (customers, products,
orders and inventory) so the focus stays on engineering: clear service boundaries, REST and event-driven
integration, an order saga with idempotent consumers, automated tests on a real Oracle database, containers,
OpenShift, observability and CI/CD/GitOps.

> All code, data and names in this repository are fictional and were created exclusively for this project.

## Highlights

- **Four Quarkus microservices**, each owning its own Oracle schema (database per service).
- **Order saga** over Kafka with a documented envelope, events vs commands, transactional outbox and
  idempotent consumers ([event contracts](docs/events/README.md)).
- **RFC 9457 Problem Details**, optimistic locking, versioned REST APIs, OpenAPI + Swagger UI.
- **Tests on real Oracle**: unit (JUnit 5 + Mockito), API (REST Assured) and integration tests with
  Oracle Database Free started by Quarkus Dev Services.
- **Reproducible local stack** with Docker Compose, pinned image tags and digests, no secrets in Git.
- **Architecture decisions recorded** as ADRs.

## Status

| Sprint | Scope | Status |
| --- | --- | --- |
| S1 | Foundation: monorepo, Maven/Quarkus, Oracle, Docker Compose, ADRs | Done |
| S2 | Product Service: CRUD, Flyway, Problem Details, OpenAPI, tests, Postman, CI | Done |
| S3 | Order Service: orders, items, REST integration with Product Service | Done |
| S4 | Kafka + Inventory Service: saga, outbox, idempotency, stock admin endpoint | Planned |
| S5 | User Service + JWT | Planned |
| S6 | Observability: OpenTelemetry, Jaeger v2, Prometheus, Grafana | Planned |
| S7 | OpenShift deployment (local MicroShift/OKD) | Planned |
| S8 | CI/CD (GitHub Actions to GHCR, Argo CD, Jenkinsfile) and portfolio polish | Planned |

Sprint notes: [`docs/sprints`](docs/sprints). Decisions: [`docs/adr`](docs/adr). Project plan (Portuguese):
[`docs/plano/CommerceHub_Plano_Base_v0.5.md`](docs/plano/CommerceHub_Plano_Base_v0.5.md).

## Architecture

```
                     Client (Postman / curl)
                               |
              OpenShift Route / future API Gateway
                               |
        +----------------------+----------------------+
        |                      |                      |
   User Service         Product Service <---REST--- Order Service
        |                      |                      |      ^
   USER_SCHEMA          PRODUCT_SCHEMA       OrderConfirmed  |  InventoryReserved /
                                                      |      |  InventoryReservationFailed
                                                      v      |
                                                  +--------------+
                                                  |    Kafka     |
                                                  +--------------+
                                                      |      ^
                                                      v      |
                                                  Inventory Service
                                                         |
                                                  INVENTORY_SCHEMA

     ORDER_SCHEMA belongs to Order Service. All schemas live in one Oracle Database Free
     instance (FREEPDB1); no service reads another service's tables.
```

### Order saga

```
CREATED --confirm--> CONFIRMED --InventoryReserved--> INVENTORY_RESERVED --> (payment, future) --> COMPLETED
                         |
                         +--InventoryReservationFailed--> CANCELLED (INSUFFICIENT_STOCK)

CREATED | INVENTORY_RESERVED --customer cancel (reason chosen by the customer)--> CANCELLED
```

Cancellation is stored on the order (`cancellation_reason`, `cancellation_note`, `cancelled_by`,
`cancelled_at`). Customers pick a reason from `GET /api/v1/orders/cancellation-reasons` (for example
`CHANGED_MIND`, or `OTHER` with a free-text note); the system uses `INSUFFICIENT_STOCK`, `UNKNOWN_PRODUCT`
or `PAYMENT_FAILED`. The `OUTBOX_EVENTS` and `PROCESSED_EVENTS` tables already exist in `ORDER_SCHEMA` and
`INVENTORY_SCHEMA` (see [ADR 0006](docs/adr/0006-order-saga-outbox-and-consumer-reliability.md)).

| Service | Responsibility | Schema | Dev port | Container port |
| --- | --- | --- | --- | --- |
| `user-service` | Customers and demo JWT authentication | `USER_SCHEMA` | 8081 | 8080 |
| [`product-service`](services/product-service/README.md) | Catalog, prices and categories | `PRODUCT_SCHEMA` | 8082 | 8080 |
| [`order-service`](services/order-service/README.md) | Orders, items and order lifecycle (saga owner) | `ORDER_SCHEMA` | 8083 | 8080 |
| `inventory-service` | Stock, reservations, idempotent event consumption | `INVENTORY_SCHEMA` | 8084 | 8080 |

## Tech stack

| Area | Technology |
| --- | --- |
| Language / framework | Java 21, Quarkus 3.33.3.3 (LTS), Quarkus REST, Hibernate ORM with Panache, Hibernate Validator |
| Data | Oracle Database Free 23 (`gvenzl/oracle-free:23.26.3-slim-faststart`, pinned by digest), Flyway |
| Messaging | Apache Kafka (Sprint 4) |
| API docs | SmallRye OpenAPI + Swagger UI, Postman collection |
| Tests | JUnit 5, Mockito, AssertJ, REST Assured, Quarkus Dev Services (Testcontainers) |
| Build / CI | Maven Wrapper 3.9.16, GitHub Actions |
| Containers | Multi-stage Dockerfile, `ubi9/openjdk-21-runtime` (OpenShift-ready), Docker Compose |

## Quick start

Prerequisites: JDK 21+, Docker Engine 24+ with Compose v2, about 4 GB of free RAM.

```bash
git clone https://github.com/ThiagoGuimaraesRocha/CommerceHub.git
cd CommerceHub
cp .env.example .env                           # replace every "change-me" value

docker compose --profile apps up -d --build    # Oracle + the four services
docker compose --profile apps ps               # wait until everything is "healthy"

./scripts/seed.sh                              # load demo products through the API
curl 'http://localhost:8082/api/v1/products?category=PERIPHERALS'
```

Swagger UI: <http://localhost:8082/q/swagger-ui>

### Development mode (live reload)

```bash
docker compose up -d oracle
./scripts/dev.sh product-service     # or user-service, order-service, inventory-service
```

### Build and test

```bash
./mvnw verify
```

Runs unit, API and integration tests. Services with persistence start a throwaway Oracle container through
Quarkus Dev Services, so Docker must be running. CI runs the same command on every push and pull request.

### Stop

```bash
docker compose --profile apps down       # keep Oracle data
docker compose --profile apps down -v    # also delete the Oracle volume
```

## API

| Service | Base path | OpenAPI | Swagger UI |
| --- | --- | --- | --- |
| Product | `http://localhost:8082/api/v1/products` | `/q/openapi` | `/q/swagger-ui` |

Conventions ([ADR 0008](docs/adr/0008-http-api-conventions.md)): versioned paths, `application/problem+json`
errors, money with scale 4, optimistic locking through `version`, soft delete for catalog data.

```bash
curl -i -X POST http://localhost:8082/api/v1/products \
  -H 'Content-Type: application/json' \
  -d '{"sku":"KB-MECH-002","name":"Mechanical Keyboard","categoryCode":"PERIPHERALS","price":349.9}'
```

```json
{
  "id": "17c6cbc4-1865-41bc-b0e7-a99d9d49f572",
  "sku": "KB-MECH-002",
  "name": "Mechanical Keyboard",
  "categoryCode": "PERIPHERALS",
  "price": 349.9000,
  "currencyCode": "BRL",
  "active": true,
  "version": 0,
  "createdAt": "2026-09-29T21:00:55.950624Z",
  "updatedAt": "2026-09-29T21:00:55.950730Z"
}
```

### Postman

Import both files from [`postman/`](postman):

- `CommerceHub.postman_collection.json` — health checks and the full product lifecycle, with test scripts
  that chain `productId` and `version` between requests.
- `CommerceHub.local.postman_environment.json` — local URLs for the four services.

Run it from the Collection Runner, or from the command line:

```bash
npx newman run postman/CommerceHub.postman_collection.json -e postman/CommerceHub.local.postman_environment.json
```

## Events

Kafka messages share one envelope (`eventId`, `eventType`, `messageKind`, `correlationId`, `causationId`,
`payload`, ...). Topics, payloads and the saga walkthrough are in [`docs/events`](docs/events/README.md).

| Topic | Messages |
| --- | --- |
| `commerce.order.events` | `OrderConfirmed`, `OrderCancelled`, `OrderCompleted` |
| `commerce.inventory.events` | `InventoryReserved`, `InventoryReservationFailed`, `InventoryReleased` |
| `commerce.inventory.commands` | `ReleaseInventory` |

## Configuration

| Variable | Default | Used by |
| --- | --- | --- |
| `DB_URL` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` | all services |
| `<SERVICE>_DB_USERNAME` | `<service>_schema` | matching service |
| `<SERVICE>_DB_PASSWORD` | none (required) | matching service and Oracle init |
| `ORACLE_PASSWORD` | none (required) | Oracle `SYS`/`SYSTEM` |
| `ORACLE_HOST_PORT` | `1521` | host port mapped to Oracle |
| `FLYWAY_MIGRATE_AT_START` | `true` | services with persistence |
| `PRODUCT_SERVICE_URL` | `http://localhost:8082` | order-service REST client |
| `GHCR_OWNER` | `local` | image prefix `ghcr.io/<owner>/commercehub-<service>` |

`.env` is ignored by Git. Oracle reads the passwords only on its first start; to change them, run
`docker compose down -v`. Logs are plain text in dev/test and JSON in containers.

## Repository layout

```
.
├── pom.xml                       # parent POM (Quarkus BOM, plugin versions, Java 21 enforcement)
├── services/                     # user-, product-, order-, inventory-service
├── docker/service.Dockerfile     # single multi-stage Dockerfile for every service
├── docker-compose.yml            # Oracle (default) + application containers (profile "apps")
├── infra/oracle/init/            # creates one schema per service on the first Oracle start
├── data/seed/                    # demo data, loaded through the APIs
├── scripts/                      # dev.sh (Quarkus dev mode), seed.sh (demo data)
├── postman/                      # Postman collection and environment
├── .github/workflows/ci.yml      # ./mvnw verify on every push and pull request
└── docs/
    ├── adr/                      # architecture decision records
    ├── events/                   # Kafka envelope, topics and message catalog
    └── sprints/                  # sprint notes and Definition of Done evidence
```

## Architecture decisions

| ADR | Decision |
| --- | --- |
| [0001](docs/adr/0001-microservices-with-quarkus.md) | Microservices with Java 21 and Quarkus in a Maven monorepo |
| [0002](docs/adr/0002-database-per-service.md) | Database per service with one Oracle schema per service |
| [0003](docs/adr/0003-local-environment-docker-compose.md) | Local environment with Docker Compose |
| [0004](docs/adr/0004-container-image-baseline.md) | Container image baseline (UBI OpenJDK runtime) |
| [0005](docs/adr/0005-event-envelope-and-topics.md) | Event envelope, events vs commands, topics |
| [0006](docs/adr/0006-order-saga-outbox-and-consumer-reliability.md) | Order saga, cancellation reasons, transactional outbox, retries and DLQ |
| [0007](docs/adr/0007-migrations-and-test-strategy.md) | Flyway migrations and test strategy on real Oracle |
| [0008](docs/adr/0008-http-api-conventions.md) | HTTP API conventions |
| [0009](docs/adr/0009-local-openshift.md) | Local OpenShift with MicroShift (OKD) |

## Conventions

- Branches: protected `main`, work on `feature/*`, merge through pull requests with green CI.
- Commits: [Conventional Commits](https://www.conventionalcommits.org/).
- Image tags are always pinned; `latest` is never used.
