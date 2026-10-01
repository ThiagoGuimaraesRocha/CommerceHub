package com.commercehub.order.exception;

public class RemoteProductServiceException extends RuntimeException {

    public RemoteProductServiceException(String message) {
        super(message);
    }

    public RemoteProductServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
