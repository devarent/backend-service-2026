package com.aryafatthurahman.backend.order.dto;

import com.aryafatthurahman.backend.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String customerId,
        OrderStatus status,
        String currency,
        BigDecimal totalAmount,
        int itemCount,
        Instant createdAt) {
}
