package com.commercehub.order.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Serializes envelopes to the JSON stored in the outbox and parses the JSON read from Kafka.
 * Consumers are tolerant readers: unknown fields (envelope or payload) are ignored.
 */
@ApplicationScoped
public class MessageSerde {

    private final ObjectMapper mapper;

    public MessageSerde(ObjectMapper mapper) {
        this.mapper = mapper.copy().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public JsonNode toTree(Object payload) {
        return mapper.valueToTree(payload);
    }

    public String toJson(EventEnvelope envelope) {
        try {
            return mapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize message envelope " + envelope.eventId(), e);
        }
    }

    public EventEnvelope parse(String json) {
        try {
            return mapper.readValue(json, EventEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to parse message envelope", e);
        }
    }

    public <T> T payloadAs(EventEnvelope envelope, Class<T> type) {
        try {
            return mapper.treeToValue(envelope.payload(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to read payload of " + envelope.eventType(), e);
        }
    }
}
