package com.commercehub.order.infrastructure.messaging.payload;

/** Payload of {@code OrderCancelled} (EVENT, commerce.order.events). */
public record OrderCancelledPayload(
        String orderId,
        String previousStatus,
        String reason,
        String cancelledBy) {
}
