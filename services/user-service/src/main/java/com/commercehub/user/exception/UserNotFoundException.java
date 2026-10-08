package com.commercehub.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("User " + userId + " was not found");
    }
}
