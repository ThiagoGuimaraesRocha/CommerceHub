package com.commercehub.order.exception;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String expected, String actual) {
        super("All order items must share the same currency; expected " + expected + " but found " + actual);
    }
}
