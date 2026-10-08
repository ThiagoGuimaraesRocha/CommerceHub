package com.commercehub.inventory.exception;

public class InventoryItemNotFoundException extends RuntimeException {

    public InventoryItemNotFoundException(String productId) {
        super("No inventory record for product " + productId);
    }
}
