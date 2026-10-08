package com.commercehub.order.infrastructure.messaging.consumer;

import com.commercehub.order.application.OrderSagaService;
import com.commercehub.order.infrastructure.messaging.EventEnvelope;
import com.commercehub.order.infrastructure.messaging.KafkaTopics;
import com.commercehub.order.infrastructure.messaging.MessageSerde;
import com.commercehub.order.infrastructure.observability.BusinessMetrics;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code commerce.inventory.events} and lets {@link OrderSagaService} advance the order.
 * Technical failures are retried inside the saga service and, if still failing, the message is sent to
 * {@code commerce.inventory.events.dlq} by the connector's dead-letter-queue strategy (ADR 0006).
 */
@ApplicationScoped
public class InventoryEventConsumer {

    private static final Logger LOG = Logger.getLogger(InventoryEventConsumer.class);

    private final OrderSagaService saga;
    private final MessageSerde serde;
    private final BusinessMetrics metrics;

    public InventoryEventConsumer(OrderSagaService saga, MessageSerde serde, BusinessMetrics metrics) {
        this.saga = saga;
        this.serde = serde;
        this.metrics = metrics;
    }

    @Incoming("inventory-events")
    @Blocking
    public void consume(String json) {
        try {
            EventEnvelope envelope = serde.parse(json);
            LOG.debugf("Received %s %s", envelope.eventType(), envelope.eventId());
            saga.handleInventoryEvent(envelope);
        } catch (RuntimeException e) {
            metrics.recordConsumeFailure(KafkaTopics.INVENTORY_EVENTS);
            throw e;
        }
    }
}
