package com.aryafatthurahman.backend.order.dto;

import com.aryafatthurahman.backend.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RevenueStatsResponse(
        String currency,
        BigDecimal totalRevenue,
        long totalOrders,
        List<RevenueByStatusResponse> byStatus,
        Instant generatedAt) {
}
