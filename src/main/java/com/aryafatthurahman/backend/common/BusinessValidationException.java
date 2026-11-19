package com.aryafatthurahman.backend.common;

/**
 * A request that is syntactically valid JSON but violates a business rule.
 */
public class BusinessValidationException extends RuntimeException {

    private final String code;

    public BusinessValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
