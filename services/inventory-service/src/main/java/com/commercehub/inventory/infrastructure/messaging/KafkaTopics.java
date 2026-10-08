package com.commercehub.inventory.infrastructure.messaging;

/**
 * Topic and consumer-group names shared with the event contracts in {@code docs/events}.
 */
public final class KafkaTopics {

    public static final String ORDER_EVENTS = "commerce.order.events";
    public static final String INVENTORY_EVENTS = "commerce.inventory.events";
    public static final String INVENTORY_COMMANDS = "commerce.inventory.commands";

    public static final String SOURCE = "inventory-service";

    /** Deduplication scopes ({@code <service>.<topic>}). */
    public static final String ORDER_EVENTS_CONSUMER = "inventory-service." + ORDER_EVENTS;
    public static final String INVENTORY_COMMANDS_CONSUMER = "inventory-service." + INVENTORY_COMMANDS;

    private KafkaTopics() {
    }
}
