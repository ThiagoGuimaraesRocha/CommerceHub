package com.commercehub.inventory.integration;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/**
 * Boots the service against a real Kafka broker (Quarkus Dev Services) instead of the in-memory connector.
 */
public class KafkaBrokerTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.kafka.devservices.enabled", "true",
                "commercehub.outbox.relay.enabled", "true",
                "commercehub.outbox.relay.every", "1h",
                "mp.messaging.incoming.order-events.connector", "smallrye-kafka",
                "mp.messaging.incoming.inventory-commands.connector", "smallrye-kafka");
    }
}
