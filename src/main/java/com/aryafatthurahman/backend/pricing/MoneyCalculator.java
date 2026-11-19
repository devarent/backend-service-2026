package com.aryafatthurahman.backend.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Pure monetary calculations. Keeping this logic outside the entity and service
 * makes rounding behaviour deterministic and unit-testable without a database.
 */
public final class MoneyCalculator {

    public static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal MAX_TAX_RATE = BigDecimal.ONE;

    private MoneyCalculator() {
    }

    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must be greater than or equal to zero");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(SCALE, ROUNDING);
    }

    public static BigDecimal subtotal(List<Line> lines) {
        Objects.requireNonNull(lines, "lines must not be null");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("an order must contain at least one line");
        }
        BigDecimal subtotal = lines.stream()
                .map(line -> lineTotal(line.unitPrice(), line.quantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return subtotal.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal tax(BigDecimal subtotal, BigDecimal taxRate) {
        Objects.requireNonNull(subtotal, "subtotal must not be null");
        Objects.requireNonNull(taxRate, "taxRate must not be null");
        if (subtotal.signum() < 0) {
            throw new IllegalArgumentException("subtotal must be greater than or equal to zero");
        }
        if (taxRate.signum() < 0 || taxRate.compareTo(MAX_TAX_RATE) > 0) {
            throw new IllegalArgumentException("taxRate must be between 0 and 1");
        }
        return subtotal.multiply(taxRate).setScale(SCALE, ROUNDING);
    }

    public static BigDecimal total(BigDecimal subtotal, BigDecimal tax) {
        Objects.requireNonNull(subtotal, "subtotal must not be null");
        Objects.requireNonNull(tax, "tax must not be null");
        if (subtotal.signum() < 0 || tax.signum() < 0) {
            throw new IllegalArgumentException("subtotal and tax must be greater than or equal to zero");
        }
        return subtotal.add(tax).setScale(SCALE, ROUNDING);
    }

    public record Line(BigDecimal unitPrice, int quantity) {
    }
}
