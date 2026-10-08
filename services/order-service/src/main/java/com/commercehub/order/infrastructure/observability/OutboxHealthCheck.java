package com.commercehub.order.infrastructure.observability;

import com.commercehub.order.infrastructure.messaging.outbox.OutboxEventEntity;
import com.commercehub.order.infrastructure.messaging.outbox.OutboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness goes DOWN when the outbox has {@code FAILED} rows that will not be retried (ADR 0006).
 */
@Readiness
@ApplicationScoped
public class OutboxHealthCheck implements HealthCheck {

    private final OutboxRepository repository;

    public OutboxHealthCheck(OutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public HealthCheckResponse call() {
        long failed = repository.countByStatus(OutboxEventEntity.STATUS_FAILED);
        long pending = repository.countByStatus(OutboxEventEntity.STATUS_PENDING);
        var response = HealthCheckResponse.named("outbox")
                .withData("failed", failed)
                .withData("pending", pending);
        return failed > 0 ? response.down().build() : response.up().build();
    }
}
