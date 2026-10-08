package com.commercehub.inventory.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercehub.inventory.application.InventoryService;
import com.commercehub.inventory.domain.entity.InventoryItemEntity;
import com.commercehub.inventory.infrastructure.messaging.EventEnvelope;
import com.commercehub.inventory.infrastructure.messaging.MessageSerde;
import com.commercehub.inventory.infrastructure.messaging.outbox.OutboxEventEntity;
import com.commercehub.inventory.infrastructure.messaging.outbox.OutboxRepository;
import com.commercehub.inventory.infrastructure.messaging.payload.OrderConfirmedPayload;
import com.commercehub.inventory.infrastructure.persistence.InventoryRepository;
import com.commercehub.inventory.support.AwaitAssertions;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrderEventConsumerTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    InventoryService inventoryService;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    OutboxRepository outboxRepository;

    @Inject
    MessageSerde serde;

    @Test
    void orderConfirmedReservesStockAndWritesOutbox() {
        String productId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        inventoryService.setStock(productId, 10);

        InMemorySource<String> source = connector.source("order-events");
        source.send(confirmed(orderId, productId, 3));

        AwaitAssertions.untilAsserted(() -> {
            InventoryItemEntity item = inventoryRepository.findByProductId(productId).orElseThrow();
            assertThat(item.getAvailableQty()).isEqualTo(7);
            assertThat(item.getReservedQty()).isEqualTo(3);
            assertThat(outboxRepository.list("aggregateId = ?1 and eventType = ?2", orderId, "InventoryReserved"))
                    .isNotEmpty();
        });
    }

    @Test
    void insufficientStockPublishesFailureWithoutReserving() {
        String productId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        inventoryService.setStock(productId, 1);

        connector.source("order-events").send(confirmed(orderId, productId, 5));

        AwaitAssertions.untilAsserted(() -> {
            InventoryItemEntity item = inventoryRepository.findByProductId(productId).orElseThrow();
            assertThat(item.getAvailableQty()).isEqualTo(1);
            assertThat(item.getReservedQty()).isZero();
            List<OutboxEventEntity> failed =
                    outboxRepository.list("aggregateId = ?1 and eventType = ?2", orderId, "InventoryReservationFailed");
            assertThat(failed).isNotEmpty();
        });
    }

    private String confirmed(String orderId, String productId, long quantity) {
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID().toString(),
                "OrderConfirmed",
                EventEnvelope.KIND_EVENT,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "order-service",
                "Order",
                orderId,
                orderId,
                null,
                serde.toTree(new OrderConfirmedPayload(
                        orderId, "cust", List.of(new OrderConfirmedPayload.Item(productId, quantity)),
                        new BigDecimal("10.0000"), "BRL")));
        return serde.toJson(envelope);
    }
}
