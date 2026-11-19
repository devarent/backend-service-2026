package com.aryafatthurahman.backend.order;

import com.aryafatthurahman.backend.cache.RevenueCache;
import com.aryafatthurahman.backend.common.BusinessValidationException;
import com.aryafatthurahman.backend.common.ConflictException;
import com.aryafatthurahman.backend.common.NotFoundException;
import com.aryafatthurahman.backend.common.PageResponse;
import com.aryafatthurahman.backend.config.AppConfig;
import com.aryafatthurahman.backend.order.dto.CreateOrderItemRequest;
import com.aryafatthurahman.backend.order.dto.CreateOrderRequest;
import com.aryafatthurahman.backend.order.dto.OrderSummaryResponse;
import com.aryafatthurahman.backend.order.dto.RevenueByStatusResponse;
import com.aryafatthurahman.backend.order.dto.RevenueStatsResponse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the transactional service. All collaborators are mocked,
 * so no database, Redis, or Quarkus runtime is involved.
 */
class OrderServiceTest {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.0825");

    private OrderRepository repository;
    private RevenueCache revenueCache;
    private OrderMetrics metrics;
    private AppConfig config;
    private AppConfig.Pricing pricing;
    private AppConfig.Orders orders;
    private AppConfig.Cache cache;
    private OrderService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrderRepository.class);
        revenueCache = mock(RevenueCache.class);
        metrics = mock(OrderMetrics.class);
        config = mock(AppConfig.class);
        pricing = mock(AppConfig.Pricing.class);
        orders = mock(AppConfig.Orders.class);
        cache = mock(AppConfig.Cache.class);

        when(config.pricing()).thenReturn(pricing);
        when(config.orders()).thenReturn(orders);
        when(config.cache()).thenReturn(cache);
        when(pricing.taxRate()).thenReturn(TAX_RATE);
        when(orders.currency()).thenReturn("USD");
        when(orders.maxItemsPerOrder()).thenReturn(100);
        when(cache.enabled()).thenReturn(false);
        when(cache.redisEnabled()).thenReturn(false);
        when(cache.ttl()).thenReturn(Duration.ofSeconds(30));

        when(revenueCache.get()).thenReturn(Optional.empty());
        when(repository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

        service = new OrderService(repository, new OrderMapper(), revenueCache, metrics, config);
    }

    @Test
    void createPersistsOrderAndComputesTotalsAndNormalizesSkus() {
        CreateOrderRequest request = request("USD", List.of(
                item("sku-1", 2, "10.00"),
                item("sku-2", 3, "5.00")));

        CreateOrderResult result = service.create(request, "idem-key-0001");

        assertThat(result.created()).isTrue();
        assertThat(result.order().status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.order().currency()).isEqualTo("USD");
        assertThat(result.order().customerId()).isEqualTo("CUST-1");
        assertThat(result.order().subtotal()).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat(result.order().taxAmount()).isEqualByComparingTo(new BigDecimal("2.89"));
        assertThat(result.order().totalAmount()).isEqualByComparingTo(new BigDecimal("37.89"));
        assertThat(result.order().items()).hasSize(2);
        assertThat(result.order().items().get(0).sku()).isEqualTo("SKU-1");
        assertThat(result.order().items().get(0).lineTotal())
                .isEqualByComparingTo(new BigDecimal("20.00"));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(repository).persist(captor.capture());
        verify(repository).flush();
        verify(metrics).orderCreated();
        verify(repository).findByIdempotencyKey("idem-key-0001");
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("idem-key-0001");
    }

    @Test
    void createUsesConfiguredCurrencyWhenRequestOmitsIt() {
        CreateOrderRequest request = request(null, List.of(item("sku-1", 1, "10.00")));

        CreateOrderResult result = service.create(request, null);

        assertThat(result.created()).isTrue();
        assertThat(result.order().currency()).isEqualTo("USD");
        verify(repository, never()).findByIdempotencyKey(anyString());
    }

    @Test
    void createReturnsExistingOrderForRepeatedIdempotencyKey() {
        Order existing = orderWithStatus(OrderStatus.CONFIRMED);
        when(repository.findByIdempotencyKey("idem-key-0001")).thenReturn(Optional.of(existing));

        CreateOrderResult result = service.create(
                request("USD", List.of(item("sku-1", 1, "1.00"))), "idem-key-0001");

        assertThat(result.created()).isFalse();
        assertThat(result.order().id()).isEqualTo(existing.getPublicId());
        assertThat(result.order().status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(repository, never()).persist(any(Order.class));
        verify(repository, never()).flush();
        verify(metrics, never()).orderCreated();
    }

    @Test
    void createRejectsEmptyItemsAndDuplicateSkus() {
        assertThatThrownBy(() -> service.create(request("USD", List.of()), null))
                .isInstanceOfSatisfying(BusinessValidationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_ITEMS"));

        CreateOrderRequest duplicate = request("USD", List.of(
                item("sku-1", 1, "1.00"), item("SKU-1", 2, "2.00")));
        assertThatThrownBy(() -> service.create(duplicate, null))
                .isInstanceOfSatisfying(BusinessValidationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("DUPLICATE_SKU"));

        verify(repository, never()).persist(any(Order.class));
    }

    @Test
    void createRejectsUnsupportedCurrencyAndTooManyItems() {
        assertThatThrownBy(() -> service.create(request("EUR", List.of(item("sku-1", 1, "1.00"))), null))
                .isInstanceOfSatisfying(BusinessValidationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("UNSUPPORTED_CURRENCY"));

        List<CreateOrderItemRequest> tooMany = IntStream.range(0, 101)
                .mapToObj(index -> item("sku-" + index, 1, "1.00"))
                .toList();
        assertThatThrownBy(() -> service.create(request("USD", tooMany), null))
                .isInstanceOfSatisfying(BusinessValidationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TOO_MANY_ITEMS"));
    }

    @Test
    void createRejectsMalformedIdempotencyKey() {
        assertThatThrownBy(() -> service.create(
                request("USD", List.of(item("sku-1", 1, "1.00"))), "short"))
                .isInstanceOfSatisfying(BusinessValidationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_IDEMPOTENCY_KEY"));
    }

    @Test
    void getMapsFoundOrderAndThrowsForUnknownOrNullId() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(repository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

        assertThat(service.get(order.getPublicId()).id()).isEqualTo(order.getPublicId());
        assertThat(service.get(order.getPublicId()).items()).hasSize(1);

        UUID missing = UUID.randomUUID();
        when(repository.findByPublicId(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(missing)).isInstanceOf(NotFoundException.class);

        assertThatThrownBy(() -> service.get(null))
                .isInstanceOfSatisfying(BusinessValidationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_ORDER_ID"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listValidatesPaginationAndMapsPageResponse() {
        assertThatThrownBy(() -> service.list(null, -1, 20))
                .isInstanceOfSatisfying(BusinessValidationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_PAGE"));
        assertThatThrownBy(() -> service.list(null, 0, 0))
                .isInstanceOfSatisfying(BusinessValidationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_PAGE_SIZE"));
        assertThatThrownBy(() -> service.list(null, 0, 101))
                .isInstanceOfSatisfying(BusinessValidationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_PAGE_SIZE"));

        PanacheQuery<Order> query = mock(PanacheQuery.class);
        Order first = orderWithStatus(OrderStatus.PENDING);
        Order second = orderWithStatus(OrderStatus.CONFIRMED);
        when(repository.findPage(OrderStatus.PENDING)).thenReturn(query);
        when(query.count()).thenReturn(2L);
        when(query.page(0, 20)).thenReturn(query);
        when(query.list()).thenReturn(List.of(first, second));

        PageResponse<OrderSummaryResponse> page = service.list(OrderStatus.PENDING, 0, 20);

        assertThat(page.content()).hasSize(2);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isTrue();
        assertThat(page.content().get(0).status()).isEqualTo(OrderStatus.PENDING);
        assertThat(page.content().get(1).itemCount()).isEqualTo(1);
        verify(repository).findPage(OrderStatus.PENDING);
    }

    @Test
    void updateStatusAppliesAllowedTransitionAndRejectsInvalidOne() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(repository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

        assertThat(service.updateStatus(order.getPublicId(), OrderStatus.CONFIRMED).status())
                .isEqualTo(OrderStatus.CONFIRMED);
        verify(repository).flush();
        verify(metrics).statusChanged(OrderStatus.PENDING, OrderStatus.CONFIRMED);

        assertThatThrownBy(() -> service.updateStatus(order.getPublicId(), OrderStatus.DELIVERED))
                .isInstanceOfSatisfying(ConflictException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void updateStatusWithSameStatusIsNoOp() {
        Order order = orderWithStatus(OrderStatus.CONFIRMED);
        when(repository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

        assertThat(service.updateStatus(order.getPublicId(), OrderStatus.CONFIRMED).status())
                .isEqualTo(OrderStatus.CONFIRMED);
        verify(repository, never()).flush();
        verify(metrics, never()).statusChanged(any(), any());

        assertThatThrownBy(() -> service.updateStatus(order.getPublicId(), null))
                .isInstanceOfSatisfying(BusinessValidationException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATUS"));
    }

    @Test
    void cancelMovesPendingToCancelledAndIsIdempotent() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(repository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

        assertThat(service.cancel(order.getPublicId()).status()).isEqualTo(OrderStatus.CANCELLED);
        verify(repository, times(1)).flush();
        verify(metrics, times(1)).orderCancelled();

        assertThat(service.cancel(order.getPublicId()).status()).isEqualTo(OrderStatus.CANCELLED);
        verify(repository, times(1)).flush();
        verify(metrics, times(1)).orderCancelled();
    }

    @Test
    void cancelRejectsShippedAndDeliveredOrders() {
        for (OrderStatus status : List.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED)) {
            Order order = orderWithStatus(status);
            when(repository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.cancel(order.getPublicId()))
                    .isInstanceOfSatisfying(ConflictException.class,
                            exception -> assertThat(exception.getCode()).isEqualTo("ORDER_NOT_CANCELLABLE"));
        }
    }

    @Test
    void revenueAggregatesRowsAndCachesResultWhenCacheIsEmpty() {
        when(revenueCache.get()).thenReturn(Optional.empty());
        when(repository.revenueByStatus(OrderStatus.CANCELLED)).thenReturn(List.of(
                new RevenueByStatus(OrderStatus.PENDING, 2L, new BigDecimal("20.00")),
                new RevenueByStatus(OrderStatus.DELIVERED, 1L, new BigDecimal("10.00")),
                new RevenueByStatus(OrderStatus.PROCESSING, null, null)));

        RevenueStatsResponse stats = service.revenue();

        assertThat(stats.currency()).isEqualTo("USD");
        assertThat(stats.totalOrders()).isEqualTo(3);
        assertThat(stats.totalRevenue()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(stats.byStatus()).hasSize(3);
        assertThat(stats.byStatus()).anySatisfy(row -> {
            assertThat(row.status()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(row.orderCount()).isZero();
        });
        verify(revenueCache).put(stats);
    }

    @Test
    void revenueReturnsCachedValueWithoutQueryingRepository() {
        RevenueStatsResponse cached = new RevenueStatsResponse(
                "USD", new BigDecimal("5.00"), 1, List.of(), Instant.now());
        when(revenueCache.get()).thenReturn(Optional.of(cached));

        assertThat(service.revenue()).isSameAs(cached);
        verify(repository, never()).revenueByStatus(any(OrderStatus.class));
    }

    private CreateOrderRequest request(String currency, List<CreateOrderItemRequest> items) {
        return new CreateOrderRequest("CUST-1", "Ada Lovelace", "ada@example.com", currency, items);
    }

    private CreateOrderItemRequest item(String sku, int quantity, String unitPrice) {
        return new CreateOrderItemRequest(sku, "Widget " + sku, quantity, new BigDecimal(unitPrice));
    }

    private Order orderWithStatus(OrderStatus target) {
        Order order = new Order(
                UUID.randomUUID(), "CUST-1", "Ada Lovelace", "ada@example.com", "USD", null);
        order.addItem(new OrderItem("SKU-1", "Widget", 2, new BigDecimal("10.00")));
        order.recalculateTotals(TAX_RATE);

        if (target == OrderStatus.CANCELLED) {
            order.transitionTo(OrderStatus.CANCELLED);
            return order;
        }

        while (order.getStatus() != target) {
            OrderStatus next = switch (order.getStatus()) {
                case PENDING -> OrderStatus.CONFIRMED;
                case CONFIRMED -> OrderStatus.PROCESSING;
                case PROCESSING -> OrderStatus.SHIPPED;
                case SHIPPED -> OrderStatus.DELIVERED;
                default -> throw new IllegalStateException("No forward path to " + target);
            };
            order.transitionTo(next);
        }
        return order;
    }
}
