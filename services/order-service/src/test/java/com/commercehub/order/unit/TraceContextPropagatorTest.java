package com.commercehub.order.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercehub.order.infrastructure.observability.TraceContextPropagator;
import io.opentelemetry.context.Scope;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

class TraceContextPropagatorTest {

    private static final String TRACEPARENT = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

    @Test
    void extractThenInjectPreservesTraceId() {
        try (Scope ignored = TraceContextPropagator.extract(TRACEPARENT).makeCurrent()) {
            ProducerRecord<String, String> record = new ProducerRecord<>("commerce.order.events", "k", "v");
            TraceContextPropagator.inject(record);
            String injected = new String(
                    record.headers().lastHeader(TraceContextPropagator.TRACEPARENT).value(), StandardCharsets.UTF_8);
            assertThat(injected).startsWith("00-4bf92f3577b34da6a3ce929d0e0e4736-");
        }
    }

    @Test
    void blankTraceparentDoesNotThrow() {
        assertThat(TraceContextPropagator.extract(" ")).isNotNull();
        assertThat(TraceContextPropagator.extract(null)).isNotNull();
    }
}
