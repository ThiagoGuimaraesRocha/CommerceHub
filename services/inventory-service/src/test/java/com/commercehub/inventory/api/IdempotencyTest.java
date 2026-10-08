package com.commercehub.inventory.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercehub.inventory.application.InventorySagaService;
import com.commercehub.inventory.application.InventoryService;
import com.commercehub.inventory.domain.entity.InventoryItemEntity;
import com.commercehub.inventory.infrastructure.messaging.EventEnvelope;
import com.commercehub.inventory.infrastructure.messaging.MessageSerde;
import com.commercehub.inventory.infrastructure.messaging.payload.OrderConfirmedPayload;
import com.commercehub.inventory.infrastructure.persistence.InventoryRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class IdempotencyTest {

    @Inject
    InventorySagaService saga;

    @Inject
    InventoryService inventoryService;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    MessageSerde serde;

    @Test
    void duplicateOrderConfirmedDoesNotReserveTwice() {
        String productId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        inventoryService.setStock(productId, 10);

        EventEnvelope envelope = confirmed(orderId, productId, 2);
        saga.handleOrderEvent(envelope);
        saga.handleOrderEvent(envelope);

        InventoryItemEntity item = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(item.getReservedQty()).isEqualTo(2);
        assertThat(item.getAvailableQty()).isEqualTo(8);
    }

    private EventEnvelope confirmed(String orderId, String productId, long quantity) {
        return new EventEnvelope(
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
    }
}
