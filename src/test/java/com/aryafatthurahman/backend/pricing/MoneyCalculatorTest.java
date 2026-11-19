package com.aryafatthurahman.backend.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyCalculatorTest {

    @Test
    void lineTotalMultipliesAndRoundsHalfUpToTwoDecimals() {
        BigDecimal result = MoneyCalculator.lineTotal(new BigDecimal("10.005"), 3);

        assertThat(result).isEqualByComparingTo(new BigDecimal("30.02"));
        assertThat(result.scale()).isEqualTo(MoneyCalculator.SCALE);
    }

    @Test
    void lineTotalRejectsNegativeUnitPriceAndNonPositiveQuantity() {
        assertThatThrownBy(() -> MoneyCalculator.lineTotal(new BigDecimal("-1.00"), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than or equal to zero");

        assertThatThrownBy(() -> MoneyCalculator.lineTotal(BigDecimal.ONE, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> MoneyCalculator.lineTotal(null, 1))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void subtotalSumsRoundedLineTotals() {
        List<MoneyCalculator.Line> lines = List.of(
                new MoneyCalculator.Line(new BigDecimal("10.00"), 2),
                new MoneyCalculator.Line(new BigDecimal("5.25"), 3));

        assertThat(MoneyCalculator.subtotal(lines))
                .isEqualByComparingTo(new BigDecimal("35.75"));
    }

    @Test
    void subtotalRejectsEmptyOrNullInput() {
        assertThatThrownBy(() -> MoneyCalculator.subtotal(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one line");

        assertThatThrownBy(() -> MoneyCalculator.subtotal(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void taxMultipliesSubtotalByRateAndRoundsToCents() {
        BigDecimal tax = MoneyCalculator.tax(new BigDecimal("35.75"), new BigDecimal("0.0825"));

        assertThat(tax).isEqualByComparingTo(new BigDecimal("2.95"));
        assertThat(MoneyCalculator.tax(new BigDecimal("35.75"), BigDecimal.ZERO))
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
    }

    @Test
    void taxRejectsNegativeSubtotalAndOutOfRangeRate() {
        assertThatThrownBy(() -> MoneyCalculator.tax(new BigDecimal("-1.00"), BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> MoneyCalculator.tax(BigDecimal.TEN, new BigDecimal("1.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 1");

        assertThatThrownBy(() -> MoneyCalculator.tax(BigDecimal.TEN, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void totalAddsSubtotalAndTax() {
        BigDecimal total = MoneyCalculator.total(new BigDecimal("35.75"), new BigDecimal("2.95"));

        assertThat(total).isEqualByComparingTo(new BigDecimal("38.70"));
        assertThat(total.scale()).isEqualTo(MoneyCalculator.SCALE);
    }

    @Test
    void totalRejectsNegativeComponents() {
        assertThatThrownBy(() -> MoneyCalculator.total(new BigDecimal("-1.00"), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> MoneyCalculator.total(BigDecimal.ONE, new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
