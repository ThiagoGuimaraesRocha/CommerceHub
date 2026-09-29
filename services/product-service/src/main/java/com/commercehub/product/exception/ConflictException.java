package com.commercehub.product.exception;

public class ConflictException extends RuntimeException {

    private final String type;

    private ConflictException(String type, String message) {
        super(message);
        this.type = type;
    }

    public static ConflictException duplicateSku(String sku) {
        return new ConflictException("duplicate-sku", "A product with SKU " + sku + " already exists");
    }

    public static ConflictException staleVersion(String productId, long expected, long actual) {
        return new ConflictException("stale-version",
                "Product " + productId + " is at version " + actual + " but the request was based on version " + expected);
    }

    public String type() {
        return type;
    }
}
