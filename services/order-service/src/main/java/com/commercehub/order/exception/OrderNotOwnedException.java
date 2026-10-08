package com.commercehub.order.exception;

public class OrderNotOwnedException extends RuntimeException {

    public OrderNotOwnedException(String orderId) {
        super("Order " + orderId + " does not belong to the authenticated customer");
    }
}
