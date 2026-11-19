package com.aryafatthurahman.backend.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Size(max = 64) String customerId,
        @NotBlank @Size(max = 200) String customerName,
        @NotBlank @Email @Size(max = 320) String customerEmail,
        @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO 4217 code")
        String currency,
        @NotEmpty @Size(max = 100) @Valid List<CreateOrderItemRequest> items) {
}
