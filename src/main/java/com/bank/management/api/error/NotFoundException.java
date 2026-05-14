package com.bank.management.api.error;

/**
 * Thrown when a referenced entity does not exist.
 */
public class NotFoundException extends BankException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
