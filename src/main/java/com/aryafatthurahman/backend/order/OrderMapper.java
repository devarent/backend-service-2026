package com.aryafatthurahman.backend.order;

import com.aryafatthurahman.backend.order.dto.OrderItemResponse;
import com.aryafatthurahman.backend.order.dto.OrderResponse;
import com.aryafatthurahman.backend.order.dto.OrderSummaryResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItem)
                .toList();
        return new OrderResponse(
                order.getPublicId(),
                order.getCustomerId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getCurrency(),
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                items,
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getPublicId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getCurrency(),
                order.getTotalAmount(),
                order.getItems().size(),
                order.getCreatedAt());
    }

    private OrderItemResponse toItem(OrderItem item) {
        return new OrderItemResponse(
                item.getProductSku(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
