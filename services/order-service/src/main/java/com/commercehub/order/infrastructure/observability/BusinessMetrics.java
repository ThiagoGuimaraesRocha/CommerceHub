package com.commercehub.order.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BusinessMetrics {

    private final MeterRegistry registry;
    private final Counter ordersCreated;
    private final Counter ordersConfirmed;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.ordersCreated = Counter.builder("commercehub.orders.created")
                .description("Orders created")
                .register(registry);
        this.ordersConfirmed = Counter.builder("commercehub.orders.confirmed")
                .description("Orders confirmed")
                .register(registry);
    }

    public void recordCreated() {
        ordersCreated.increment();
    }

    public void recordConfirmed() {
        ordersConfirmed.increment();
    }

    public void recordCancelled(String reason) {
        Counter.builder("commercehub.orders.cancelled")
                .description("Orders cancelled")
                .tag("reason", reason == null || reason.isBlank() ? "unknown" : reason)
                .register(registry)
                .increment();
    }

    public void recordConsumeFailure(String topic) {
        Counter.builder("commercehub.kafka.consume.failures")
                .description("Kafka consume failures that proceed to retry/DLQ")
                .tag("topic", topic == null ? "unknown" : topic)
                .register(registry)
                .increment();
    }
}
