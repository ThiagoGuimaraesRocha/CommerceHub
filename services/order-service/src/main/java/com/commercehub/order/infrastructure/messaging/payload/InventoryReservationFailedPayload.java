package com.commercehub.order.infrastructure.messaging.payload;

import java.util.List;

/** Payload of {@code InventoryReservationFailed} (EVENT, commerce.inventory.events). */
public record InventoryReservationFailedPayload(
        String orderId,
        String reason,
        List<UnavailableItem> unavailableItems) {

    public static final String REASON_INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    public static final String REASON_UNKNOWN_PRODUCT = "UNKNOWN_PRODUCT";

    public record UnavailableItem(String productId, long requestedQuantity, long availableQuantity) {
    }
}
