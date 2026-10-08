package com.commercehub.inventory.infrastructure.messaging.payload;

/** Payload of {@code ReleaseInventory} (COMMAND, commerce.inventory.commands). Consumed to release stock. */
public record ReleaseInventoryPayload(
        String orderId,
        String reason) {
}
