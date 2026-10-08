package com.commercehub.inventory.infrastructure.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A message written to {@code OUTBOX_EVENTS} in the same transaction as the business change and
 * published to Kafka by {@link OutboxRelay} (Transactional Outbox, ADR 0006).
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @Column(name = "event_id", length = 36, nullable = false, updatable = false)
    private String eventId;

    @Column(name = "aggregate_type", length = 30, nullable = false, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 36, nullable = false, updatable = false)
    private String aggregateId;

    @Column(name = "event_type", length = 60, nullable = false, updatable = false)
    private String eventType;

    @Column(name = "message_kind", length = 10, nullable = false, updatable = false)
    private String messageKind;

    @Column(name = "topic", length = 120, nullable = false, updatable = false)
    private String topic;

    @Column(name = "message_key", length = 36, nullable = false, updatable = false)
    private String messageKey;

    @Lob
    @Column(name = "payload", nullable = false, updatable = false)
    private String payload;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "traceparent", length = 55, updatable = false)
    private String traceparent;

    protected OutboxEventEntity() {
    }

    public static OutboxEventEntity pending(String eventId, String aggregateType, String aggregateId,
            String eventType, String messageKind, String topic, String messageKey, String payload) {
        return pending(eventId, aggregateType, aggregateId, eventType, messageKind, topic, messageKey, payload, null);
    }

    public static OutboxEventEntity pending(String eventId, String aggregateType, String aggregateId,
            String eventType, String messageKind, String topic, String messageKey, String payload, String traceparent) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.eventId = eventId;
        entity.aggregateType = aggregateType;
        entity.aggregateId = aggregateId;
        entity.eventType = eventType;
        entity.messageKind = messageKind;
        entity.topic = topic;
        entity.messageKey = messageKey;
        entity.payload = payload;
        entity.traceparent = traceparent;
        entity.status = STATUS_PENDING;
        entity.attempts = 0;
        return entity;
    }

    public void markPublished() {
        this.status = STATUS_PUBLISHED;
        this.publishedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.lastError = null;
    }

    public void recordFailure(String error, int maxAttempts) {
        this.attempts++;
        this.lastError = truncate(error);
        if (this.attempts >= maxAttempts) {
            this.status = STATUS_FAILED;
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getPayload() {
        return payload;
    }

    public String getTraceparent() {
        return traceparent;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }
}
