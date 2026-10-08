package com.commercehub.inventory.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercehub.inventory.application.InventorySagaService;
import com.commercehub.inventory.application.InventoryService;
import com.commercehub.inventory.application.ReservationRequest;
import com.commercehub.inventory.application.StockReservationService;
import com.commercehub.inventory.domain.entity.InventoryItemEntity;
import com.commercehub.inventory.infrastructure.messaging.EventEnvelope;
import com.commercehub.inventory.infrastructure.messaging.MessageSerde;
import com.commercehub.inventory.infrastructure.messaging.payload.ReleaseInventoryPayload;
import com.commercehub.inventory.infrastructure.persistence.InventoryRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InventoryCommandConsumerTest {

    @Inject
    InventorySagaService saga;

    @Inject
    InventoryService inventoryService;

    @Inject
    StockReservationService reservationService;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    MessageSerde serde;

    @Test
    void releaseInventoryRestoresAvailableQuantity() {
        String productId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        inventoryService.setStock(productId, 10);
        QuarkusTransaction.requiringNew().run(() ->
                reservationService.reserve(orderId, List.of(new ReservationRequest(productId, 4))));

        saga.handleInventoryCommand(release(orderId));

        InventoryItemEntity item = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(item.getAvailableQty()).isEqualTo(10);
        assertThat(item.getReservedQty()).isZero();
    }

    private EventEnvelope release(String orderId) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                "ReleaseInventory",
                EventEnvelope.KIND_COMMAND,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "order-service",
                "Order",
                orderId,
                orderId,
                null,
                serde.toTree(new ReleaseInventoryPayload(orderId, "CUSTOMER_CANCELLED")));
    }
}
