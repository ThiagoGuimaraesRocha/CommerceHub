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
 * Consumes {@code commerce.inventory.commands} and releases stock. Technical failures are retried inside
 * the saga and, if still failing, the message goes to {@code commerce.inventory.commands.dlq}.
 */
@ApplicationScoped
public class InventoryCommandConsumer {

    private static final Logger LOG = Logger.getLogger(InventoryCommandConsumer.class);

    private final InventorySagaService saga;
    private final MessageSerde serde;
    private final BusinessMetrics metrics;

    public InventoryCommandConsumer(InventorySagaService saga, MessageSerde serde, BusinessMetrics metrics) {
        this.saga = saga;
        this.serde = serde;
        this.metrics = metrics;
    }

    @Incoming("inventory-commands")
    @Blocking
    public void consume(String json) {
        try {
            EventEnvelope envelope = serde.parse(json);
            LOG.debugf("Received %s %s", envelope.eventType(), envelope.eventId());
            saga.handleInventoryCommand(envelope);
        } catch (RuntimeException e) {
            metrics.recordConsumeFailure(KafkaTopics.INVENTORY_COMMANDS);
            throw e;
        }
    }
}
