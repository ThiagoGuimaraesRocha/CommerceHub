package com.commercehub.inventory.infrastructure.observability;

import com.commercehub.inventory.infrastructure.messaging.outbox.OutboxEventEntity;
import com.commercehub.inventory.infrastructure.messaging.outbox.OutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OutboxMetrics {

    private final OutboxRepository repository;
    private final boolean enabled;
    private final AtomicLong pending = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong published = new AtomicLong();

    public OutboxMetrics(
            MeterRegistry registry,
            OutboxRepository repository,
            @ConfigProperty(name = "commercehub.outbox.metrics.enabled", defaultValue = "true") boolean enabled) {
        this.repository = repository;
        this.enabled = enabled;
        Gauge.builder("commercehub.outbox.events", pending, AtomicLong::doubleValue)
                .description("Outbox rows by status")
                .tag("status", OutboxEventEntity.STATUS_PENDING)
                .register(registry);
        Gauge.builder("commercehub.outbox.events", failed, AtomicLong::doubleValue)
                .description("Outbox rows by status")
                .tag("status", OutboxEventEntity.STATUS_FAILED)
                .register(registry);
        Gauge.builder("commercehub.outbox.events", published, AtomicLong::doubleValue)
                .description("Outbox rows by status")
                .tag("status", OutboxEventEntity.STATUS_PUBLISHED)
                .register(registry);
    }

    @Scheduled(every = "${commercehub.outbox.metrics.every:10s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    void refresh() {
        if (!enabled) {
            return;
        }
        pending.set(repository.countByStatus(OutboxEventEntity.STATUS_PENDING));
        failed.set(repository.countByStatus(OutboxEventEntity.STATUS_FAILED));
        published.set(repository.countByStatus(OutboxEventEntity.STATUS_PUBLISHED));
    }
}
