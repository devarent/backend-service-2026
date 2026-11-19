package com.aryafatthurahman.backend.order;

import java.math.BigDecimal;

/**
 * JPQL constructor projection for the revenue aggregate.
 */
public record RevenueByStatus(OrderStatus status, Long orderCount, BigDecimal revenue) {
}
