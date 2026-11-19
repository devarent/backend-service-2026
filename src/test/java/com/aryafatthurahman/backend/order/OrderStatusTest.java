package com.aryafatthurahman.backend.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTest {

    @Test
    void pendingCanOnlyMoveToConfirmedOrCancelled() {
        assertThat(OrderStatus.PENDING.allowedTransitions())
                .containsExactlyInAnyOrder(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
    }

    @Test
    void shippedCanOnlyMoveToDelivered() {
        assertThat(OrderStatus.SHIPPED.allowedTransitions())
                .containsExactly(OrderStatus.DELIVERED);
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void terminalStatusesHaveNoOutgoingTransitions() {
        assertThat(OrderStatus.DELIVERED.isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(OrderStatus.DELIVERED.allowedTransitions()).isEmpty();
        assertThat(OrderStatus.CANCELLED.allowedTransitions()).isEmpty();
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void nullTargetIsNeverAllowed() {
        assertThat(OrderStatus.PENDING.canTransitionTo(null)).isFalse();
    }

    @Test
    void transitionSetsAreImmutable() {
        assertThatThrownBy(() -> OrderStatus.PENDING.allowedTransitions().add(OrderStatus.SHIPPED))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
