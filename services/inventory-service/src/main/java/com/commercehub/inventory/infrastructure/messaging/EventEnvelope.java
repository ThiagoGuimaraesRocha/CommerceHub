package com.commercehub.inventory.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

/**
 * Single message envelope used for every event and command (see {@code docs/events/README.md}).
 * The business data lives in {@code payload}; consumers are tolerant readers and ignore unknown fields.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope(
        String eventId,
        String eventType,
        String messageKind,
        int schemaVersion,
        OffsetDateTime occurredAt,
        String source,
        String aggregateType,
        String aggregateId,
        String correlationId,
        String causationId,
        JsonNode payload) {

    public static final String KIND_EVENT = "EVENT";
    public static final String KIND_COMMAND = "COMMAND";
    public static final int CURRENT_SCHEMA_VERSION = 1;
}
