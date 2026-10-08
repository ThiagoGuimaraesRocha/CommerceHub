package com.commercehub.user.exception;

public class ConflictException extends RuntimeException {

    private final String type;

    private ConflictException(String type, String message) {
        super(message);
        this.type = type;
    }

    public static ConflictException duplicateEmail(String email) {
        return new ConflictException("duplicate-email", "A user with email " + email + " already exists");
    }

    public String type() {
        return type;
    }
}
