package com.commercehub.order.api;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Test-only probe so {@link TracePropagationIT} can read the server span without a live Jaeger.
 */
@Path("/__otel/span")
public class SpanProbeResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> currentSpan() {
        SpanContext context = Span.current().getSpanContext();
        return Map.of(
                "valid", context.isValid(),
                "traceId", context.getTraceId(),
                "spanId", context.getSpanId());
    }
}
