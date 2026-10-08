package com.commercehub.order.infrastructure.messaging;

/**
 * Topic and consumer-group names shared with the event contracts in {@code docs/events}.
 */
public final class KafkaTopics {

    public static final String ORDER_EVENTS = "commerce.order.events";
    public static final String INVENTORY_EVENTS = "commerce.inventory.events";
    public static final String INVENTORY_COMMANDS = "commerce.inventory.commands";

    public static final String SOURCE = "order-service";

    /** Deduplication scope for the inventory-events consumer: {@code <service>.<topic>}. */
    public static final String INVENTORY_EVENTS_CONSUMER = "order-service." + INVENTORY_EVENTS;

    private KafkaTopics() {
    }
}
