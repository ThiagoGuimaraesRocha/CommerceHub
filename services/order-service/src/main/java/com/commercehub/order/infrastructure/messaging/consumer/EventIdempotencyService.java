package com.commercehub.order.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Deduplication by {@code (eventId, consumerName)}. Callers run inside the consumer transaction so that
 * marking a message processed and applying its effect commit together.
 */
@ApplicationScoped
public class EventIdempotencyService {

    private final ProcessedEventRepository repository;

    public EventIdempotencyService(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    public boolean alreadyProcessed(String eventId, String consumerName) {
        return repository.exists(eventId, consumerName);
    }

    public void markProcessed(String eventId, String consumerName, String eventType) {
        repository.persist(new ProcessedEventEntity(eventId, consumerName, eventType));
    }
}
