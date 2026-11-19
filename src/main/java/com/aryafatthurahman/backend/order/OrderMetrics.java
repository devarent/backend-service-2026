package com.aryafatthurahman.backend.order;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Business metrics exported through Micrometer/Prometheus. Tag values are
 * bounded by the enum set, keeping cardinality predictable.
 */
@ApplicationScoped
public class OrderMetrics {

    private final MeterRegistry registry;

    @Inject
    public OrderMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void orderCreated() {
        registry.counter("orders.created").increment();
    }

    public void orderCancelled() {
        registry.counter("orders.cancelled").increment();
    }

    public void statusChanged(OrderStatus from, OrderStatus to) {
        registry.counter("orders.status.transitions", "from", from.name(), "to", to.name()).increment();
    }
}
