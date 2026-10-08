package com.commercehub.order.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.enumtype.CancellationReason;
import com.commercehub.order.domain.enumtype.CancelledBy;
import com.commercehub.order.domain.enumtype.OrderStatus;
import com.commercehub.order.infrastructure.messaging.EventEnvelope;
import com.commercehub.order.infrastructure.messaging.MessageSerde;
import com.commercehub.order.infrastructure.messaging.payload.InventoryReservationFailedPayload;
import com.commercehub.order.infrastructure.messaging.payload.InventoryReservedPayload;
import com.commercehub.order.infrastructure.persistence.OrderRepository;
import com.commercehub.order.support.AwaitAssertions;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InventoryEventConsumerTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    OrderRepository orderRepository;

    @Inject
    MessageSerde serde;

    @Test
    void inventoryReservedMovesOrder() {
        OrderEntity order = persistConfirmedOrder();

        connector.source("inventory-events").send(reserved(order.getId()));

        AwaitAssertions.untilAsserted(() -> {
            OrderEntity reloaded = orderRepository.findById(order.getId());
            assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
        });
    }

    @Test
    void inventoryReservationFailedCancelsBySystem() {
        OrderEntity order = persistConfirmedOrder();

        connector.source("inventory-events").send(failed(order.getId()));

        AwaitAssertions.untilAsserted(() -> {
            OrderEntity reloaded = orderRepository.findById(order.getId());
            assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(reloaded.getCancelledBy()).isEqualTo(CancelledBy.SYSTEM);
            assertThat(reloaded.getCancellationReason()).isEqualTo(CancellationReason.INSUFFICIENT_STOCK);
        });
    }

    private OrderEntity persistConfirmedOrder() {
        return QuarkusTransaction.requiringNew().call(() -> {
            OrderEntity order = OrderEntity.newOrder();
            order.setCustomerId("11111111-1111-1111-1111-111111111111");
            order.setCurrencyCode("BRL");
            order.setTotalAmount(new BigDecimal("10.0000"));
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.persistAndFlush(order);
            return order;
        });
    }

    private String reserved(String orderId) {
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID().toString(),
                "InventoryReserved",
                EventEnvelope.KIND_EVENT,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "inventory-service",
                "Inventory",
                orderId,
                orderId,
                UUID.randomUUID().toString(),
                serde.toTree(new InventoryReservedPayload(orderId, List.of(
                        new InventoryReservedPayload.Reservation(UUID.randomUUID().toString(), "p1", 1)))));
        return serde.toJson(envelope);
    }

    private String failed(String orderId) {
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID().toString(),
                "InventoryReservationFailed",
                EventEnvelope.KIND_EVENT,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "inventory-service",
                "Inventory",
                orderId,
                orderId,
                UUID.randomUUID().toString(),
                serde.toTree(new InventoryReservationFailedPayload(orderId, "INSUFFICIENT_STOCK", List.of(
                        new InventoryReservationFailedPayload.UnavailableItem("p1", 2, 0)))));
        return serde.toJson(envelope);
    }
}
