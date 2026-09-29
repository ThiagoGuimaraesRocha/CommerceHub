package com.commercehub.product.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("Product " + productId + " was not found");
    }
}
