package com.nstrange.expenseservice.exception;

/**
 * Thrown when authentication fails (e.g., missing/invalid/expired JWT).
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

