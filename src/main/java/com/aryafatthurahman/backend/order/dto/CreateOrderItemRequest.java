package com.aryafatthurahman.backend.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateOrderItemRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "[A-Za-z0-9._:-]+", message = "sku may contain letters, digits, dot, underscore, colon and dash")
        String sku,
        @NotBlank @Size(max = 200) String name,
        @Min(1) @Max(1000) int quantity,
        @NotNull
        @DecimalMin(value = "0.01", message = "unitPrice must be at least 0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal unitPrice) {
}
