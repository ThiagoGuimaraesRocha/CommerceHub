package com.commercehub.order.application;

import com.commercehub.order.domain.entity.OrderEntity;
import com.commercehub.order.domain.enumtype.CancellationReason;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.logging.Logger;

/**
 * Applies the inbound half of the order saga: reacts to inventory events and advances or compensates the
 * order. Each message is deduplicated by {@code (eventId, consumerName)} in the same transaction as its
 * effect, so redeliveries are harmless.
 */
@ApplicationScoped
public class OrderSagaService {

    private static final Logger LOG = Logger.getLogger(OrderSagaService.class);
    private static final String CONSUMER = KafkaTopics.INVENTORY_EVENTS_CONSUMER;

    static final String EVENT_INVENTORY_RESERVED = "InventoryReserved";
    static final String EVENT_INVENTORY_RESERVATION_FAILED = "InventoryReservationFailed";
    static final String EVENT_INVENTORY_RELEASED = "InventoryReleased";

    private final OrderRepository repository;
    private final OrderStateTransitionService transitions;
    private final OrderCancellationService cancellationService;
    private final OutboxWriter outboxWriter;
    private final EventIdempotencyService idempotency;
    private final MessageSerde serde;

    public OrderSagaService(
            OrderRepository repository,
            OrderStateTransitionService transitions,
            OrderCancellationService cancellationService,
            OutboxWriter outboxWriter,
            EventIdempotencyService idempotency,
            MessageSerde serde) {
        this.repository = repository;
        this.transitions = transitions;
        this.cancellationService = cancellationService;
        this.outboxWriter = outboxWriter;
        this.idempotency = idempotency;
        this.serde = serde;
    }

    /**
     * Handles one inventory event. A fresh transaction is started per retry attempt; only technical
     * failures throw (and are retried, then dead-lettered). Business outcomes never throw here.
     */
    @Retry(maxRetries = 4, delay = 200, jitter = 100, retryOn = Exception.class)
    @Transactional
    public void handleInventoryEvent(EventEnvelope envelope) {
        if (idempotency.alreadyProcessed(envelope.eventId(), CONSUMER)) {
            LOG.debugf("Skipping already-processed %s %s", envelope.eventType(), envelope.eventId());
            return;
        }
        switch (envelope.eventType()) {
            case EVENT_INVENTORY_RESERVED -> onInventoryReserved(envelope);
            case EVENT_INVENTORY_RESERVATION_FAILED -> onInventoryReservationFailed(envelope);
            case EVENT_INVENTORY_RELEASED -> onInventoryReleased(envelope);
            default -> LOG.debugf("Ignoring unsupported inventory event %s", envelope.eventType());
        }
        idempotency.markProcessed(envelope.eventId(), CONSUMER, envelope.eventType());
    }

    private void onInventoryReserved(EventEnvelope envelope) {
        InventoryReservedPayload payload = serde.payloadAs(envelope, InventoryReservedPayload.class);
        OrderEntity order = repository.findByIdOptional(payload.orderId()).orElse(null);
        if (order == null) {
            LOG.warnf("InventoryReserved for unknown order %s", payload.orderId());
            return;
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            transitions.transition(order, OrderStatus.INVENTORY_RESERVED);
            LOG.infof("Order %s reserved -> INVENTORY_RESERVED", order.getId());
        } else {
            LOG.infof("Ignoring InventoryReserved for order %s in status %s", order.getId(), order.getStatus());
        }
    }

    private void onInventoryReservationFailed(EventEnvelope envelope) {
        InventoryReservationFailedPayload payload = serde.payloadAs(envelope, InventoryReservationFailedPayload.class);
        OrderEntity order = repository.findByIdOptional(payload.orderId()).orElse(null);
        if (order == null) {
            LOG.warnf("InventoryReservationFailed for unknown order %s", payload.orderId());
            return;
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            LOG.infof("Ignoring InventoryReservationFailed for order %s in status %s", order.getId(), order.getStatus());
            return;
        }
        CancellationReason reason = mapFailureReason(payload.reason());
        cancellationService.cancelBySystem(order, reason);
        outboxWriter.writeEvent(
                "OrderCancelled",
                KafkaTopics.ORDER_EVENTS,
                order.getId(),
                envelope.eventId(),
                new OrderCancelledPayload(order.getId(), OrderStatus.CONFIRMED.name(), reason.name(), "SYSTEM"));
        LOG.infof("Order %s cancelled by SYSTEM (%s)", order.getId(), reason);
    }

    private void onInventoryReleased(EventEnvelope envelope) {
        LOG.debugf("InventoryReleased acknowledged for %s (order already cancelled)", envelope.aggregateId());
    }

    private static CancellationReason mapFailureReason(String reason) {
        if (InventoryReservationFailedPayload.REASON_UNKNOWN_PRODUCT.equals(reason)) {
            return CancellationReason.UNKNOWN_PRODUCT;
        }
        return CancellationReason.INSUFFICIENT_STOCK;
    }
}
