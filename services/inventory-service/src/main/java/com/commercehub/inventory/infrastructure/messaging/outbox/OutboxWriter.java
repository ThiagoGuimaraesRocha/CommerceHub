package com.commercehub.inventory.infrastructure.messaging.outbox;

import com.commercehub.inventory.infrastructure.messaging.EventEnvelope;
import com.commercehub.inventory.infrastructure.messaging.KafkaTopics;
import com.commercehub.inventory.infrastructure.messaging.MessageSerde;
import com.commercehub.inventory.infrastructure.observability.TraceContextPropagator;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Builds an {@link EventEnvelope} and stores it in the outbox. Must be called inside the business
 * transaction so the message and the state change commit atomically.
 */
@ApplicationScoped
public class OutboxWriter {

    private static final String AGGREGATE_TYPE = "Inventory";

    private final OutboxRepository repository;
    private final MessageSerde serde;

    public OutboxWriter(OutboxRepository repository, MessageSerde serde) {
        this.repository = repository;
        this.serde = serde;
    }

    /**
     * @param orderId     saga id; it is the aggregate id, the correlation id and the Kafka record key.
     * @param causationId eventId of the message that caused this one.
     */
    public String writeEvent(String eventType, String topic, String orderId, String causationId, Object payload) {
        String eventId = UUID.randomUUID().toString();
        EventEnvelope envelope = new EventEnvelope(
                eventId,
                eventType,
                EventEnvelope.KIND_EVENT,
                EventEnvelope.CURRENT_SCHEMA_VERSION,
                OffsetDateTime.now(ZoneOffset.UTC),
                KafkaTopics.SOURCE,
                AGGREGATE_TYPE,
                orderId,
                orderId,
                causationId,
                serde.toTree(payload));
        repository.persist(OutboxEventEntity.pending(
                eventId, AGGREGATE_TYPE, orderId, eventType, EventEnvelope.KIND_EVENT, topic, orderId, serde.toJson(envelope),
                TraceContextPropagator.currentTraceparent()));
        return eventId;
    }
}
