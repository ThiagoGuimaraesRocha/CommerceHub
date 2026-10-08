package com.commercehub.order.integration;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class KafkaBrokerTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.kafka.devservices.enabled", "true",
                "commercehub.outbox.relay.enabled", "true",
                "commercehub.outbox.relay.every", "1h",
                "mp.messaging.incoming.inventory-events.connector", "smallrye-kafka");
    }
}
