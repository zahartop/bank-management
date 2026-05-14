package com.bank.management.api.error;

/**
 * Domain-level exception carrying a stable machine-readable code.
 */
public class BankException extends RuntimeException {

    private final String code;

    public BankException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
