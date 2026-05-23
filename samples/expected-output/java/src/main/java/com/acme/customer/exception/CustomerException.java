package com.acme.customer.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception hierarchy for customer operations.
 * Maps COBOL status codes and error messages to Java exceptions.
 *
 * <pre>
 * COBOL Return Code Mapping:
 * - 1002: CUSTOMER ALREADY EXISTS → CustomerAlreadyExistsException
 * - 1003: WRITE FAILED → CustomerPersistenceException
 * - 2001: CUSTOMER NOT FOUND → CustomerNotFoundException
 * - 2002: REWRITE FAILED → CustomerPersistenceException
 * - 3001: CUSTOMER NOT FOUND → CustomerNotFoundException
 * - 3002: DELETE FAILED → CustomerPersistenceException
 * - 4001: CUSTOMER NOT FOUND → CustomerNotFoundException
 * - 6001: DISCOUNT CALC OVERFLOW → DiscountCalculationException
 * - 9999: FAILED TO OPEN FILE → CustomerPersistenceException
 * - 1001: INVALID TRANSACTION TYPE → InvalidOperationException
 * </pre>
 */
public class CustomerException extends RuntimeException {

    private final int errorCode;
    private final HttpStatus httpStatus;

    public CustomerException(String message, int errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public CustomerException(String message, int errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Customer not found exception.
     * Maps to COBOL return codes: 2001, 3001, 4001 (CUSTOMER NOT FOUND)
     */
    public static class CustomerNotFoundException extends CustomerException {
        public CustomerNotFoundException(String customerId) {
            super("Customer not found: " + customerId, 2001, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Customer already exists exception.
     * Maps to COBOL return code: 1002 (CUSTOMER ALREADY EXISTS)
     */
    public static class CustomerAlreadyExistsException extends CustomerException {
        public CustomerAlreadyExistsException(String customerId) {
            super("Customer already exists: " + customerId, 1002, HttpStatus.CONFLICT);
        }
    }

    /**
     * Customer persistence exception.
     * Maps to COBOL return codes: 1003, 2002, 3002, 9999 (WRITE/REWRITE/DELETE/OPEN FAILED)
     */
    public static class CustomerPersistenceException extends CustomerException {
        public CustomerPersistenceException(String operation, int errorCode, Throwable cause) {
            super(operation + " failed", errorCode, HttpStatus.INTERNAL_SERVER_ERROR, cause);
        }

        public CustomerPersistenceException(String operation, int errorCode) {
            super(operation + " failed", errorCode, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Discount calculation exception.
     * Maps to COBOL return code: 6001 (DISCOUNT CALC OVERFLOW)
     */
    public static class DiscountCalculationException extends CustomerException {
        public DiscountCalculationException(String message) {
            super(message, 6001, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    /**
     * Invalid operation exception.
     * Maps to COBOL return code: 1001 (INVALID TRANSACTION TYPE)
     */
    public static class InvalidOperationException extends CustomerException {
        public InvalidOperationException(String message) {
            super(message, 1001, HttpStatus.BAD_REQUEST);
        }
    }
}
