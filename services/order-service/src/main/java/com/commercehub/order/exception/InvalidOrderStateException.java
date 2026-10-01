package com.commercehub.order.exception;

import com.commercehub.order.domain.enumtype.OrderStatus;

public class InvalidOrderStateException extends RuntimeException {

    private final String type;

    private InvalidOrderStateException(String type, String message) {
        super(message);
        this.type = type;
    }

    public static InvalidOrderStateException transition(OrderStatus from, OrderStatus to) {
        return new InvalidOrderStateException("invalid-order-state",
                "Order cannot transition from " + from + " to " + to);
    }

    public static InvalidOrderStateException awaitingInventory(String orderId) {
        return new InvalidOrderStateException("order-awaiting-inventory",
                "Order " + orderId + " is awaiting inventory reservation and cannot be cancelled yet");
    }

    public static InvalidOrderStateException cancelNotAllowed(OrderStatus status) {
        return new InvalidOrderStateException("invalid-order-state",
                "Order in status " + status + " cannot be cancelled by the customer");
    }

    public String type() {
        return type;
    }
}
