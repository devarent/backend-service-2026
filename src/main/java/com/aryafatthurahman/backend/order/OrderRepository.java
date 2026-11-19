package com.aryafatthurahman.backend.order;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {

    public Optional<Order> findByPublicId(UUID publicId) {
        return find("publicId", publicId).firstResultOptional();
    }

    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return find("idempotencyKey", idempotencyKey).firstResultOptional();
    }

    /**
     * Returns a query ordered newest-first. Callers page it with Panache's
     * zero-based page API.
     */
    public PanacheQuery<Order> findPage(OrderStatus status) {
        if (status == null) {
            return find("order by createdAt desc, id desc");
        }
        return find("status = ?1 order by createdAt desc, id desc", status);
    }

    /**
     * Custom JPQL aggregate: revenue and order count grouped by status,
     * excluding cancelled orders. Demonstrates projection queries alongside
     * Panache's derived finders.
     */
    public List<RevenueByStatus> revenueByStatus(OrderStatus excludedStatus) {
        return getEntityManager()
                .createQuery("""
                        select new com.aryafatthurahman.backend.order.RevenueByStatus(
                            o.status, count(o), sum(o.totalAmount))
                        from Order o
                        where o.status <> :excludedStatus
                        group by o.status
                        order by o.status
                        """, RevenueByStatus.class)
                .setParameter("excludedStatus", excludedStatus)
                .getResultList();
    }
}
