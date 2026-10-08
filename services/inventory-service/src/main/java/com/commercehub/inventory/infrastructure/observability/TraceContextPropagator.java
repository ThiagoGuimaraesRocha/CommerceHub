package com.commercehub.inventory.infrastructure.observability;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * W3C {@code traceparent} helpers for the transactional outbox. The HTTP/saga span is stored on the
 * outbox row and restored when the relay publishes, so Kafka consumers join the same trace.
 */
public final class TraceContextPropagator {

    public static final String TRACEPARENT = "traceparent";

    private static final W3CTraceContextPropagator W3C = W3CTraceContextPropagator.getInstance();
    private static final TextMapSetter<ProducerRecord<String, String>> KAFKA_SETTER =
            (carrier, key, value) -> carrier.headers().add(key, value.getBytes(StandardCharsets.UTF_8));
    private static final TextMapGetter<String> STRING_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(String carrier) {
            return List.of(TRACEPARENT);
        }

        @Override
        public String get(String carrier, String key) {
            return TRACEPARENT.equals(key) ? carrier : null;
        }
    };

    private TraceContextPropagator() {
    }

    public static String currentTraceparent() {
        Map<String, String> carrier = new HashMap<>();
        W3C.inject(Context.current(), carrier, Map::put);
        String value = carrier.get(TRACEPARENT);
        return (value == null || value.isBlank()) ? null : value;
    }

    public static void inject(ProducerRecord<String, String> record) {
        W3C.inject(Context.current(), record, KAFKA_SETTER);
    }

    public static Context extract(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return Context.current();
        }
        return W3C.extract(Context.root(), traceparent, STRING_GETTER);
    }
}
