package com.commercehub.order.infrastructure.messaging.payload;

/** Payload of {@code ReleaseInventory} (COMMAND, commerce.inventory.commands). */
public record ReleaseInventoryPayload(
        String orderId,
        String reason) {

    public static final String REASON_CUSTOMER_CANCELLED = "CUSTOMER_CANCELLED";
    public static final String REASON_PAYMENT_FAILED = "PAYMENT_FAILED";
}
