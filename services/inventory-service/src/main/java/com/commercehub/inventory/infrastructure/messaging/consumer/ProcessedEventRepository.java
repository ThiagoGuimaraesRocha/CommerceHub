package com.commercehub.inventory.infrastructure.messaging.consumer;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessedEventRepository
        implements PanacheRepositoryBase<ProcessedEventEntity, ProcessedEventEntity.Key> {

    public boolean exists(String eventId, String consumerName) {
        return count("eventId = ?1 and consumerName = ?2", eventId, consumerName) > 0;
    }
}
