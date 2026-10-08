package com.commercehub.inventory.infrastructure.messaging.consumer;

import com.commercehub.inventory.application.InventorySagaService;
import com.commercehub.inventory.infrastructure.messaging.EventEnvelope;
import com.commercehub.inventory.infrastructure.messaging.MessageSerde;
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

    public InventoryCommandConsumer(InventorySagaService saga, MessageSerde serde) {
        this.saga = saga;
        this.serde = serde;
    }

    @Incoming("inventory-commands")
    @Blocking
    public void consume(String json) {
        EventEnvelope envelope = serde.parse(json);
        LOG.debugf("Received %s %s", envelope.eventType(), envelope.eventId());
        saga.handleInventoryCommand(envelope);
    }
}
