package com.bank.management.api.error;

/**
 * User is not allowed to perform the operation.
 */
public class ForbiddenException extends BankException {

    public ForbiddenException(String code, String message) {
        super(code, message);
    }
}
