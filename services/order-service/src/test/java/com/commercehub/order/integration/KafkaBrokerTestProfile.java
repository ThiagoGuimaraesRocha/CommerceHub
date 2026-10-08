package com.commercehub.order.integration;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.HashMap;
import java.util.Map;

/**
 * Boots the service against a real Kafka broker started by Quarkus Dev Services.
 * Overrides the {@code %test.} keys from {@code application.properties}, which otherwise disable
 * the broker and the outbox relay.
 */
public class KafkaBrokerTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("quarkus.kafka.devservices.enabled", "true");
        overrides.put("%test.quarkus.kafka.devservices.enabled", "true");
        overrides.put("commercehub.outbox.relay.enabled", "true");
        overrides.put("%test.commercehub.outbox.relay.enabled", "true");
        overrides.put("commercehub.outbox.relay.every", "1h");
        overrides.put("mp.messaging.incoming.inventory-events.connector", "smallrye-kafka");
        overrides.put("%test.mp.messaging.incoming.inventory-events.connector", "smallrye-kafka");
        return overrides;
    }
}
