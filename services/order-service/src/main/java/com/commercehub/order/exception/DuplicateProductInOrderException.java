package com.commercehub.order.exception;

public class DuplicateProductInOrderException extends RuntimeException {

    public DuplicateProductInOrderException(String productId) {
        super("Product " + productId + " appears more than once in the order");
    }
}
