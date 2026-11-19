package com.aryafatthurahman.backend.common;

/**
 * A single bean-validation failure. Rejected values are deliberately not echoed
 * back to avoid leaking sensitive input into logs or client-visible errors.
 */
public record FieldViolation(String field, String message) {
}
