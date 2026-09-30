# Revisão técnica do plano-base v0.3

Data: 29/09/2026. Registra as decisões tomadas sobre a revisão da v0.2 e os ajustes que ainda faltam no
documento para a versão 0.4.

## Decisões registradas (revisão v0.2)

| # | Tema | Decisão | Onde está implementado/registrado |
| --- | --- | --- | --- |
| 1 | Nome da imagem Oracle | `gvenzl/oracle-free:<tag>@sha256:<digest>` | `docker-compose.yml`, ADR 0003 |
| 2 | Dockerfile | Build a partir da raiz com wrapper e POM pai | `docker/service.Dockerfile`, ADR 0004 |
| 3 | Preço | Zero é aceito (`price >= 0`) | Product Service, ADR 0008 |
| 4 | Envelope de evento | Envelope único para eventos e comandos, inferido das seções 5.4–5.6 e 16.1 | `docs/events`, ADR 0005 |
| 5 | Versão do documento | v0.3 publicada | — |
| 6 | Runtime | UBI OpenJDK 21 | ADR 0004 |
| 7 | Dual write | Transactional Outbox em Order e Inventory | ADR 0006 |
| 8 | Fechamento do fluxo | Saga formalizada no documento v0.3 | ADR 0006, `docs/events` |
| 9 | Saldo de estoque | Endpoint administrativo + carga de dados por esse endpoint | Planejado para a S4 (abaixo) |
| 10 | Erros no consumidor | Retry com backoff + DLQ `<tópico>.dlq` | ADR 0006 |
| 11 | Migrações | Flyway por schema | Product Service, ADR 0007 |
| 12 | Testes com Oracle | Dev Services com a mesma imagem fixada | Product Service, ADR 0007 |
| 13 | `customerId` | No corpo até a S5, depois vem do JWT | ADR 0008 |
| 14 | Dinheiro em JSON | Número com escala 4 documentada | ADR 0008, `docs/events` |
| 15 | `ProductClient` | REST Client + Fault Tolerance | ADR 0008 |
| 16 | `updated_at` | `@UpdateTimestamp` | Product Service |

### Item 9 — endpoint administrativo de estoque (Sprint 4)

- `PUT /api/v1/inventory/{productId}` com `{ "availableQuantity": 50 }`: cria ou ajusta o saldo (idempotente).
- `GET /api/v1/inventory/{productId}`: saldo disponível e reservado.
- Carga de demonstração: `data/seed/inventory.ndjson` (por SKU) e `scripts/seed.sh`, que busca o `productId`
  pelo SKU no Product Service e chama o endpoint administrativo. Nunca grava direto no banco.
- Até a S5 o endpoint é aberto; na S5 passa a exigir role `ADMIN`.

## Ajustes pendentes no documento v0.3

Todos os itens A–M foram aplicados em
[`CommerceHub_Plano_Base_v0.4.md`](CommerceHub_Plano_Base_v0.4.md). Na v0.4, o motivo de cancelamento (H)
também pode ser escolhido pelo cliente, e `OUTBOX_EVENTS`/`PROCESSED_EVENTS` (I) já foram criadas via
Flyway nos schemas de Order e Inventory.

| # | Seção | Problema | Ajuste aplicado na v0.4 |
| --- | --- | --- | --- |
| A | 1 | A linha "0.3+ A preencher" continua antes da linha 0.3. | Trocar por "0.4+". |
| B | 23 | Ainda pede "atualizar para a versão 0.2" e mostra a Sprint 1 como não iniciada; o rodapé diz "pronto para iniciar a Sprint 1". | Marcar S1 e S2 concluídas e apontar a S3 como próxima. |
| C | 4.1, 8.5, 11.5, 14.4, 18.2 | Continua `gvenzl/oci-oracle-free` e `23.26.2-full`. | `gvenzl/oracle-free:23.26.3-slim-faststart` com digest (decisão 1). |
| D | 9.1 | "Validação de preço positivo". | "Preço maior ou igual a zero" (decisão 3). |
| E | 9.4, 18.1 | Modelo de Dockerfile com `maven`/Temurin JRE e `USER 1001`. | Dockerfile único com UBI OpenJDK (decisões 2 e 6). |
| F | 11.3 | Exemplo de evento plano com `ORDER_CONFIRMED`. | Envelope de `docs/events` com `eventType: "OrderConfirmed"` (decisão 4). |
| G | 13.4, 14.4, 18.2 | `jaegertracing/all-in-one:1.76.0` (Jaeger v1, fim de vida em 31/12/2025). | Jaeger v2 com tag fixada na S6. |
| H | 10.2 | O fluxo 5.3 cancela com motivo `INSUFFICIENT_STOCK`, mas `ORDERS` não tem coluna para o motivo. | Adicionar `cancellation_reason VARCHAR2(40 CHAR)`. |
| I | 10.2, 11.2 | Order Service agora também consome eventos, mas só o `INVENTORY_SCHEMA` tem `PROCESSED_EVENTS`; faltam as tabelas de outbox. | `PROCESSED_EVENTS` e `OUTBOX_EVENTS` nos dois schemas (DDL no ADR 0006). |
| J | 10.3 | S3 lista `InventoryEventConsumer`, `InventoryReservedEvent` etc., mas Kafka só entra na S4. | Mover as classes de mensageria para a S4; na S3 fica só `OrderStateTransitionService` com `CREATED -> CONFIRMED`. |
| K | 11.4 | Inventory Service não lista o consumidor do comando `ReleaseInventory` nem o endpoint administrativo. | Adicionar `InventoryCommandConsumer`, `InventoryAdminResource`, `SetStockRequest`. |
| L | 22 | Risco "Ambiente OpenShift limitado" ainda aponta só para o Sandbox. | OpenShift local com MicroShift/OKD (ADR 0009). |
| M | 5.2 | Order Service descrito como "Kafka producer"; agora também é consumer. | "REST + Kafka producer/consumer + REST client". |

## OpenShift local junto com o docker-compose

Resumo do ADR 0009:

- **É possível ter OpenShift local subindo junto**, usando o MicroShift upstream (construído a partir do
  OKD, a distribuição comunitária do OpenShift). Ele roda como um container privilegiado, sem conta nem
  pull secret da Red Hat, e oferece as APIs do OpenShift que o projeto usa (Routes e o modelo de segurança
  SCC).
- **Não é um serviço comum do Compose** como o Oracle: é um nó completo (systemd, CRI-O, rede do cluster).
  O projeto upstream documenta Podman; rodar sob Docker precisa ser validado no spike da S7.
- O desenho proposto é: Oracle e Kafka continuam no Compose; os 4 serviços são implantados **dentro** do
  MicroShift a partir dos manifests; um único comando (`scripts/up.sh`) sobe tudo.
- O MicroShift não tem console web. Para as capturas de tela da visão Topology do portfólio, usar o
  OpenShift Local (CRC) ou o Developer Sandbox só no momento das evidências.
- Requisitos estimados: Linux com Podman (ou Podman machine no macOS/Windows), +2–4 GB de RAM além do Compose.
