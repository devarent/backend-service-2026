package com.aryafatthurahman.backend.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String sku,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
