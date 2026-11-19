package com.aryafatthurahman.backend.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.0825");

    @Test
    void newOrderStartsPendingWithTimestampsAndNoItems() {
        Order order = newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCurrency()).isEqualTo("USD");
        assertThat(order.getVersion()).isZero();
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
        assertThat(order.getItems()).isEmpty();
    }

    @Test
    void recalculateTotalsRoundsEachLineAndTheTax() {
        Order order = newOrder();
        order.addItem(new OrderItem("SKU-1", "Widget", 2, new BigDecimal("10.00")));
        order.addItem(new OrderItem("SKU-2", "Gadget", 1, new BigDecimal("5.00")));

        order.recalculateTotals(TAX_RATE);

        assertThat(order.getSubtotal()).isEqualByComparingTo("25.00");
        assertThat(order.getTaxAmount()).isEqualByComparingTo("2.06");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("27.06");
        assertThat(order.getSubtotal().scale()).isEqualTo(2);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(
                order.getSubtotal().add(order.getTaxAmount()));
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems().get(0).getLineTotal()).isEqualByComparingTo("20.00");
    }

    @Test
    void validTransitionUpdatesStatusAndRejectsIllegalMoves() {
        Order order = newOrder();

        order.transitionTo(OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.SHIPPED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid transition");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelledOrderIsTerminal() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CANCELLED);

        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PENDING))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addingNullItemIsRejected() {
        Order order = newOrder();

        assertThatThrownBy(() -> order.addItem(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("item");
    }

    private Order newOrder() {
        return new Order(
                UUID.randomUUID(),
                "CUST-UNIT-1",
                "Ada Lovelace",
                "ada@example.com",
                "USD",
                null);
    }
}
