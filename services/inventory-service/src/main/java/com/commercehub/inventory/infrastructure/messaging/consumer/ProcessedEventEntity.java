package com.commercehub.inventory.infrastructure.messaging.consumer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * One row per processed message and consumer. The composite primary key {@code (event_id, consumer_name)}
 * makes redeliveries idempotent: a duplicate key means the effect was already applied.
 */
@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEventEntity.Key.class)
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", length = 36, nullable = false, updatable = false)
    private String eventId;

    @Id
    @Column(name = "consumer_name", length = 100, nullable = false, updatable = false)
    private String consumerName;

    @Column(name = "event_type", length = 60, nullable = false, updatable = false)
    private String eventType;

    protected ProcessedEventEntity() {
    }

    public ProcessedEventEntity(String eventId, String consumerName, String eventType) {
        this.eventId = eventId;
        this.consumerName = consumerName;
        this.eventType = eventType;
    }

    public static class Key implements Serializable {
        private String eventId;
        private String consumerName;

        public Key() {
        }

        public Key(String eventId, String consumerName) {
            this.eventId = eventId;
            this.consumerName = consumerName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(eventId, key.eventId) && Objects.equals(consumerName, key.consumerName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, consumerName);
        }
    }
}
