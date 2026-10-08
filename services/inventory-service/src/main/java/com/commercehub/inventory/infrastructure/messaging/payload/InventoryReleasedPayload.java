package com.commercehub.inventory.infrastructure.messaging.payload;

import java.util.List;

/** Payload of {@code InventoryReleased} (EVENT, commerce.inventory.events). */
public record InventoryReleasedPayload(
        String orderId,
        List<ReleasedItem> releasedItems) {

    public record ReleasedItem(String productId, long quantity) {
    }
}
