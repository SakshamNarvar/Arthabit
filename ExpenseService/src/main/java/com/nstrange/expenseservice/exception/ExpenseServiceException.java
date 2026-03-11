package com.nstrange.expenseservice.exception;

/**
 * Generic exception for unexpected errors in the Expense Service.
 */
public class ExpenseServiceException extends RuntimeException {

    public ExpenseServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

