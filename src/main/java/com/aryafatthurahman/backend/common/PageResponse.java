package com.aryafatthurahman.backend.common;

import java.util.List;

/**
 * Envelope for paginated collection endpoints.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
