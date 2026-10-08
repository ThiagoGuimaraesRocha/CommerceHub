package com.commercehub.inventory.application;

/** One requested line for a reservation, independent of the Kafka payload shape. */
public record ReservationRequest(String productId, long quantity) {
}
