package com.aryafatthurahman.backend.order.dto;

import com.aryafatthurahman.backend.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        String customerName,
        String customerEmail,
        OrderStatus status,
        String currency,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
