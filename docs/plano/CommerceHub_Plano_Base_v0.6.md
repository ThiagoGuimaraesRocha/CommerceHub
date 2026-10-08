---
title: "CommerceHub — Documento-base de arquitetura e execução"
subtitle: "Event-Driven E-Commerce Microservices Platform"
version: "0.6"
date: "08/10/2026"
lang: pt-BR
---

# CommerceHub

**Event-Driven E-Commerce Microservices Platform** — documento-base para projeto de portfólio técnico e
aquisição de clientes na Upwork.

| Campo | Valor |
| --- | --- |
| Versão do documento | 0.6 — Sprints 1–4 concluídas; saga de pedido sobre Kafka com outbox e Inventory Service |
| Data | 08/10/2026 |
| Status | Baseline atualizado; pronto para iniciar a Sprint 5 |
| Objetivo | Projeto público de portfólio que demonstre Java, Quarkus, microsserviços, Kafka, Oracle, testes, observabilidade, containers, OpenShift e CI/CD/GitOps |
| Estratégia de evolução | Incremental por sprints, com Definition of Done, contratos de eventos/comandos, ADRs e registro de mudanças |
| Repositório | <https://github.com/ThiagoGuimaraesRocha/CommerceHub> |

> **Regra de confidencialidade:** o projeto deve ser totalmente independente de código, dados, endpoints,
> nomes de sistemas, regras ou arquitetura proprietária do ambiente profissional. A experiência profissional
> serve apenas como referência para escolher competências a demonstrar.

# 1. Controle do documento

Este arquivo é o ponto de partida oficial do projeto. A fonte versionada fica em
`docs/plano/CommerceHub_Plano_Base_v0.6.md`; o DOCX é gerado a partir dela. Cada atualização relevante
incrementa a versão e registra o que mudou.

| Versão | Data | Status | Descrição da alteração |
| --- | --- | --- | --- |
| 0.1 | 29/09/2026 | Baseline | Documento inicial: escopo, arquitetura, contas, sprints, DDLs, classes e imagens Docker. |
| 0.2 | 29/09/2026 | Baseline atualizado | Correção editorial: remoção dos marcadores internos de citação e inclusão de referências legíveis e links oficiais. |
| 0.3 | 29/09/2026 | Arquitetura atualizada | Saga de pedido: InventoryReserved/InventoryReservationFailed, estados do Order, eventos x comandos, processamento idempotente e ação compensatória ReleaseInventory. |
| 0.4 | 30/09/2026 | Baseline atualizado | Sprints 1 e 2 concluídas com versões reais; decisões da revisão técnica aplicadas; envelope de mensagens; cancelamento com motivo escolhido pelo cliente; tabelas OUTBOX_EVENTS e PROCESSED_EVENTS criadas; classes Kafka movidas da S3 para a S4; endpoint administrativo de estoque; OpenShift local (MicroShift/OKD); Jaeger v2; Postman, OpenAPI e CI. |
| 0.5 | 01/10/2026 | Baseline atualizado | Sprint 3 concluída: API de pedidos (criação, consulta, confirmação e cancelamento), snapshot de produto via REST Client + Fault Tolerance, `V2__create_orders.sql`, Problem Details 400/404/409/503, testes unitários/API/integração (Oracle + WireMock), Postman e imagem `order-service:0.3.0`; classes da S3 atualizadas conforme implementação. |
| 0.6 | 08/10/2026 | Baseline atualizado | Sprint 4 concluída: Kafka KRaft no Compose (`apache/kafka:3.9.1`); Inventory Service com reserva tudo-ou-nada, endpoint administrativo e `V2__create_inventory.sql`; outbox + relay nos produtores; consumidores idempotentes; compensação `ReleaseInventory`; seed por SKU; testes unitários/API/Kafka Dev Services; imagens `0.4.0`. |
| 0.7+ | A preencher | Planejada | Atualizações ao fim das Sprints 5 em diante. |

Regra de versionamento: incrementar o segundo dígito para alterações de conteúdo sem quebra de escopo e
registrar alterações estruturais que mudem arquitetura ou tecnologia antes da implementação correspondente
(como ADR em `docs/adr`).

# 2. Visão geral e objetivo

O CommerceHub é uma plataforma de e-commerce demonstrativa, orientada a eventos, formada por microsserviços
Java/Quarkus que se comunicam por APIs REST e Apache Kafka. O projeto é deliberadamente pequeno no domínio,
mas rico em engenharia de software para funcionar como prova pública de competência técnica.

## 2.1 Objetivos

- Demonstrar experiência prática com Java e Quarkus em backend.
- Demonstrar arquitetura de microsserviços e comunicação síncrona e assíncrona.
- Demonstrar Kafka, idempotência e tratamento de falhas em processamento de eventos.
- Demonstrar Oracle/SQL, testes unitários, testes de API e testes de integração.
- Demonstrar containers, OpenShift, CI/CD, GitOps e observabilidade distribuída.
- Produzir documentação, diagramas, decisões arquiteturais e evidências visuais utilizáveis na Upwork.

## 2.2 Fora do escopo inicial

- Frontend web ou aplicativo mobile.
- Pagamento real com provedor externo.
- Integrações com empresas reais ou credenciais reais.
- Service mesh, Terraform, AWS/GCP e infraestrutura pública de produção.
- Dezenas de microsserviços ou modelagem de domínio excessivamente complexa.

> **Princípio de escopo:** a plataforma deve demonstrar engenharia suficiente para gerar credibilidade sem
> se transformar em um produto comercial completo. Pagamento e notificação ficam como extensões futuras, não
> como bloqueadores do MVP.

# 3. Posicionamento profissional usado como referência

O projeto sustenta o posicionamento comercial do profissional na Upwork, sem atribuir experiência em
tecnologias que não fazem parte do histórico informado. Dados pessoais de disponibilidade e idioma ficam
fora da versão pública deste documento.

| Item | Definição |
| --- | --- |
| Headline | Java Backend Developer \| Quarkus \| Microservices \| Kafka \| REST APIs |
| Experiência Java | 7+ anos de experiência profissional desde dezembro de 2018 |
| Quarkus | Contato pontual em 2020 e stack principal profissional desde maio de 2022 |
| Microsserviços | Experiência profissional diária desde maio de 2022 |
| Integrações | Integrações HTTP/RESTful entre APIs em ambiente corporativo de cloud privada |
| Dados | Oracle e SQL |
| Mensageria | Apache Kafka |
| Containers/Plataforma | Contêineres em clusters OpenShift |
| Testes | JUnit, Mockito, REST Assured e testes de integração |
| Build/CI/CD | Maven, Jenkins, GitHub Actions e Argo CD |
| Observabilidade | Grafana, Prometheus, OpenTelemetry e Jaeger |
| Versionamento | GitHub e GitLab |

## 3.1 Stack comercial

| Área | Tecnologias |
| --- | --- |
| Core | Java, Quarkus, Microservices, REST/HTTP, API Integration |
| Data | Oracle, SQL |
| Messaging | Apache Kafka |
| Containers/Platform | Containers, Docker, OpenShift |
| Security | JWT |
| Testing | JUnit, Mockito, REST Assured, Integration Testing |
| Build/CI/CD | Maven, Jenkins, GitHub Actions, Argo CD |
| Observability | OpenTelemetry, Jaeger, Prometheus, Grafana |
| VCS | Git, GitHub, GitLab |

> **Posicionamento:** Spring Boot não é tratado como competência principal, pois não há experiência
> profissional informada no ecossistema Spring.

# 4. Contas e plataformas necessárias

| Plataforma | Conta | Quando | Finalidade | Obrigatoriedade |
| --- | --- | --- | --- | --- |
| GitHub.com | Sim (ativa) | Sprint 1 | Código, Issues, documentação, GitHub Actions e GHCR | Obrigatória |
| Upwork | Sim | Antes da publicação do portfólio | Perfil profissional e aquisição de clientes | Obrigatória para monetização |
| Red Hat Developer | Opcional | Sprint 7/8 | Developer Sandbox ou OpenShift Local apenas para capturas do console web | Opcional |
| Docker Hub | Não | — | Apenas leitura de imagens públicas | Não aplicável |
| Oracle | Não | — | A imagem comunitária Oracle Free não exige conta | Não aplicável |
| Jenkins | Não | Sprint 8 | Instância local para pipeline alternativo | Sem conta externa |
| Argo CD | Não | Sprint 8 | Executado dentro do cluster local | Sem conta externa |
| Kafka | Não | Sprint 4 | Broker local no Docker Compose | Sem conta externa |
| Grafana/Prometheus/Jaeger/OpenTelemetry | Não | Sprint 6 | Containers locais | Sem conta externa |

## 4.1 Verificações das plataformas

- GitHub: conta pessoal com e-mail verificado e 2FA recomendada. O GitHub Container Registry (GHCR) armazena
  imagens OCI e integra com GitHub Actions via `GITHUB_TOKEN`.
- Upwork: um único login de freelancer; perfil completo antes das propostas.
- OpenShift: o ambiente principal passa a ser local (MicroShift/OKD, seção 14). O Developer Sandbox (gratuito,
  janela de 30 dias, cotas limitadas) fica apenas como opção para capturas do console web.
- Oracle local: imagem `gvenzl/oracle-free` (repositório de scripts: `gvenzl/oci-oracle-free` no GitHub),
  tag `23.26.3-slim-faststart` fixada por digest.

# 5. Arquitetura alvo

Quatro microsserviços implementáveis no MVP e dois componentes de extensão futura (Payment Service e
Notification Service). O fluxo de negócio segue uma Saga de pedido coreografada: o Order Service é o único
proprietário do estado do pedido; o Inventory Service é o único proprietário do estoque; nenhum serviço
altera o banco de outro.

```
Client
  |
OpenShift Route / future API Gateway
  |
  +---------------------+---------------------+
  |                     |                     |
User Service      Product Service <--REST-- Order Service
  |                     |                     |      ^
USER_SCHEMA       PRODUCT_SCHEMA     OrderConfirmed  |  InventoryReserved /
                                  OrderCancelled     |  InventoryReservationFailed /
                                  ReleaseInventory   |  InventoryReleased
                                            v        |
                                         +--------------+
                                         |    Kafka     |
                                         +--------------+
                                            |        ^
                                            v        |
                                        Inventory Service
                                              |
                                       INVENTORY_SCHEMA

Futuro: Order Service --PaymentRequested--> Payment Service --PaymentApproved/PaymentFailed--> Order Service
```

## 5.1 Princípios arquiteturais

- Database-per-service: cada microsserviço possui seu próprio schema e não acessa tabelas de outro serviço.
- Contratos HTTP explícitos e versionados (`/api/v1`).
- Mensagens Kafka em JSON com envelope único e `eventId` único (seção 5.7).
- Transactional Outbox para publicar mensagens na mesma transação da mudança de negócio.
- Consumidores idempotentes, deduplicando por `(eventId, consumerName)`.
- Falhas de dependências tratadas com timeout, retry, circuit breaker e fila de erros (DLQ).
- Observabilidade como requisito de qualidade.
- Nenhum segredo persistido no Git; uso de Secret/variáveis de ambiente.

## 5.2 Serviços e responsabilidades

| Serviço | Responsabilidade | Persistência | Comunicação |
| --- | --- | --- | --- |
| User Service | Cadastro/consulta de clientes e autenticação demonstrativa com JWT | USER_SCHEMA | REST |
| Product Service | Catálogo, preço, categoria e estado do produto | PRODUCT_SCHEMA | REST |
| Order Service | Pedidos, itens, cálculo, ciclo de vida e cancelamento; coordena a saga | ORDER_SCHEMA | REST + REST client + Kafka producer/consumer |
| Inventory Service | Estoque, endpoint administrativo de saldo, reservas e liberações | INVENTORY_SCHEMA | REST + Kafka producer/consumer |
| Payment Service | Extensão futura | Não definido | Kafka |
| Notification Service | Extensão futura | Não definido | Kafka |

## 5.3 Fluxo de negócio principal

1. Client → `POST /api/v1/orders`.
2. Order Service valida itens e obtém preço do Product Service (REST).
3. Order Service persiste ORDER + ORDER_ITEM com status `CREATED`.
4. Client → `POST /api/v1/orders/{id}/confirm`.
5. Order Service valida `CREATED -> CONFIRMED` e grava `OrderConfirmed` no outbox na mesma transação.
6. O relay publica `OrderConfirmed`; o Inventory Service consome, verifica disponibilidade e reserva (tudo ou nada).
7. Inventory Service grava no outbox `InventoryReserved` ou `InventoryReservationFailed`.
8. Order Service consome o evento e altera somente o estado do próprio pedido.
9. `InventoryReserved` → `INVENTORY_RESERVED` (na evolução com pagamento, `PaymentRequested`).
10. `InventoryReservationFailed` → `CANCELLED`, `cancelled_by = SYSTEM`, motivo `INSUFFICIENT_STOCK` ou `UNKNOWN_PRODUCT`.
11. Cancelamento pelo cliente (seção 5.4.3): em `INVENTORY_RESERVED`, gera `OrderCancelled` + `ReleaseInventory`.
12. Dedupe por `(eventId, consumerName)` em `PROCESSED_EVENTS` impede efeitos duplicados.

## 5.4 Saga do pedido e transições de estado

```
CREATED -> CONFIRMED -> INVENTORY_RESERVED -> PAYMENT_PENDING (futuro) -> PAYMENT_APPROVED (futuro) -> COMPLETED (futuro)

CREATED            -> CANCELLED   (cliente; sem efeitos colaterais)
CONFIRMED          -> CANCELLED   (sistema: InventoryReservationFailed)
INVENTORY_RESERVED -> CANCELLED   (cliente, ou sistema: PaymentFailed futuro) + ReleaseInventory
```

O cliente não pode cancelar em `CONFIRMED` (reserva em andamento): resposta 409 `order-awaiting-inventory`.

### 5.4.1 Eventos (fatos)

OrderConfirmed, OrderCancelled, OrderCompleted, InventoryReserved, InventoryReservationFailed,
InventoryReleased, PaymentApproved (futuro), PaymentFailed (futuro).

### 5.4.2 Comandos (intenção)

ReleaseInventory e PaymentRequested (futuro). Um comando é uma solicitação ao serviço dono do recurso, que
responde com um evento.

### 5.4.3 Cancelamento do pedido e motivo escolhido pelo cliente

| Código | Definido por | Selecionável pelo cliente | Observação obrigatória |
| --- | --- | --- | --- |
| `CHANGED_MIND` | cliente | sim | não |
| `ORDERED_BY_MISTAKE` | cliente | sim | não |
| `FOUND_BETTER_PRICE` | cliente | sim | não |
| `DELIVERY_TIME_TOO_LONG` | cliente | sim | não |
| `OTHER` | cliente | sim | **sim** (até 500 caracteres) |
| `INSUFFICIENT_STOCK` | sistema | não | não |
| `UNKNOWN_PRODUCT` | sistema | não | não |
| `PAYMENT_FAILED` | sistema | não | não |

- `GET /api/v1/orders/cancellation-reasons`: lista os motivos selecionáveis com rótulo de exibição.
- `POST /api/v1/orders/{id}/cancel` com `{ "reasonCode": "CHANGED_MIND", "note": "opcional" }`.
- A observação livre fica apenas no `ORDER_SCHEMA`; não é publicada em eventos.

## 5.5 Tópicos Kafka

| Tópico | Mensagens | Produtor | Consumidor (grupo) |
| --- | --- | --- | --- |
| `commerce.order.events` | OrderConfirmed, OrderCancelled, OrderCompleted | order-service | inventory-service |
| `commerce.inventory.events` | InventoryReserved, InventoryReservationFailed, InventoryReleased | inventory-service | order-service |
| `commerce.inventory.commands` | ReleaseInventory | order-service | inventory-service |
| `commerce.payment.commands` (futuro) | PaymentRequested | order-service | payment-service |
| `commerce.payment.events` (futuro) | PaymentApproved, PaymentFailed | payment-service | order-service |

Cada tópico consumido tem um tópico de erros `<tópico>.dlq`. Chave do registro Kafka: `orderId`.

## 5.6 Idempotência, outbox e ações compensatórias

- **OUTBOX_EVENTS** (ORDER_SCHEMA e INVENTORY_SCHEMA): a mensagem completa é gravada na mesma transação da
  mudança de negócio; um relay agendado publica as linhas `PENDING` e marca `PUBLISHED`. Após 10 tentativas
  a linha vira `FAILED` e é sinalizada por health check e métrica.
- **PROCESSED_EVENTS** (ORDER_SCHEMA e INVENTORY_SCHEMA): o consumidor insere `(eventId, consumerName)` na
  mesma transação do efeito de negócio; chave duplicada significa "já processado".
- Compensação: cancelamento após reserva (cliente ou pagamento recusado) gera `ReleaseInventory`; o Inventory
  Service libera e responde `InventoryReleased`.
- Erros técnicos: retry com backoff (até 5 tentativas) e depois DLQ. Rejeições de negócio geram eventos de
  falha, não erros.

## 5.7 Envelope de mensagens

Todas as mensagens (eventos e comandos) usam o mesmo envelope. Contrato completo, com payload de cada
mensagem e JSON Schema, em `docs/events`.

```json
{
  "eventId": "7b0c9a4e-2f1d-4b8e-9c3a-5d6e7f8a9b0c",
  "eventType": "OrderConfirmed",
  "messageKind": "EVENT",
  "schemaVersion": 1,
  "occurredAt": "2026-09-29T18:30:00.000Z",
  "source": "order-service",
  "aggregateType": "Order",
  "aggregateId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "correlationId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
  "causationId": null,
  "payload": {
    "orderId": "3f1c2a9e-6b0d-4c47-9a53-0f3f5a1d2b7c",
    "customerId": "a1b2c3d4-0000-4000-8000-000000000001",
    "items": [{ "productId": "9d8c7b6a-1111-4222-8333-444455556666", "quantity": 2 }],
    "totalAmount": 299.9000,
    "currencyCode": "BRL"
  }
}
```

| Campo | Regra |
| --- | --- |
| `eventId` | UUID único por mensagem; chave de deduplicação com o nome do consumidor |
| `eventType` | Nome em PascalCase (`OrderConfirmed`) |
| `messageKind` | `EVENT` ou `COMMAND` |
| `schemaVersion` | Inicia em 1; muda só em quebra de contrato |
| `occurredAt` | ISO-8601 UTC do fato de negócio |
| `source` | Serviço produtor |
| `aggregateType` / `aggregateId` | Entidade que mudou; `aggregateId` é a chave Kafka |
| `correlationId` | Id da saga, sempre o `orderId` |
| `causationId` | `eventId` da mensagem que causou esta (`null` na primeira) |
| `payload` | Dados específicos da mensagem |

# 6. Padrões funcionais e técnicos

| Tema | Padrão definido |
| --- | --- |
| IDs | UUID textual (`VARCHAR2(36 CHAR)`) gerado pela aplicação |
| Datas | `TIMESTAMP WITH TIME ZONE`; UTC na aplicação, JVM e containers |
| Valores monetários | `NUMBER(19,4)`; Java `BigDecimal`; JSON sempre com **escala 4** (`349.9000`); até 4 casas na entrada |
| Preço | Maior ou igual a zero |
| Ativo/inativo | `NUMBER(1)` com CHECK (0/1) |
| Versionamento otimista | Coluna `VERSION_NO` e `@Version`; `PUT` envia a `version` atual, divergência = 409 |
| Auditoria | `created_at` com `@CreationTimestamp`, `updated_at` com `@UpdateTimestamp` |
| Status | Enum Java persistido como texto (`@Enumerated(EnumType.STRING)`) |
| Exclusão | Soft delete para catálogo (`DELETE` marca `active = 0`) |
| Erros HTTP | RFC 9457 Problem Details (`application/problem+json`), `type = urn:commercehub:problem:<código>`; 400, 401, 403, 404, 409, 503 e 500 |
| Paginação | `page` (base 0) e `size` (1–100); resposta `{items, page, size, totalItems, totalPages}` |
| Migrações | Flyway por schema; Hibernate não altera o schema |
| Documentação de API | OpenAPI (`/q/openapi`) e Swagger UI (`/q/swagger-ui`) em todos os perfis |
| Logs | Estruturados em JSON nos containers; nunca registrar senha, JWT, tokens ou dados sensíveis |
| Configuração | `application.properties` + variáveis de ambiente; segredos em `.env` local (ignorado) ou Secret |
| Branches | `main` protegida; `feature/*`; merge por pull request com CI verde |
| Commits | Conventional Commits |

# 7. Roadmap de sprints

| Sprint | Objetivo | Entregável principal | Status |
| --- | --- | --- | --- |
| S1 | Base do projeto | Monorepo, Maven/Quarkus, Oracle, Docker Compose, padrões, ADRs | Concluída (29/09/2026) |
| S2 | Product Service | CRUD, Flyway, Problem Details, OpenAPI, testes, Postman, CI; tabelas de mensageria | Concluída (30/09/2026) |
| S3 | Order Service | Pedidos, itens, integração REST com Product Service, confirmação e cancelamento | Concluída (01/10/2026) |
| S4 | Kafka + Inventory | Saga, outbox, consumidores idempotentes, DLQ, endpoint administrativo de estoque | Concluída (08/10/2026) |
| S5 | User + JWT | Usuários, autenticação demonstrativa e autorização | Planejada |
| S6 | Observabilidade | OpenTelemetry, Jaeger v2, Prometheus, Grafana e métricas | Planejada |
| S7 | OpenShift local | MicroShift/OKD, manifests, ConfigMap/Secret, probes, Routes | Planejada |
| S8 | CI/CD + acabamento | GHCR, Argo CD, Jenkinsfile, README final, screenshots e vídeo | Planejada |

# 8. Sprint 1 — Base do projeto (concluída)

Objetivo: fundação executável, repetível e documentada, sem regras de negócio.

## 8.1 Entregue

- Repositório público `CommerceHub` no GitHub.
- POM pai com BOM do Quarkus, plugins fixados e Java 21/Maven 3.9.6+ obrigatórios; Maven Wrapper 3.9.16.
- Módulos `user-service`, `product-service`, `order-service`, `inventory-service`.
- Dockerfile multi-stage único (`docker/service.Dockerfile`) com runtime UBI OpenJDK 21.
- `docker-compose.yml` com Oracle (padrão) e os quatro serviços (perfil `apps`), com health checks.
- Script de init que cria USER_SCHEMA, PRODUCT_SCHEMA, ORDER_SCHEMA e INVENTORY_SCHEMA no FREEPDB1.
- `scripts/dev.sh` (modo dev), README e ADRs 0001–0004.

## 8.2 Versões congeladas

| Item | Valor |
| --- | --- |
| Java | 21 |
| Quarkus | 3.33.3.3 (LTS) |
| Maven Wrapper / Maven | 3.3.4 / 3.9.16 |
| Oracle | `gvenzl/oracle-free:23.26.3-slim-faststart@sha256:f5ff19033860d662c821cb04eb10483fa94f14f78eae252d054291ea07028093` |
| Imagem de build | `eclipse-temurin:21.0.12.1_1-jdk-noble` |
| Imagem de runtime | `registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24` |

## 8.3 Definition of Done

- [x] Clone limpo executa o bootstrap documentado.
- [x] Todos os serviços compilam com Maven Wrapper.
- [x] Cada serviço sobe e responde ao health endpoint.
- [x] Oracle inicia em container e é acessado pelos serviços.
- [x] Nenhum segredo versionado.
- [x] README permite reproduzir a execução sem conhecimento prévio.
- [x] Commit final integrado na `main`.

# 9. Sprint 2 — Product Service (concluída)

Objetivo: catálogo de produtos com persistência Oracle e API REST testada.

## 9.1 Entregue

- CRUD em `/api/v1/products`: criar, listar com filtros (`category`, `active`) e paginação, consultar,
  atualizar com versionamento otimista e desativar (soft delete).
- SKU único (409 `duplicate-sku`), preço maior ou igual a zero com escala 4, moeda ISO com padrão `BRL`.
- Problem Details para 400/404/409/500; OpenAPI e Swagger UI.
- Migração Flyway `V1__create_products.sql`.
- Testes: unitários (Mockito), API (REST Assured) e integração em Oracle real via Dev Services.
- Coleção Postman + environment local; carga de dados `scripts/seed.sh` pela API.
- Workflow de CI (`./mvnw verify`) com badge no README.
- Tabelas `OUTBOX_EVENTS` e `PROCESSED_EVENTS` criadas por Flyway no ORDER_SCHEMA e no INVENTORY_SCHEMA
  (`V1__create_messaging_tables.sql`), validadas por `MessagingTablesIT`.

## 9.2 DDL — PRODUCT_SCHEMA

```sql
CREATE TABLE products (
    product_id    VARCHAR2(36 CHAR)        PRIMARY KEY,
    sku           VARCHAR2(64 CHAR)        NOT NULL,
    name          VARCHAR2(150 CHAR)       NOT NULL,
    description   VARCHAR2(1000 CHAR),
    category_code VARCHAR2(60 CHAR)        NOT NULL,
    price         NUMBER(19,4)             NOT NULL,
    currency_code VARCHAR2(3 CHAR)         DEFAULT 'BRL' NOT NULL,
    active        NUMBER(1)                DEFAULT 1 NOT NULL,
    version_no    NUMBER(19,0)             DEFAULT 0 NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_products_sku      UNIQUE (sku),
    CONSTRAINT ck_products_price    CHECK (price >= 0),
    CONSTRAINT ck_products_active   CHECK (active IN (0, 1)),
    CONSTRAINT ck_products_currency CHECK (LENGTH(currency_code) = 3)
);
CREATE INDEX ix_products_category ON products (category_code);
CREATE INDEX ix_products_active ON products (active);
```

## 9.3 Classes Java

| Camada | Classes |
| --- | --- |
| Domain | `ProductEntity`, `Money` |
| API | `ProductResource`, `CreateProductRequest`, `UpdateProductRequest`, `ProductResponse`, `PageResponse` |
| Application | `ProductService` |
| Persistence | `ProductRepository` |
| Mapping | `ProductMapper` |
| Errors | `ProductNotFoundException`, `ConflictException`, `ProblemDetail`, `ConstraintViolationHandler`, `ApiExceptionHandler` |
| Config | `JacksonConfig` |
| Tests | `ProductServiceTest`, `ProductResourceTest`, `HealthEndpointTest`, `ProductRepositoryIT` |

## 9.4 Imagem

`ghcr.io/<usuario>/commercehub-product-service:0.2.0`, gerada pelo Dockerfile único (build Temurin 21,
runtime UBI OpenJDK 21).

## 9.5 Definition of Done

- [x] POST/GET/PUT/DELETE de produto funcionando.
- [x] SKU duplicado retorna conflito controlado.
- [x] Preço inválido (negativo ou com mais de 4 casas) é rejeitado; zero é aceito.
- [x] Persistência e leitura funcionam no Oracle local.
- [x] Testes unitários e de API verdes.
- [x] Imagem Docker roda sem código-fonte montado como volume.
- [x] README do serviço com endpoints e exemplos.
- [ ] Imagem publicada no GHCR (automatizada na S8).

# 10. Sprint 3 — Order Service

Objetivo: criação, consulta, confirmação e cancelamento de pedidos, preservando o preço do momento da compra
e mantendo o isolamento do banco. **Sem Kafka nesta sprint**: a confirmação só valida `CREATED -> CONFIRMED`
e a publicação de eventos entra na S4.

## 10.1 Entregáveis

- `POST /api/v1/orders` com itens; `customerId` no corpo até a S5 (depois vem do JWT).
- `GET /api/v1/orders/{id}` e `GET /api/v1/orders?customerId=...`.
- Integração REST com Product Service (MicroProfile REST Client + Fault Tolerance: `@Timeout(2s)`,
  `@Retry` em falhas de conexão/5xx, `@CircuitBreaker`) para validar produto ativo e capturar o preço.
- Cálculo de subtotal por item e total do pedido no backend.
- `POST /api/v1/orders/{id}/confirm`: `CREATED -> CONFIRMED`.
- `GET /api/v1/orders/cancellation-reasons` e `POST /api/v1/orders/{id}/cancel` (em `CREATED`).
- Migração Flyway `V2__create_orders.sql` no ORDER_SCHEMA.
- Testes unitários, API e integração REST.

## 10.2 DDL — ORDER_SCHEMA

```sql
CREATE TABLE orders (
    order_id            VARCHAR2(36 CHAR)        PRIMARY KEY,
    customer_id         VARCHAR2(36 CHAR)        NOT NULL,
    status              VARCHAR2(30 CHAR)        NOT NULL,
    total_amount        NUMBER(19,4)             NOT NULL,
    currency_code       VARCHAR2(3 CHAR)         DEFAULT 'BRL' NOT NULL,
    cancellation_reason VARCHAR2(40 CHAR),
    cancellation_note   VARCHAR2(500 CHAR),
    cancelled_by        VARCHAR2(20 CHAR),
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    version_no          NUMBER(19,0)             DEFAULT 0 NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT ck_orders_total CHECK (total_amount >= 0),
    CONSTRAINT ck_orders_status CHECK (status IN
        ('CREATED','CONFIRMED','INVENTORY_RESERVED','PAYMENT_PENDING','PAYMENT_APPROVED','COMPLETED','CANCELLED')),
    CONSTRAINT ck_orders_currency CHECK (LENGTH(currency_code) = 3),
    CONSTRAINT ck_orders_cancel_reason CHECK (cancellation_reason IS NULL OR cancellation_reason IN
        ('CHANGED_MIND','ORDERED_BY_MISTAKE','FOUND_BETTER_PRICE','DELIVERY_TIME_TOO_LONG','OTHER',
         'INSUFFICIENT_STOCK','UNKNOWN_PRODUCT','PAYMENT_FAILED')),
    CONSTRAINT ck_orders_cancelled_by CHECK (cancelled_by IS NULL OR cancelled_by IN ('CUSTOMER','SYSTEM')),
    CONSTRAINT ck_orders_cancel_fields CHECK (
        (status = 'CANCELLED' AND cancellation_reason IS NOT NULL AND cancelled_by IS NOT NULL
             AND cancelled_at IS NOT NULL)
        OR (status <> 'CANCELLED' AND cancellation_reason IS NULL AND cancellation_note IS NULL
             AND cancelled_by IS NULL AND cancelled_at IS NULL)),
    CONSTRAINT ck_orders_cancel_note CHECK (cancellation_reason IS NULL OR cancellation_reason <> 'OTHER'
        OR cancellation_note IS NOT NULL)
);

CREATE TABLE order_items (
    order_item_id VARCHAR2(36 CHAR) PRIMARY KEY,
    order_id      VARCHAR2(36 CHAR) NOT NULL,
    product_id    VARCHAR2(36 CHAR) NOT NULL,
    sku           VARCHAR2(64 CHAR) NOT NULL,
    product_name  VARCHAR2(150 CHAR) NOT NULL,
    quantity      NUMBER(19,0)      NOT NULL,
    unit_price    NUMBER(19,4)      NOT NULL,
    line_total    NUMBER(19,4)      NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (order_id),
    CONSTRAINT uk_order_items_product UNIQUE (order_id, product_id),
    CONSTRAINT ck_order_items_qty CHECK (quantity > 0),
    CONSTRAINT ck_order_items_price CHECK (unit_price >= 0),
    CONSTRAINT ck_order_items_total CHECK (line_total >= 0)
);

CREATE INDEX ix_orders_customer ON orders (customer_id);
CREATE INDEX ix_orders_status ON orders (status);
CREATE INDEX ix_order_items_order ON order_items (order_id);
```

`sku` e `product_name` em `ORDER_ITEMS` completam o snapshot do produto no momento da compra (o preço já
estava previsto); `uk_order_items_product` impede o mesmo produto duas vezes no pedido.

## 10.3 Classes Java

| Camada | Classes |
| --- | --- |
| Domain | `OrderEntity`, `OrderItemEntity`, `OrderStatus`, `CancellationReason`, `CancelledBy`, `Money` |
| API | `OrderResource`, `CreateOrderRequest`, `OrderItemRequest`, `CancelOrderRequest`, `OrderResponse`, `OrderItemResponse`, `CancellationReasonResponse` |
| Application | `OrderApplicationService`, `OrderCalculator`, `OrderStateTransitionService`, `OrderCancellationService` |
| Persistence | `OrderRepository`, `OrderItemRepository` |
| Integration | `ProductClient`, `ProductSnapshotResponse`, `ProductClientExceptionMapper` |
| Mapping/Config | `OrderMapper`, `JacksonConfig` |
| Errors | `ProblemDetail`, `ApiExceptionHandler`, `ConstraintViolationHandler`, `OrderNotFoundException`, `InvalidOrderStateException`, `UnknownProductException`, `RemoteProductServiceException`, `DuplicateProductInOrderException`, `CurrencyMismatchException` |
| Tests | `OrderApplicationServiceTest`, `OrderCalculatorTest`, `OrderStateTransitionTest`, `OrderCancellationTest`, `OrderResourceTest`, `HealthEndpointTest`, `ProductClientIT`, `OrderRepositoryIT` |

As classes de mensageria (publicação, consumo, outbox e deduplicação) foram movidas para a S4 (seção 11.4).

## 10.4 Imagem

`ghcr.io/<usuario>/commercehub-order-service:0.3.0`.

## 10.5 Definition of Done

- [x] Pedido com N itens pode ser criado e consultado.
- [x] Preço, SKU e nome do item gravados como snapshot.
- [x] Total calculado no backend e validado por teste.
- [x] Product Service acessado apenas via REST.
- [x] Produto inexistente/inativo (400) e serviço indisponível (503) tratados.
- [x] Confirmação valida `CREATED -> CONFIRMED`; transição inválida retorna 409.
- [x] Cliente cancela em `CREATED` escolhendo um motivo; `OTHER` exige observação; motivo de sistema é recusado.
- [x] Restrições de cancelamento garantidas também pelo banco.
- [x] Testes unitários e de integração verdes; imagem executa em ambiente limpo.
- [x] Coleção Postman ampliada com o fluxo de pedidos.

# 11. Sprint 4 — Kafka + Inventory Service + idempotência (concluída)

Objetivo: comunicação orientada a eventos com outbox, consumo idempotente, compensação e endpoint
administrativo de estoque.

## 11.1 Entregáveis

- Kafka local no Docker Compose (`apache/kafka`, tag fixada).
- Order Service: `OrderConfirmed` via outbox ao confirmar; consumo de `commerce.inventory.events`;
  cancelamento pelo cliente em `INVENTORY_RESERVED` com `OrderCancelled` + `ReleaseInventory`.
- Inventory Service: reserva tudo-ou-nada, `InventoryReserved`/`InventoryReservationFailed`, liberação por
  `ReleaseInventory` com `InventoryReleased`.
- Endpoint administrativo de estoque:
  - `PUT /api/v1/inventory/{productId}` com `{ "availableQuantity": 50 }` (cria ou ajusta, idempotente);
  - `GET /api/v1/inventory/{productId}` (disponível e reservado);
  - aberto até a S5, depois exige role `ADMIN`.
- Carga de dados: `data/seed/inventory.ndjson` (por SKU); `scripts/seed.sh` resolve o `productId` pelo SKU no
  Product Service e chama o endpoint administrativo. Nunca grava direto no banco.
- Relay do outbox (Quarkus Scheduler), retry com backoff e DLQ.
- Testes de duplicidade, transições, compensação e integração com broker real.

## 11.2 DDL — INVENTORY_SCHEMA

```sql
CREATE TABLE inventory_items (
    inventory_item_id VARCHAR2(36 CHAR)        PRIMARY KEY,
    product_id        VARCHAR2(36 CHAR)        NOT NULL,
    available_qty     NUMBER(19,0)             NOT NULL,
    reserved_qty      NUMBER(19,0)             DEFAULT 0 NOT NULL,
    version_no        NUMBER(19,0)             DEFAULT 0 NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_inventory_product UNIQUE (product_id),
    CONSTRAINT ck_inventory_available CHECK (available_qty >= 0),
    CONSTRAINT ck_inventory_reserved CHECK (reserved_qty >= 0)
);

CREATE TABLE stock_reservations (
    reservation_id VARCHAR2(36 CHAR)        PRIMARY KEY,
    order_id       VARCHAR2(36 CHAR)        NOT NULL,
    product_id     VARCHAR2(36 CHAR)        NOT NULL,
    quantity       NUMBER(19,0)             NOT NULL,
    status         VARCHAR2(30 CHAR)        NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_reservation_order_product UNIQUE (order_id, product_id),
    CONSTRAINT ck_reservation_qty CHECK (quantity > 0),
    CONSTRAINT ck_reservation_status CHECK (status IN ('RESERVED','RELEASED'))
);
CREATE INDEX ix_reservations_order ON stock_reservations (order_id);
CREATE INDEX ix_reservations_status ON stock_reservations (status);
```

Com a reserva tudo-ou-nada, falhas não geram linha de reserva; por isso os status ficam `RESERVED` e
`RELEASED` (os antigos `PENDING` e `REJECTED` não têm uso).

## 11.3 DDL — tabelas de mensageria (ORDER_SCHEMA e INVENTORY_SCHEMA, já criadas)

```sql
CREATE TABLE outbox_events (
    event_id       VARCHAR2(36 CHAR)        PRIMARY KEY,
    aggregate_type VARCHAR2(30 CHAR)        NOT NULL,
    aggregate_id   VARCHAR2(36 CHAR)        NOT NULL,
    event_type     VARCHAR2(60 CHAR)        NOT NULL,
    message_kind   VARCHAR2(10 CHAR)        NOT NULL,
    topic          VARCHAR2(120 CHAR)       NOT NULL,
    message_key    VARCHAR2(36 CHAR)        NOT NULL,
    payload        CLOB                     NOT NULL,
    status         VARCHAR2(20 CHAR)        DEFAULT 'PENDING' NOT NULL,
    attempts       NUMBER(10,0)             DEFAULT 0 NOT NULL,
    last_error     VARCHAR2(1000 CHAR),
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_outbox_kind         CHECK (message_kind IN ('EVENT', 'COMMAND')),
    CONSTRAINT ck_outbox_status       CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_attempts     CHECK (attempts >= 0),
    CONSTRAINT ck_outbox_payload_json CHECK (payload IS JSON),
    CONSTRAINT ck_outbox_published    CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL)
);
CREATE INDEX ix_outbox_status_created ON outbox_events (status, created_at);
CREATE INDEX ix_outbox_aggregate ON outbox_events (aggregate_id);

CREATE TABLE processed_events (
    event_id      VARCHAR2(36 CHAR)        NOT NULL,
    consumer_name VARCHAR2(100 CHAR)       NOT NULL,
    event_type    VARCHAR2(60 CHAR)        NOT NULL,
    processed_at  TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_processed_events PRIMARY KEY (event_id, consumer_name)
);
```

## 11.4 Classes Java

| Serviço | Camada | Classes |
| --- | --- | --- |
| Order | Application | `OrderSagaService` (além das classes da S3); `OrderApplicationService` passa a gravar no outbox |
| Order | Messaging | `KafkaTopics`, `EventEnvelope`, `MessageSerde`, payloads (`OrderConfirmedPayload`, `OrderCancelledPayload`, `ReleaseInventoryPayload`, `InventoryReservedPayload`, `InventoryReservationFailedPayload`) |
| Order | Outbox | `OutboxEventEntity`, `OutboxRepository`, `OutboxWriter`, `OutboxRelay` |
| Order | Consumer | `InventoryEventConsumer`, `ProcessedEventEntity`, `ProcessedEventRepository`, `EventIdempotencyService` |
| Order | Tests | `OrderSagaServiceTest`, `OutboxRelayTest`, `CustomerCancellationCompensationTest`, `InventoryEventConsumerTest`, `KafkaIntegrationTest` |
| Inventory | API | `InventoryAdminResource`, `SetStockRequest`, `InventoryResponse` |
| Inventory | Domain/Persistence | `InventoryItemEntity`, `StockReservationEntity`, `ReservationStatus`, `InventoryRepository`, `StockReservationRepository` |
| Inventory | Application | `InventoryService`, `StockReservationService`, `InventorySagaService`, `ReservationOutcome`, `ReservationRequest` |
| Inventory | Messaging | `KafkaTopics`, `EventEnvelope`, `MessageSerde`, payloads (`OrderConfirmedPayload`, `ReleaseInventoryPayload`, `InventoryReservedPayload`, `InventoryReservationFailedPayload`, `InventoryReleasedPayload`) |
| Inventory | Outbox/Consumer | `OutboxEventEntity`, `OutboxRepository`, `OutboxWriter`, `OutboxRelay`, `OrderEventConsumer`, `InventoryCommandConsumer`, `ProcessedEventEntity`, `ProcessedEventRepository`, `EventIdempotencyService` |
| Inventory | Tests | `InventoryServiceTest`, `StockReservationServiceTest`, `InventorySagaServiceTest`, `InventoryAdminResourceTest`, `OrderEventConsumerTest`, `InventoryCommandConsumerTest`, `IdempotencyTest`, `KafkaIntegrationTest` |

## 11.5 Imagens

| Imagem | Uso |
| --- | --- |
| `apache/kafka:3.9.1` | Broker Kafka local (modo KRaft) |
| `gvenzl/oracle-free:23.26.3-slim-faststart@sha256:...` | Oracle local e testes |
| `ghcr.io/<usuario>/commercehub-order-service:0.4.0` | Order Service com outbox e consumer |
| `ghcr.io/<usuario>/commercehub-inventory-service:0.4.0` | Inventory Service com consumer e outbox |

## 11.6 Definition of Done

- [x] Confirmar pedido valida `CREATED -> CONFIRMED` e publica exatamente um `OrderConfirmed` (outbox).
- [x] Inventory reserva tudo-ou-nada de forma transacional.
- [x] Evento duplicado não gera segunda reserva nem outro efeito no mesmo consumidor.
- [x] Falha de estoque gera `InventoryReservationFailed` e o pedido vai para `CANCELLED` (`SYSTEM`, `INSUFFICIENT_STOCK`).
- [x] Cancelamento pelo cliente em `INVENTORY_RESERVED` libera o estoque (`ReleaseInventory` → `InventoryReleased`).
- [x] Endpoint administrativo de estoque e carga de dados por ele funcionando.
- [x] Mensagens com falha técnica vão para a DLQ após os retries.
- [x] Tópicos e grupos de consumo documentados; configuração Kafka externalizada.
- [x] Testes de integração com broker real em container verdes.

# 12. Sprint 5 — User Service + JWT

Objetivo: identidade de demonstração e proteção de endpoints com JWT, registrando que o mecanismo é para
portfólio e não substitui um IdP corporativo.

## 12.1 DDL — USER_SCHEMA

```sql
CREATE TABLE app_users (
    user_id       VARCHAR2(36 CHAR)        PRIMARY KEY,
    email         VARCHAR2(320 CHAR)       NOT NULL,
    password_hash VARCHAR2(255 CHAR)       NOT NULL,
    full_name     VARCHAR2(150 CHAR)       NOT NULL,
    role_code     VARCHAR2(30 CHAR)        NOT NULL,
    active        NUMBER(1)                DEFAULT 1 NOT NULL,
    version_no    NUMBER(19,0)             DEFAULT 0 NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_app_users_email UNIQUE (email),
    CONSTRAINT ck_app_users_active CHECK (active IN (0, 1)),
    CONSTRAINT ck_app_users_role CHECK (role_code IN ('CUSTOMER', 'ADMIN'))
);
```

## 12.2 Classes Java

| Camada | Classes |
| --- | --- |
| Domain | `UserEntity`, `UserRole` |
| API | `AuthResource`, `UserResource`, `LoginRequest`, `CreateUserRequest`, `UserResponse`, `TokenResponse` |
| Application | `UserService`, `JwtService`, `PasswordHasher` |
| Persistence | `UserRepository` |
| Security | Emissor de JWT (SmallRye JWT Build) e configuração de verificação nos demais serviços |
| Errors | `InvalidCredentialsException`, `UserNotFoundException` |
| Tests | `AuthResourceTest`, `JwtServiceTest`, `UserServiceTest`, `SecurityIT` |

## 12.3 Mudanças nos outros serviços

- Order Service passa a obter `customerId` do claim `sub` do JWT; o campo sai do corpo de `POST /orders`.
- Um cliente só consulta e cancela os próprios pedidos.
- Endpoint administrativo de estoque e escrita no catálogo exigem role `ADMIN`.

## 12.4 Definition of Done

- [ ] Senha armazenada somente como hash.
- [ ] Login gera JWT assinado para o ambiente de demonstração.
- [ ] Endpoints protegidos rejeitam token ausente/inválido; autorização por role testada.
- [ ] Tokens, senhas e hashes nunca aparecem em logs ou README.
- [ ] Documentação deixa claro o limite de segurança da solução demonstrativa.

# 13. Sprint 6 — Observabilidade distribuída

Objetivo: tracing distribuído, métricas e dashboards sobre o fluxo HTTP → Order Service → Kafka → Inventory
Service → Order Service.

## 13.1 Entregáveis

- OpenTelemetry nos microsserviços; propagação `traceparent` em HTTP e headers Kafka.
- Jaeger v2 para tracing; Prometheus; Grafana com dashboard do CommerceHub.
- Métricas de negócio: pedidos criados/confirmados/cancelados por motivo, reservas, outbox pendente/`FAILED`,
  mensagens em DLQ.
- README com roteiro para reproduzir uma transação e observar o trace.

## 13.2 Classes Java

| Categoria | Classes |
| --- | --- |
| Metrics | `BusinessMetrics` ou `@Counted`/`@Timed` |
| Health | `OutboxHealthCheck` (linhas `FAILED`) |
| Tests | `ObservabilityIT`, `TracePropagationIT`, `MetricsEndpointTest` |

## 13.3 Imagens

| Imagem | Uso |
| --- | --- |
| `cr.jaegertracing.io/jaegertracing/jaeger:2.<x>` | Jaeger v2 (a linha 1.x teve fim de vida em 31/12/2025) |
| `prom/prometheus:<tag fixada>` | Métricas |
| `grafana/grafana:<tag fixada>` | Dashboards (edição OSS) |
| `otel/opentelemetry-collector-contrib:<tag fixada>` | Collector OTLP opcional |

## 13.4 Definition of Done

- [ ] Criação/confirmação de pedido gera trace visível com Order e Inventory.
- [ ] Métricas de negócio no Prometheus; dashboard Grafana com pelo menos 5 métricas.
- [ ] Um cenário de erro pode ser localizado pelo trace/log.
- [ ] Documentação explica como abrir Jaeger e Grafana e reproduzir o fluxo.

# 14. Sprint 7 — OpenShift local

Objetivo: executar os serviços em OpenShift **na máquina local**, iniciando junto com o restante da stack.

## 14.1 Decisão (ADR 0009)

- **MicroShift upstream (OKD)**, imagem `ghcr.io/microshift-io/microshift`, executado como container
  privilegiado (bootc com systemd), sem conta nem pull secret da Red Hat. Oferece Routes e o modelo SCC.
- Oracle e Kafka permanecem no Docker Compose; os quatro serviços são implantados **dentro** do MicroShift.
- Um comando único, `scripts/up.sh`, sobe o Compose, o MicroShift e aplica os manifests. Um perfil
  `openshift` no Compose será usado se o spike confirmar a execução estável sob Docker; caso contrário,
  o script usa Podman.
- Capturas do console web (visão Topology) com OpenShift Local (CRC) ou Developer Sandbox, só para evidência.

## 14.2 Entregáveis

- Manifests por serviço: Deployment, Service, Route, ConfigMap, Secret.
- Readiness/liveness em `/q/health/ready` e `/q/health/live`; requests/limits de CPU e memória.
- Migrações Flyway em Job pré-deploy (`FLYWAY_MIGRATE_AT_START=false` nos pods).
- Guia de deploy e rollback.

## 14.3 Pontos a validar no spike

- Execução do MicroShift sob Docker Engine (documentação oficial cobre Podman).
- Armazenamento (TopoLVM exige volume LVM; alternativa para demonstração).
- Requisitos: Linux com Podman ou Podman machine no macOS/Windows; +2–4 GB de RAM.

## 14.4 Definition of Done

- [ ] Os quatro serviços implantados a partir de manifests versionados.
- [ ] Pods Ready, sem loop de reinício; probes funcionando.
- [ ] Segredos e configurações fora dos manifests.
- [ ] Fluxo end-to-end executado no cluster local.
- [ ] Rollback documentado e testado.
- [ ] `scripts/up.sh` sobe tudo com um comando.

# 15. Sprint 8 — CI/CD, GitOps e acabamento do portfólio

## 15.1 Entregáveis

- GitHub Actions: build, testes (já existente desde a S2) e publicação das imagens no GHCR.
- Argo CD (instalado por manifests no MicroShift) para sincronização GitOps.
- Jenkinsfile como pipeline alternativo demonstrativo.
- Tags/releases; README final; ADRs e diagramas atualizados.
- Screenshots de execução, Grafana, Jaeger e OpenShift; vídeo curto do fluxo de pedido.

## 15.2 Imagens de release

`ghcr.io/<usuario>/commercehub-{user,product,order,inventory}-service:<git-tag>`

## 15.3 Definition of Done

- [x] Push/PR executa build e testes automaticamente (desde a S2).
- [ ] Merge na `main` gera imagem versionada no GHCR com metadados OCI.
- [ ] Argo CD sincroniza o cluster a partir do Git.
- [ ] Jenkinsfile reproduz o build/test principal.
- [ ] README permite entender problema, arquitetura, stack, execução e observabilidade em poucos minutos.
- [ ] Evidências visuais para o portfólio.

# 16. Inventário consolidado de entidades

| Serviço | Tabela | Chave | Finalidade | Sprint |
| --- | --- | --- | --- | --- |
| User | APP_USERS | USER_ID | Cadastro e autenticação demonstrativa | S5 |
| Product | PRODUCTS | PRODUCT_ID | Catálogo e preço atual | S2 (criada) |
| Order | ORDERS | ORDER_ID | Cabeçalho, ciclo de vida e cancelamento | S3 |
| Order | ORDER_ITEMS | ORDER_ITEM_ID | Itens e snapshot de preço/SKU/nome | S3 |
| Order | OUTBOX_EVENTS | EVENT_ID | Mensagens a publicar | S2 (criada) |
| Order | PROCESSED_EVENTS | (EVENT_ID, CONSUMER_NAME) | Idempotência | S2 (criada) |
| Inventory | INVENTORY_ITEMS | INVENTORY_ITEM_ID | Saldo disponível e reservado | S4 |
| Inventory | STOCK_RESERVATIONS | RESERVATION_ID | Reserva por pedido/produto | S4 |
| Inventory | OUTBOX_EVENTS | EVENT_ID | Mensagens a publicar | S2 (criada) |
| Inventory | PROCESSED_EVENTS | (EVENT_ID, CONSUMER_NAME) | Idempotência | S2 (criada) |

## 16.1 Regras de consistência

- Nenhuma FK cruza schema de microsserviço.
- Order Item não referencia Product via banco; validação e snapshot via API.
- Inventory não referencia Order via banco; correlação por `orderId` nas mensagens.
- Preço, SKU e nome aplicados na compra ficam em ORDER_ITEMS.
- `eventId` combinado com `consumer_name` para deduplicação; reentregas não repetem efeitos.
- Mensagens só saem pelo outbox, nunca diretamente da transação de negócio.

# 17. Estrutura de pacotes Java

```
src/main/java/com/commercehub/<service>/
├── api/
│   ├── <Resource>.java
│   └── dto/
├── application/
├── domain/
│   ├── entity/
│   └── enumtype/
├── infrastructure/
│   ├── persistence/
│   ├── messaging/
│   └── client/
├── exception/
├── config/
└── mapper/
src/test/java/com/commercehub/<service>/
├── unit/
├── api/
└── integration/
```

Regra: evitar pacote compartilhado com entidades de domínio; copiar pequenos DTOs de mensagens por contrato
(`docs/events`) para preservar a autonomia dos serviços.

# 18. Estratégia Docker

## 18.1 Dockerfile único das aplicações

```dockerfile
ARG BUILD_IMAGE=eclipse-temurin:21.0.12.1_1-jdk-noble
ARG RUNTIME_IMAGE=registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24

FROM ${BUILD_IMAGE} AS build
ARG SERVICE
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY services/ services/
RUN --mount=type=cache,id=commercehub-m2,target=/root/.m2,sharing=locked \
    ./mvnw -B -ntp -pl "services/${SERVICE}" -am -DskipTests package

FROM ${RUNTIME_IMAGE}
ARG SERVICE
ENV TZ="UTC" \
    JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Duser.timezone=UTC" \
    JAVA_APP_JAR="/deployments/quarkus-run.jar"
COPY --from=build --chown=185 /workspace/services/${SERVICE}/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /workspace/services/${SERVICE}/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /workspace/services/${SERVICE}/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /workspace/services/${SERVICE}/target/quarkus-app/quarkus/ /deployments/quarkus/
EXPOSE 8080
USER 185
ENTRYPOINT ["/opt/jboss/container/java/run/run-java.sh"]
```

Build a partir da raiz: `docker build -f docker/service.Dockerfile --build-arg SERVICE=product-service .`

## 18.2 Docker Compose

| Serviço | Imagem | Sprint |
| --- | --- | --- |
| oracle | `gvenzl/oracle-free:23.26.3-slim-faststart@sha256:...` | S1 |
| user/product/order/inventory-service | build local, perfil `apps` | S1 |
| kafka | `apache/kafka:3.9.1` | S4 |
| jaeger | `cr.jaegertracing.io/jaegertracing/jaeger:2.<x>` | S6 |
| prometheus | `prom/prometheus:<tag fixada>` | S6 |
| grafana | `grafana/grafana:<tag fixada>` | S6 |
| otel-collector | `otel/opentelemetry-collector-contrib:<tag fixada>` | S6 |
| microshift | `ghcr.io/microshift-io/microshift:<tag fixada>` (perfil `openshift`, se validado) | S7 |

# 19. Estratégia de CI/CD e GitOps

```
Pull Request / push --> GitHub Actions: build + unit + API + integration (Oracle Dev Services)   [desde a S2]
main / release      --> image build --> push GHCR                                               [S8]
                    --> GitOps manifests --> Argo CD --> OpenShift local (MicroShift)            [S8]
Jenkinsfile: pipeline alternativa de build/test                                                  [S8]
```

## 19.1 Controles de qualidade

- Falha de compilação ou de testes bloqueia a publicação da imagem.
- Tags de imagem derivam da versão Git/release.
- Secrets no GitHub/OpenShift, nunca no código.
- Pull requests exigem checks verdes antes do merge na `main`.

# 20. Entregáveis finais de portfólio

| Artefato | Conteúdo mínimo | Situação |
| --- | --- | --- |
| GitHub README (inglês) | Problema, solução, arquitetura, stack, execução, endpoints, eventos, testes, observabilidade e deploy | Em evolução desde a S1 |
| Architecture Diagram | Visão lógica + visão de runtime | Parcial (README) |
| ADRs | Decisões com contexto, decisão e consequências | 0001–0009 |
| API examples | Coleção Postman + OpenAPI/Swagger UI | Desde a S2 |
| Event contracts | Envelope, tópicos e exemplos JSON (`docs/events`) | Definido |
| Test evidence | Resumo real dos testes, sem números artificiais | CI desde a S2 |
| Observability evidence | Dashboard Grafana e trace Jaeger reais | S6 |
| OpenShift evidence | Pods/routes/configuração e Topology | S7/S8 |
| CI/CD evidence | GitHub Actions e sync do Argo CD | S2/S8 |
| Demo video | 3–5 min: criar → confirmar → OrderConfirmed → reservar → atualização do pedido → cancelamento com motivo → trace/métricas | S8 |

# 21. Matriz de aceite do MVP

| Capacidade | Evidência de aceite |
| --- | --- |
| Java/Quarkus | Quatro serviços executáveis e README |
| REST | Endpoints documentados (OpenAPI) e testes REST Assured |
| Microservices | Deploy e schemas isolados |
| Kafka | OrderConfirmed e resposta do Inventory, via outbox |
| Idempotência | Teste de evento duplicado sem dupla reserva |
| Compensação | Cancelamento após reserva libera o estoque |
| Oracle | Persistência funcional em Oracle Free, testes em Oracle real |
| JWT | Endpoint protegido e teste de autorização |
| Testing | JUnit/Mockito/REST Assured/integration tests |
| Containers | Imagens versionadas no GHCR |
| OpenShift | Deploy reproduzível no cluster local com probes |
| Observabilidade | Trace e métricas reais |
| CI/CD | Pipeline automática + GitOps |

# 22. Riscos e medidas preventivas

| Risco | Impacto | Mitigação |
| --- | --- | --- |
| Escopo crescer demais | Alto | Payment/Notification fora do MVP; DoD por sprint |
| OpenShift local pesado ou instável sob Docker | Médio/alto | Spike na S7; Podman como alternativa; Oracle e Kafka fora do cluster |
| Memória local (Oracle + Kafka + 4 JVMs + observabilidade + MicroShift) | Médio | Limites de memória no Compose a partir da S4; perfis opcionais |
| Dependência de imagens externas | Médio | Tag + digest fixados; nunca `latest` |
| Exposição de informação profissional | Alto | Domínio fictício, dados sintéticos, arquitetura independente |
| Inglês dificultar comunicação | Médio | Projetos assíncronos e templates técnicos |
| Falhas de integração difíceis de reproduzir | Médio | Testes com containers reais, tracing e `causationId` nas mensagens |
| Estimativa das S4, S6 e S7 | Médio | Revisar prazo ao fim de cada sprint sem cortar o MVP |

# 23. Próximo passo operacional

Iniciar a **Sprint 5 — User Service + JWT** (seção 12), começando pelo DDL de `APP_USERS`, pelo login
demonstrativo e pela proteção dos endpoints de escrita.

- [x] Conta GitHub com 2FA e repositório público `CommerceHub`.
- [x] Projeto Quarkus/Maven com Java 21 e quatro serviços executando.
- [x] Oracle Free em container e schemas criados.
- [x] Product Service completo (S2).
- [x] Tabelas OUTBOX_EVENTS e PROCESSED_EVENTS criadas no ORDER_SCHEMA e INVENTORY_SCHEMA.
- [x] Documento atualizado para v0.4.
- [x] Order Service completo (S3) e documento atualizado para v0.5.
- [x] Kafka + Inventory Service (S4) e documento atualizado para v0.6.
- [ ] Sprint 5 e atualização do documento para v0.7 ao final.

# 24. Fontes oficiais consultadas

Consultas em 29–30/09/2026. Revalidar versões e tags antes de cada release.

- GitHub — Creating an account: <https://docs.github.com/en/account-and-profile/how-tos/account-management/creating-an-account-on-github>
- GitHub — Two-factor authentication: <https://docs.github.com/en/authentication/securing-your-account-with-two-factor-authentication-2fa>
- GitHub — Container registry: <https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry>
- Upwork Help — Sign up as a freelancer: <https://support.upwork.com/hc/en-us/articles/24492534968211-How-to-sign-up-as-a-freelancer-on-Upwork>
- Upwork Help — Profile essentials: <https://support.upwork.com/hc/en-us/articles/36001625225226-How-to-build-your-freelancer-profile-the-essentials>
- Red Hat Developer Sandbox FAQ: <https://developers.redhat.com/developer-sandbox/FAQ>
- gvenzl/oci-oracle-free (imagem `gvenzl/oracle-free`): <https://github.com/gvenzl/oci-oracle-free>
- Quarkus — Dev Services for databases: <https://quarkus.io/guides/databases-dev-services>
- Apache Kafka — Docker image: <https://hub.docker.com/r/apache/kafka>
- Jaeger — Download (v1 fim de vida em 31/12/2025): <https://www.jaegertracing.io/download/>
- Grafana — Docker image: <https://grafana.com/docs/grafana/latest/setup-grafana/configure-docker/>
- MicroShift upstream (OKD): <https://github.com/microshift-io/microshift>
- RFC 9457 — Problem Details for HTTP APIs: <https://www.rfc-editor.org/rfc/rfc9457>

**FIM DO BASELINE v0.6** — CommerceHub pronto para iniciar a Sprint 5.
