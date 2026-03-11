package com.nstrange.expenseservice.exception;

public class InvalidExpenseRequestException extends RuntimeException {

    public InvalidExpenseRequestException(String message) {
        super(message);
    }

    public InvalidExpenseRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
