package com.aryafatthurahman.backend.order;

import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle. The transition table is the single source of truth used by
 * the service layer and documented in docs/data-model.md.
 */
public enum OrderStatus {

    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(PROCESSING, CANCELLED),
            PROCESSING, Set.of(SHIPPED, CANCELLED),
            SHIPPED, Set.of(DELIVERED),
            DELIVERED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return target != null && ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public Set<OrderStatus> allowedTransitions() {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of());
    }
}
