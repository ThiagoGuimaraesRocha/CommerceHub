package com.commercehub.inventory.infrastructure.messaging.consumer;

import com.commercehub.inventory.application.InventorySagaService;
import com.commercehub.inventory.infrastructure.messaging.EventEnvelope;
import com.commercehub.inventory.infrastructure.messaging.KafkaTopics;
import com.commercehub.inventory.infrastructure.messaging.MessageSerde;
import com.commercehub.inventory.infrastructure.observability.BusinessMetrics;
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
    private final BusinessMetrics metrics;

    public OrderEventConsumer(InventorySagaService saga, MessageSerde serde, BusinessMetrics metrics) {
        this.saga = saga;
        this.serde = serde;
        this.metrics = metrics;
    }

    @Incoming("order-events")
    @Blocking
    public void consume(String json) {
        try {
            EventEnvelope envelope = serde.parse(json);
            LOG.debugf("Received %s %s", envelope.eventType(), envelope.eventId());
            saga.handleOrderEvent(envelope);
        } catch (RuntimeException e) {
            metrics.recordConsumeFailure(KafkaTopics.ORDER_EVENTS);
            throw e;
        }
    }
}
