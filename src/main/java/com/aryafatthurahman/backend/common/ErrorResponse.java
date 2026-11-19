package com.aryafatthurahman.backend.common;

import java.time.Instant;
import java.util.List;

/**
 * Stable error contract returned for every non-2xx response produced by the API.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String requestId,
        List<FieldViolation> violations) {
}
