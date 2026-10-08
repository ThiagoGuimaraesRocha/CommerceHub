package com.commercehub.inventory.application;

import java.util.List;

/**
 * Result of an all-or-nothing reservation attempt. On failure nothing is reserved and the offending lines
 * are reported; the saga turns this into {@code InventoryReserved} or {@code InventoryReservationFailed}.
 */
public record ReservationOutcome(
        boolean reserved,
        List<ReservedLine> reservations,
        String failureReason,
        List<UnavailableLine> unavailableItems) {

    public static final String REASON_INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    public static final String REASON_UNKNOWN_PRODUCT = "UNKNOWN_PRODUCT";

    public static ReservationOutcome reserved(List<ReservedLine> reservations) {
        return new ReservationOutcome(true, reservations, null, List.of());
    }

    public static ReservationOutcome failed(String reason, List<UnavailableLine> unavailable) {
        return new ReservationOutcome(false, List.of(), reason, unavailable);
    }

    public record ReservedLine(String reservationId, String productId, long quantity) {
    }

    public record UnavailableLine(String productId, long requestedQuantity, long availableQuantity) {
    }
}
