package com.aryafatthurahman.backend.order.dto;

import com.aryafatthurahman.backend.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
