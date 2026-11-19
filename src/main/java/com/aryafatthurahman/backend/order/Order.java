package com.aryafatthurahman.backend.order;

import com.aryafatthurahman.backend.pricing.MoneyCalculator;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order extends PanacheEntity {

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 320)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(UUID publicId,
                 String customerId,
                 String customerName,
                 String customerEmail,
                 String currency,
                 String idempotencyKey) {
        this.publicId = Objects.requireNonNull(publicId, "publicId must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.customerName = Objects.requireNonNull(customerName, "customerName must not be null");
        this.customerEmail = Objects.requireNonNull(customerEmail, "customerEmail must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.idempotencyKey = idempotencyKey;
        this.status = OrderStatus.PENDING;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addItem(OrderItem item) {
        Objects.requireNonNull(item, "item must not be null");
        item.attachTo(this);
        items.add(item);
    }

    /**
     * Recomputes all monetary fields from the current line items. Called after
     * every mutation that can change the basket.
     */
    public void recalculateTotals(BigDecimal taxRate) {
        List<MoneyCalculator.Line> lines = items.stream()
                .map(item -> new MoneyCalculator.Line(item.getUnitPrice(), item.getQuantity()))
                .toList();
        this.subtotal = MoneyCalculator.subtotal(lines);
        this.taxAmount = MoneyCalculator.tax(subtotal, taxRate);
        this.totalAmount = MoneyCalculator.total(subtotal, taxAmount);
    }

    public void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Invalid transition from " + status + " to " + target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }
}
