package com.aryafatthurahman.backend.order.dto;

import com.aryafatthurahman.backend.order.OrderStatus;

import java.math.BigDecimal;

public record RevenueByStatusResponse(OrderStatus status, long orderCount, BigDecimal revenue) {
}
