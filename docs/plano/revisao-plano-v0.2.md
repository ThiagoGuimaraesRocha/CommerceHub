# Revisão técnica do plano-base v0.2

Data: 29/09/2026. Objetivo: registrar os ajustes recomendados para a versão 0.3 do documento-base.

Veredito: o plano é **viável** e bem delimitado. Os pontos abaixo não impedem o início, mas alguns
precisam ser resolvidos antes da sprint indicada para não gerar retrabalho.

## Correções (inconsistências no documento)

| # | Seção | Problema | Ajuste recomendado |
| --- | --- | --- | --- |
| 1 | 8.5 / 4.1 | O nome `gvenzl/oci-oracle-free` é o repositório GitHub; a imagem Docker é `gvenzl/oracle-free`. | Usar sempre `gvenzl/oracle-free:<tag>@sha256:<digest>`. |
| 2 | 18.1 | O Dockerfile usa `./mvnw` mas copia só `pom.xml` e `src` (o wrapper e o POM pai não entram na imagem). | Build a partir da raiz, copiando `mvnw`, `.mvn/`, POM pai e módulos (feito na Sprint 1). |
| 3 | 9.1 x 9.2 | Entregável fala em "preço positivo", DDL permite `price >= 0`. | Decidir: `CHECK (price > 0)` ou aceitar zero e ajustar o texto. |
| 4 | 5.1 x 11.3 | Princípio exige "envelope de evento", mas o JSON de exemplo é plano. | Definir envelope: `eventId`, `eventType`, `eventVersion`, `occurredAt`, `source`, `correlationId`, `payload`. |
| 5 | 23 | Pede atualizar o documento para "v0.2" ao fim da Sprint 1, mas ele já é v0.2. | Atualizar para v0.3. |
| 6 | 18.1 | `USER 1001` em imagem Temurin não garante compatibilidade com UID arbitrário do OpenShift. | Runtime UBI OpenJDK (ADR 0004) ou permissões de grupo 0 nos diretórios. |

## Lacunas de design (resolver antes da sprint indicada)

| # | Sprint | Lacuna | Recomendação |
| --- | --- | --- | --- |
| 7 | S4 | "Publicar exatamente um evento" ao confirmar pedido é um *dual write* (banco + Kafka). Sem cuidado, um crash entre commit e envio perde ou duplica o evento. | Transactional Outbox: tabela `outbox_events` no `ORDER_SCHEMA`, gravada na mesma transação do pedido e publicada por um job/scheduler. É um diferencial forte para portfólio. |
| 8 | S4 | O fluxo não fecha: ninguém consome `InventoryReserved`/`InventoryReservationFailed`. Os status `PROCESSING`/`COMPLETED` do pedido não têm transição definida. | Order Service consome os eventos de inventário e muda o pedido para `RESERVED`/`REJECTED` (ou equivalente). Documentar a máquina de estados. |
| 9 | S4 | Não existe forma de cadastrar saldo de estoque. | Endpoint administrativo `PUT /inventory/{productId}` e/ou seed de dados de demonstração. |
| 10 | S4 | Falta política de erro no consumidor. | Retry com backoff + tópico DLQ (`*.dlq`) via SmallRye Reactive Messaging; chave da mensagem = `orderId` para manter ordenação. |
| 11 | S2 | Não há ferramenta de migração de DDL. | Flyway (`quarkus-flyway`), um histórico por schema, `migrate-at-start` apenas em dev/test. |
| 12 | S2 | Testes de repositório (`*IT`) precisam de Oracle real. | Quarkus Dev Services/Testcontainers com a mesma imagem `gvenzl/oracle-free` fixada. |
| 13 | S3/S5 | `customerId` vem do corpo da requisição até a S5; depois deveria vir do JWT. | Registrar como decisão temporária e ajustar na S5. |
| 14 | S2 | Valores monetários em JSON como número podem perder precisão em clientes. | Serializar `BigDecimal` como string ou documentar a escala (4 casas). |
| 15 | S3 | `ProductClient` sem política definida. | MicroProfile REST Client + Fault Tolerance (`@Timeout`, `@Retry`, `@CircuitBreaker`). |
| 16 | S2 | `updated_at` só tem `DEFAULT`; não atualiza sozinho. | Atualizar pela aplicação (`@UpdateTimestamp` ou `@PreUpdate`). |

## Atualizações de versões

| Item | No plano | Situação em 29/09/2026 |
| --- | --- | --- |
| Oracle Free | `23.26.2-full` | `23.26.3` disponível; adotado `23.26.3-slim-faststart` com digest. |
| Jaeger | `jaegertracing/all-in-one:1.76.0` | A linha 1.x do Jaeger foi descontinuada em favor do Jaeger v2 (`jaegertracing/jaeger:2.x`). Revalidar na S6. |
| Quarkus | não fixado | 3.33.x (LTS) adotado. |
| Java | 21 | Mantido. Java 25 (LTS) é opção futura. |

## Riscos de ambiente

- **Red Hat Developer Sandbox (S7)**: tem cotas de CPU/memória/armazenamento e hiberna recursos. Oracle Free +
  Kafka + observabilidade dificilmente cabem juntos. Plano B: Oracle e Kafka rodando fora do cluster
  (máquina local exposta de forma segura) ou OpenShift Local (CRC, exige ~16 GB de RAM) para as evidências.
- **Memória local**: Oracle (~2-3 GB) + Kafka + 4 serviços JVM + Grafana/Prometheus/Jaeger somam 6-8 GB.
  Definir `-Xmx` e `mem_limit` no Compose a partir da S4.
- **Estimativa de 8-10 semanas a 20 h/semana**: realista para S1-S3 e S5; S4 (outbox + idempotência + DLQ),
  S6 e S7 tendem a exceder a estimativa. Manter o MVP e aceitar que o prazo dessas sprints seja revisado.

## Sugestões de valor para o portfólio

- Coleção de requisições (`.http` ou Postman) versionada desde a S2.
- OpenAPI/Swagger UI por serviço desde a S2.
- Workflow simples de CI (`./mvnw verify`) já a partir da S2, para ter o badge verde no README cedo;
  a publicação no GHCR e o GitOps continuam na S8.
- README em inglês (público da Upwork), documentação interna pode continuar em português.
