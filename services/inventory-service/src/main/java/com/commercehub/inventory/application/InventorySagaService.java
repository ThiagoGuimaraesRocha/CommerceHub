package com.commercehub.inventory.application;

import com.commercehub.inventory.infrastructure.messaging.EventEnvelope;
import com.commercehub.inventory.infrastructure.messaging.KafkaTopics;
import com.commercehub.inventory.infrastructure.messaging.MessageSerde;
import com.commercehub.inventory.infrastructure.messaging.consumer.EventIdempotencyService;
import com.commercehub.inventory.infrastructure.messaging.outbox.OutboxWriter;
import com.commercehub.inventory.infrastructure.messaging.payload.InventoryReleasedPayload;
import com.commercehub.inventory.infrastructure.messaging.payload.InventoryReservationFailedPayload;
import com.commercehub.inventory.infrastructure.messaging.payload.InventoryReservedPayload;
import com.commercehub.inventory.infrastructure.messaging.payload.OrderConfirmedPayload;
import com.commercehub.inventory.infrastructure.messaging.payload.ReleaseInventoryPayload;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.logging.Logger;

/**
 * Inbound half of the saga for the Inventory Service: reserves stock when an order is confirmed and
 * releases it on command. Each message is deduplicated by {@code (eventId, consumerName)} in the same
 * transaction as its effect and its outbox reply, so redeliveries never double-reserve or double-release.
 */
@ApplicationScoped
public class InventorySagaService {

    private static final Logger LOG = Logger.getLogger(InventorySagaService.class);

    static final String EVENT_ORDER_CONFIRMED = "OrderConfirmed";
    static final String EVENT_INVENTORY_RESERVED = "InventoryReserved";
    static final String EVENT_INVENTORY_RESERVATION_FAILED = "InventoryReservationFailed";
    static final String EVENT_INVENTORY_RELEASED = "InventoryReleased";
    static final String COMMAND_RELEASE_INVENTORY = "ReleaseInventory";

    private final StockReservationService reservationService;
    private final OutboxWriter outboxWriter;
    private final EventIdempotencyService idempotency;
    private final MessageSerde serde;

    public InventorySagaService(
            StockReservationService reservationService,
            OutboxWriter outboxWriter,
            EventIdempotencyService idempotency,
            MessageSerde serde) {
        this.reservationService = reservationService;
        this.outboxWriter = outboxWriter;
        this.idempotency = idempotency;
        this.serde = serde;
    }

    @Retry(maxRetries = 4, delay = 200, jitter = 100, retryOn = Exception.class)
    @Transactional
    public void handleOrderEvent(EventEnvelope envelope) {
        String consumer = KafkaTopics.ORDER_EVENTS_CONSUMER;
        if (idempotency.alreadyProcessed(envelope.eventId(), consumer)) {
            LOG.debugf("Skipping already-processed %s %s", envelope.eventType(), envelope.eventId());
            return;
        }
        if (EVENT_ORDER_CONFIRMED.equals(envelope.eventType())) {
            reserveForConfirmedOrder(envelope);
        } else {
            LOG.debugf("Ignoring order event %s", envelope.eventType());
        }
        idempotency.markProcessed(envelope.eventId(), consumer, envelope.eventType());
    }

    @Retry(maxRetries = 4, delay = 200, jitter = 100, retryOn = Exception.class)
    @Transactional
    public void handleInventoryCommand(EventEnvelope envelope) {
        String consumer = KafkaTopics.INVENTORY_COMMANDS_CONSUMER;
        if (idempotency.alreadyProcessed(envelope.eventId(), consumer)) {
            LOG.debugf("Skipping already-processed %s %s", envelope.eventType(), envelope.eventId());
            return;
        }
        if (COMMAND_RELEASE_INVENTORY.equals(envelope.eventType())) {
            releaseForOrder(envelope);
        } else {
            LOG.debugf("Ignoring inventory command %s", envelope.eventType());
        }
        idempotency.markProcessed(envelope.eventId(), consumer, envelope.eventType());
    }

    private void reserveForConfirmedOrder(EventEnvelope envelope) {
        OrderConfirmedPayload payload = serde.payloadAs(envelope, OrderConfirmedPayload.class);
        List<ReservationRequest> requests = payload.items().stream()
                .map(item -> new ReservationRequest(item.productId(), item.quantity()))
                .toList();

        ReservationOutcome outcome = reservationService.reserve(payload.orderId(), requests);
        if (outcome.reserved()) {
            List<InventoryReservedPayload.Reservation> reservations = outcome.reservations().stream()
                    .map(line -> new InventoryReservedPayload.Reservation(
                            line.reservationId(), line.productId(), line.quantity()))
                    .toList();
            outboxWriter.writeEvent(EVENT_INVENTORY_RESERVED, KafkaTopics.INVENTORY_EVENTS,
                    payload.orderId(), envelope.eventId(), new InventoryReservedPayload(payload.orderId(), reservations));
            LOG.infof("Order %s: reserved %d line(s)", payload.orderId(), reservations.size());
        } else {
            List<InventoryReservationFailedPayload.UnavailableItem> unavailable = outcome.unavailableItems().stream()
                    .map(line -> new InventoryReservationFailedPayload.UnavailableItem(
                            line.productId(), line.requestedQuantity(), line.availableQuantity()))
                    .toList();
            outboxWriter.writeEvent(EVENT_INVENTORY_RESERVATION_FAILED, KafkaTopics.INVENTORY_EVENTS,
                    payload.orderId(), envelope.eventId(),
                    new InventoryReservationFailedPayload(payload.orderId(), outcome.failureReason(), unavailable));
            LOG.infof("Order %s: reservation failed (%s)", payload.orderId(), outcome.failureReason());
        }
    }

    private void releaseForOrder(EventEnvelope envelope) {
        ReleaseInventoryPayload payload = serde.payloadAs(envelope, ReleaseInventoryPayload.class);
        List<ReservationOutcome.ReservedLine> released = reservationService.release(payload.orderId());
        if (released.isEmpty()) {
            LOG.infof("Order %s: nothing to release (idempotent)", payload.orderId());
            return;
        }
        List<InventoryReleasedPayload.ReleasedItem> releasedItems = released.stream()
                .map(line -> new InventoryReleasedPayload.ReleasedItem(line.productId(), line.quantity()))
                .toList();
        outboxWriter.writeEvent(EVENT_INVENTORY_RELEASED, KafkaTopics.INVENTORY_EVENTS,
                payload.orderId(), envelope.eventId(), new InventoryReleasedPayload(payload.orderId(), releasedItems));
        LOG.infof("Order %s: released %d line(s)", payload.orderId(), releasedItems.size());
    }
}
