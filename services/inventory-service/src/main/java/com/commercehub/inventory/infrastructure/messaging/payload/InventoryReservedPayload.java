package com.commercehub.inventory.infrastructure.messaging.payload;

import java.util.List;

/** Payload of {@code InventoryReserved} (EVENT, commerce.inventory.events). */
public record InventoryReservedPayload(
        String orderId,
        List<Reservation> reservations) {

    public record Reservation(String reservationId, String productId, long quantity) {
    }
}
