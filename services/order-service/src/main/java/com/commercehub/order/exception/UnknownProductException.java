package com.commercehub.order.exception;

public class UnknownProductException extends RuntimeException {

    public UnknownProductException(String productId) {
        super("Product " + productId + " is unknown or inactive");
    }
}
