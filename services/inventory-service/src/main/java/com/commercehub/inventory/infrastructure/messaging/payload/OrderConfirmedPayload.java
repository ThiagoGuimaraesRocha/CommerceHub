package com.commercehub.inventory.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.util.List;

/** Payload of {@code OrderConfirmed} (EVENT, commerce.order.events). Consumed to reserve stock. */
public record OrderConfirmedPayload(
        String orderId,
        String customerId,
        List<Item> items,
        BigDecimal totalAmount,
        String currencyCode) {

    public record Item(String productId, long quantity) {
    }
}
