package com.commercehub.inventory.infrastructure.messaging.consumer;

import com.commercehub.inventory.application.InventorySagaService;
import com.commercehub.inventory.infrastructure.messaging.EventEnvelope;
import com.commercehub.inventory.infrastructure.messaging.MessageSerde;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code commerce.order.events} and reserves stock for confirmed orders. Technical failures are
 * retried inside the saga and, if still failing, the message goes to {@code commerce.order.events.dlq}.
 */
@ApplicationScoped
public class OrderEventConsumer {

    private static final Logger LOG = Logger.getLogger(OrderEventConsumer.class);

    private final InventorySagaService saga;
    private final MessageSerde serde;

    public OrderEventConsumer(InventorySagaService saga, MessageSerde serde) {
        this.saga = saga;
        this.serde = serde;
    }

    @Incoming("order-events")
    @Blocking
    public void consume(String json) {
        EventEnvelope envelope = serde.parse(json);
        LOG.debugf("Received %s %s", envelope.eventType(), envelope.eventId());
        saga.handleOrderEvent(envelope);
    }
}
