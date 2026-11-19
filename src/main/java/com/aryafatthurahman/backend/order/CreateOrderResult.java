package com.aryafatthurahman.backend.order;

import com.aryafatthurahman.backend.order.dto.OrderResponse;

public record CreateOrderResult(OrderResponse order, boolean created) {
}
