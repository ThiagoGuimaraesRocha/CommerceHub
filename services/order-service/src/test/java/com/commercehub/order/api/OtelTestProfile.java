package com.commercehub.order.api;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/**
 * Enables the OpenTelemetry SDK without exporting, so HTTP spans can be asserted in process.
 */
public class OtelTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.otel.sdk.disabled", "false",
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none");
    }
}
