package com.commercehub.inventory.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BusinessMetrics {

    public static final String OUTCOME_RESERVED = "reserved";
    public static final String OUTCOME_FAILED = "failed";
    public static final String OUTCOME_RELEASED = "released";

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordReservation(String outcome) {
        Counter.builder("commercehub.inventory.reservations")
                .description("Inventory reservation outcomes")
                .tag("outcome", outcome == null ? "unknown" : outcome)
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
