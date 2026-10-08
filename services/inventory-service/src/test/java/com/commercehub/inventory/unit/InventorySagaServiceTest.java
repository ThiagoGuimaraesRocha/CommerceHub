package com.commercehub.inventory.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercehub.inventory.application.InventorySagaService;
import com.commercehub.inventory.application.ReservationOutcome;
import com.commercehub.inventory.application.StockReservationService;
import com.commercehub.inventory.infrastructure.messaging.EventEnvelope;
import com.commercehub.inventory.infrastructure.messaging.KafkaTopics;
import com.commercehub.inventory.infrastructure.messaging.MessageSerde;
import com.commercehub.inventory.infrastructure.messaging.consumer.EventIdempotencyService;
import com.commercehub.inventory.infrastructure.messaging.outbox.OutboxWriter;
import com.commercehub.inventory.infrastructure.observability.BusinessMetrics;
import com.commercehub.inventory.infrastructure.messaging.payload.InventoryReleasedPayload;
import com.commercehub.inventory.infrastructure.messaging.payload.InventoryReservationFailedPayload;
import com.commercehub.inventory.infrastructure.messaging.payload.InventoryReservedPayload;
import com.commercehub.inventory.infrastructure.messaging.payload.OrderConfirmedPayload;
import com.commercehub.inventory.infrastructure.messaging.payload.ReleaseInventoryPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventorySagaServiceTest {

    @Mock
    StockReservationService reservationService;

    @Mock
    OutboxWriter outboxWriter;

    @Mock
    EventIdempotencyService idempotency;

    @Mock
    BusinessMetrics metrics;

    InventorySagaService saga;
    MessageSerde serde;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        serde = new MessageSerde(mapper);
        saga = new InventorySagaService(reservationService, outboxWriter, idempotency, serde, metrics);
    }

    @Test
    void orderConfirmedPublishesInventoryReserved() {
        String orderId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();
        when(reservationService.reserve(eq(orderId), any())).thenReturn(ReservationOutcome.reserved(List.of(
                new ReservationOutcome.ReservedLine("r1", productId, 2))));

        EventEnvelope envelope = envelope("OrderConfirmed", orderId, new OrderConfirmedPayload(
                orderId, "cust", List.of(new OrderConfirmedPayload.Item(productId, 2)), new BigDecimal("21.0000"), "BRL"));
        saga.handleOrderEvent(envelope);

        ArgumentCaptor<InventoryReservedPayload> payload = ArgumentCaptor.forClass(InventoryReservedPayload.class);
        verify(outboxWriter).writeEvent(
                eq("InventoryReserved"), eq(KafkaTopics.INVENTORY_EVENTS), eq(orderId), eq(envelope.eventId()), payload.capture());
        assertThat(payload.getValue().reservations()).hasSize(1);
        verify(idempotency).markProcessed(envelope.eventId(), KafkaTopics.ORDER_EVENTS_CONSUMER, "OrderConfirmed");
    }

    @Test
    void orderConfirmedPublishesFailureWhenShort() {
        String orderId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();
        when(reservationService.reserve(eq(orderId), any())).thenReturn(ReservationOutcome.failed(
                ReservationOutcome.REASON_INSUFFICIENT_STOCK,
                List.of(new ReservationOutcome.UnavailableLine(productId, 5, 1))));

        EventEnvelope envelope = envelope("OrderConfirmed", orderId, new OrderConfirmedPayload(
                orderId, "cust", List.of(new OrderConfirmedPayload.Item(productId, 5)), new BigDecimal("50.0000"), "BRL"));
        saga.handleOrderEvent(envelope);

        ArgumentCaptor<InventoryReservationFailedPayload> payload =
                ArgumentCaptor.forClass(InventoryReservationFailedPayload.class);
        verify(outboxWriter).writeEvent(
                eq("InventoryReservationFailed"), eq(KafkaTopics.INVENTORY_EVENTS), eq(orderId),
                eq(envelope.eventId()), payload.capture());
        assertThat(payload.getValue().reason()).isEqualTo("INSUFFICIENT_STOCK");
    }

    @Test
    void releasePublishesInventoryReleased() {
        String orderId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();
        when(reservationService.release(orderId)).thenReturn(List.of(
                new ReservationOutcome.ReservedLine("r1", productId, 2)));

        EventEnvelope envelope = command("ReleaseInventory", orderId,
                new ReleaseInventoryPayload(orderId, "CUSTOMER_CANCELLED"));
        saga.handleInventoryCommand(envelope);

        ArgumentCaptor<InventoryReleasedPayload> payload = ArgumentCaptor.forClass(InventoryReleasedPayload.class);
        verify(outboxWriter).writeEvent(
                eq("InventoryReleased"), eq(KafkaTopics.INVENTORY_EVENTS), eq(orderId), eq(envelope.eventId()), payload.capture());
        assertThat(payload.getValue().releasedItems().getFirst().quantity()).isEqualTo(2);
    }

    @Test
    void releaseWithoutReservedLinesPublishesNothing() {
        String orderId = UUID.randomUUID().toString();
        when(reservationService.release(orderId)).thenReturn(List.of());

        saga.handleInventoryCommand(command("ReleaseInventory", orderId,
                new ReleaseInventoryPayload(orderId, "CUSTOMER_CANCELLED")));

        verify(outboxWriter, never()).writeEvent(any(), any(), any(), any(), any());
        verify(idempotency).markProcessed(any(), eq(KafkaTopics.INVENTORY_COMMANDS_CONSUMER), eq("ReleaseInventory"));
    }

    @Test
    void duplicateOrderEventIsSkipped() {
        String eventId = UUID.randomUUID().toString();
        when(idempotency.alreadyProcessed(eventId, KafkaTopics.ORDER_EVENTS_CONSUMER)).thenReturn(true);

        saga.handleOrderEvent(new EventEnvelope(
                eventId, "OrderConfirmed", "EVENT", 1, OffsetDateTime.now(ZoneOffset.UTC),
                "order-service", "Order", "o", "o", null, serde.toTree(new OrderConfirmedPayload("o", "c", List.of(), BigDecimal.ZERO, "BRL"))));

        verify(reservationService, never()).reserve(any(), any());
    }

    private EventEnvelope envelope(String type, String orderId, Object payload) {
        return new EventEnvelope(
                UUID.randomUUID().toString(), type, EventEnvelope.KIND_EVENT, 1,
                OffsetDateTime.now(ZoneOffset.UTC), "order-service", "Order", orderId, orderId, null, serde.toTree(payload));
    }

    private EventEnvelope command(String type, String orderId, Object payload) {
        return new EventEnvelope(
                UUID.randomUUID().toString(), type, EventEnvelope.KIND_COMMAND, 1,
                OffsetDateTime.now(ZoneOffset.UTC), "order-service", "Order", orderId, orderId, null, serde.toTree(payload));
    }
}
