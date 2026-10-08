package com.commercehub.order.infrastructure.messaging.consumer;

import com.commercehub.order.application.OrderSagaService;
import com.commercehub.order.infrastructure.messaging.EventEnvelope;
import com.commercehub.order.infrastructure.messaging.MessageSerde;
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

    public InventoryEventConsumer(OrderSagaService saga, MessageSerde serde) {
        this.saga = saga;
        this.serde = serde;
    }

    @Incoming("inventory-events")
    @Blocking
    public void consume(String json) {
        EventEnvelope envelope = serde.parse(json);
        LOG.debugf("Received %s %s", envelope.eventType(), envelope.eventId());
        saga.handleInventoryEvent(envelope);
    }
}
