package com.aryafatthurahman.backend.order;

import com.aryafatthurahman.backend.cache.RevenueCache;
import com.aryafatthurahman.backend.common.BusinessValidationException;
import com.aryafatthurahman.backend.common.ConflictException;
import com.aryafatthurahman.backend.common.NotFoundException;
import com.aryafatthurahman.backend.common.PageResponse;
import com.aryafatthurahman.backend.config.AppConfig;
import com.aryafatthurahman.backend.order.dto.CreateOrderItemRequest;
import com.aryafatthurahman.backend.order.dto.CreateOrderRequest;
import com.aryafatthurahman.backend.order.dto.OrderResponse;
import com.aryafatthurahman.backend.order.dto.OrderSummaryResponse;
import com.aryafatthurahman.backend.order.dto.RevenueByStatusResponse;
import com.aryafatthurahman.backend.order.dto.RevenueStatsResponse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Transactional application service. All business rules live here so the REST
 * resource stays a thin HTTP adapter and the rules remain testable directly.
 */
@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._:-]{8,128}");
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final RevenueCache revenueCache;
    private final OrderMetrics metrics;
    private final AppConfig config;

    @Inject
    public OrderService(OrderRepository repository,
                        OrderMapper mapper,
                        RevenueCache revenueCache,
                        OrderMetrics metrics,
                        AppConfig config) {
        this.repository = repository;
        this.mapper = mapper;
        this.revenueCache = revenueCache;
        this.metrics = metrics;
        this.config = config;
    }

    @Transactional
    public CreateOrderResult create(CreateOrderRequest request, String idempotencyKey) {
        Objects.requireNonNull(request, "request must not be null");
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);

        if (normalizedKey != null) {
            Optional<Order> existing = repository.findByIdempotencyKey(normalizedKey);
            if (existing.isPresent()) {
                LOG.infof("order.create.idempotent-hit key=%s orderId=%s", normalizedKey, existing.get().getPublicId());
                return new CreateOrderResult(mapper.toResponse(existing.get()), false);
            }
        }

        validateItems(request.items());
        String currency = resolveCurrency(request.currency());

        Order order = new Order(
                UUID.randomUUID(),
                request.customerId().trim(),
                request.customerName().trim(),
                request.customerEmail().trim(),
                currency,
                normalizedKey);

        request.items().forEach(item -> order.addItem(new OrderItem(
                normalizeSku(item.sku()),
                item.name().trim(),
                item.quantity(),
                item.unitPrice())));

        order.recalculateTotals(config.pricing().taxRate());
        repository.persist(order);
        repository.flush();

        metrics.orderCreated();
        LOG.infof("order.created id=%s status=%s total=%s currency=%s",
                order.getPublicId(), order.getStatus(), order.getTotalAmount(), order.getCurrency());

        return new CreateOrderResult(mapper.toResponse(order), true);
    }

    @Transactional
    public OrderResponse get(UUID id) {
        return mapper.toResponse(requireOrder(id));
    }

    @Transactional
    public PageResponse<OrderSummaryResponse> list(OrderStatus status, int page, int size) {
        if (page < 0) {
            throw new BusinessValidationException("INVALID_PAGE", "page must be greater than or equal to zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessValidationException("INVALID_PAGE_SIZE", "size must be between 1 and " + MAX_PAGE_SIZE);
        }

        PanacheQuery<Order> query = repository.findPage(status);
        long totalElements = query.count();
        List<OrderSummaryResponse> content = query.page(page, size).list().stream()
                .map(mapper::toSummary)
                .toList();
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);

        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                totalPages == 0 || page >= totalPages - 1);
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, OrderStatus target) {
        if (target == null) {
            throw new BusinessValidationException("INVALID_STATUS", "target status is required");
        }
        Order order = requireOrder(id);
        OrderStatus current = order.getStatus();

        if (current == target) {
            return mapper.toResponse(order);
        }
        if (!current.canTransitionTo(target)) {
            throw new ConflictException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot transition order from " + current + " to " + target);
        }

        order.transitionTo(target);
        repository.flush();
        metrics.statusChanged(current, target);
        if (target == OrderStatus.CANCELLED) {
            metrics.orderCancelled();
        }
        LOG.infof("order.status.changed id=%s from=%s to=%s", order.getPublicId(), current, target);
        return mapper.toResponse(order);
    }

    /**
     * Soft cancel. Repeating the operation is safe and returns the already
     * cancelled order. Shipped and delivered orders are immutable.
     */
    @Transactional
    public OrderResponse cancel(UUID id) {
        Order order = requireOrder(id);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return mapper.toResponse(order);
        }
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new ConflictException(
                    "ORDER_NOT_CANCELLABLE",
                    "Orders that are " + order.getStatus() + " cannot be cancelled");
        }

        OrderStatus previous = order.getStatus();
        order.transitionTo(OrderStatus.CANCELLED);
        repository.flush();
        metrics.orderCancelled();
        LOG.infof("order.cancelled id=%s previousStatus=%s", order.getPublicId(), previous);
        return mapper.toResponse(order);
    }

    @Transactional
    public RevenueStatsResponse revenue() {
        Optional<RevenueStatsResponse> cached = revenueCache.get();
        if (cached.isPresent()) {
            return cached.get();
        }

        List<RevenueByStatus> rows = repository.revenueByStatus(OrderStatus.CANCELLED);
        BigDecimal totalRevenue = rows.stream()
                .map(RevenueByStatus::revenue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        long totalOrders = rows.stream()
                .mapToLong(row -> row.orderCount() == null ? 0 : row.orderCount())
                .sum();

        RevenueStatsResponse stats = new RevenueStatsResponse(
                config.orders().currency(),
                totalRevenue,
                totalOrders,
                rows.stream()
                        .map(row -> new RevenueByStatusResponse(
                                row.status(),
                                row.orderCount() == null ? 0 : row.orderCount(),
                                row.revenue()))
                        .toList(),
                Instant.now());

        revenueCache.put(stats);
        return stats;
    }

    private Order requireOrder(UUID id) {
        if (id == null) {
            throw new BusinessValidationException("INVALID_ORDER_ID", "order id is required");
        }
        return repository.findByPublicId(id)
                .orElseThrow(() -> new NotFoundException("Order " + id + " was not found"));
    }

    private void validateItems(List<CreateOrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessValidationException("INVALID_ITEMS", "an order must contain at least one item");
        }
        if (items.size() > config.orders().maxItemsPerOrder()) {
            throw new BusinessValidationException(
                    "TOO_MANY_ITEMS",
                    "an order may contain at most " + config.orders().maxItemsPerOrder() + " items");
        }

        Set<String> seenSkus = new HashSet<>();
        for (CreateOrderItemRequest item : items) {
            if (item == null || item.sku() == null) {
                throw new BusinessValidationException("INVALID_ITEM", "each order item must have a sku");
            }
            String sku = normalizeSku(item.sku());
            if (!seenSkus.add(sku)) {
                throw new BusinessValidationException("DUPLICATE_SKU", "Duplicate sku in order: " + sku);
            }
        }
    }

    private String resolveCurrency(String requestedCurrency) {
        String configured = config.orders().currency().toUpperCase(Locale.ROOT);
        if (requestedCurrency == null || requestedCurrency.isBlank()) {
            return configured;
        }
        String requested = requestedCurrency.trim().toUpperCase(Locale.ROOT);
        if (!configured.equals(requested)) {
            throw new BusinessValidationException(
                    "UNSUPPORTED_CURRENCY",
                    "Only " + configured + " orders are supported by this deployment");
        }
        return configured;
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        if (!IDEMPOTENCY_KEY.matcher(trimmed).matches()) {
            throw new BusinessValidationException(
                    "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key must be 8-128 characters using letters, digits, dot, underscore, colon or dash");
        }
        return trimmed;
    }
}
