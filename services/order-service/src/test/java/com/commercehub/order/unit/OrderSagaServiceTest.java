package com.commercehub.order.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercehub.order.application.OrderCancellationService;
import com.commercehub.order.application.OrderSagaService;
import com.commercehub.order.application.OrderStateTransitionService;
import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.enumtype.CancellationReason;
import com.commercehub.order.domain.enumtype.CancelledBy;
import com.commercehub.order.domain.enumtype.OrderStatus;
import com.commercehub.order.infrastructure.messaging.EventEnvelope;
import com.commercehub.order.infrastructure.messaging.KafkaTopics;
import com.commercehub.order.infrastructure.messaging.MessageSerde;
import com.commercehub.order.infrastructure.messaging.consumer.EventIdempotencyService;
import com.commercehub.order.infrastructure.messaging.outbox.OutboxWriter;
import com.commercehub.order.infrastructure.messaging.payload.InventoryReservationFailedPayload;
import com.commercehub.order.infrastructure.messaging.payload.InventoryReservedPayload;
import com.commercehub.order.infrastructure.messaging.payload.OrderCancelledPayload;
import com.commercehub.order.infrastructure.persistence.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderSagaServiceTest {

    @Mock
    OrderRepository repository;

    @Mock
    OutboxWriter outboxWriter;

    @Mock
    EventIdempotencyService idempotency;

    OrderSagaService saga;
    MessageSerde serde;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        serde = new MessageSerde(mapper);
        OrderStateTransitionService transitions = new OrderStateTransitionService();
        saga = new OrderSagaService(
                repository,
                transitions,
                new OrderCancellationService(transitions),
                outboxWriter,
                idempotency,
                serde);
    }

    @Test
    void inventoryReservedMovesConfirmedOrder() {
        OrderEntity order = OrderEntity.newOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        when(repository.findByIdOptional(order.getId())).thenReturn(Optional.of(order));

        saga.handleInventoryEvent(envelope("InventoryReserved", order.getId(),
                new InventoryReservedPayload(order.getId(), List.of(
                        new InventoryReservedPayload.Reservation(UUID.randomUUID().toString(), "p1", 1)))));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
        verify(idempotency).markProcessed(any(), eq(KafkaTopics.INVENTORY_EVENTS_CONSUMER), eq("InventoryReserved"));
    }

    @Test
    void inventoryReservationFailedCancelsAndPublishesOrderCancelled() {
        OrderEntity order = OrderEntity.newOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        when(repository.findByIdOptional(order.getId())).thenReturn(Optional.of(order));

        EventEnvelope envelope = envelope("InventoryReservationFailed", order.getId(),
                new InventoryReservationFailedPayload(order.getId(), "INSUFFICIENT_STOCK", List.of(
                        new InventoryReservationFailedPayload.UnavailableItem("p1", 2, 0))));
        saga.handleInventoryEvent(envelope);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledBy()).isEqualTo(CancelledBy.SYSTEM);
        assertThat(order.getCancellationReason()).isEqualTo(CancellationReason.INSUFFICIENT_STOCK);
        ArgumentCaptor<OrderCancelledPayload> payload = ArgumentCaptor.forClass(OrderCancelledPayload.class);
        verify(outboxWriter).writeEvent(
                eq("OrderCancelled"), eq(KafkaTopics.ORDER_EVENTS), eq(order.getId()), eq(envelope.eventId()), payload.capture());
        assertThat(payload.getValue().cancelledBy()).isEqualTo("SYSTEM");
    }

    @Test
    void duplicateEventIsIgnored() {
        String eventId = UUID.randomUUID().toString();
        when(idempotency.alreadyProcessed(eventId, KafkaTopics.INVENTORY_EVENTS_CONSUMER)).thenReturn(true);

        saga.handleInventoryEvent(new EventEnvelope(
                eventId, "InventoryReserved", "EVENT", 1, OffsetDateTime.now(ZoneOffset.UTC),
                "inventory-service", "Inventory", "order", "order", null, serde.toTree(new InventoryReservedPayload("order", List.of()))));

        verify(repository, never()).findByIdOptional(any());
        verify(idempotency, never()).markProcessed(any(), any(), any());
    }

    @Test
    void unknownProductFailureUsesUnknownProductReason() {
        OrderEntity order = OrderEntity.newOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        when(repository.findByIdOptional(order.getId())).thenReturn(Optional.of(order));

        saga.handleInventoryEvent(envelope("InventoryReservationFailed", order.getId(),
                new InventoryReservationFailedPayload(order.getId(), "UNKNOWN_PRODUCT", List.of(
                        new InventoryReservationFailedPayload.UnavailableItem("missing", 1, 0)))));

        assertThat(order.getCancellationReason()).isEqualTo(CancellationReason.UNKNOWN_PRODUCT);
    }

    private EventEnvelope envelope(String type, String orderId, Object payload) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                type,
                EventEnvelope.KIND_EVENT,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "inventory-service",
                "Inventory",
                orderId,
                orderId,
                UUID.randomUUID().toString(),
                serde.toTree(payload));
    }
}
