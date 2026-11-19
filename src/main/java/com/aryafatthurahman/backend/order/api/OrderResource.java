package com.aryafatthurahman.backend.order.api;

import com.aryafatthurahman.backend.common.BusinessValidationException;
import com.aryafatthurahman.backend.common.PageResponse;
import com.aryafatthurahman.backend.order.CreateOrderResult;
import com.aryafatthurahman.backend.order.OrderService;
import com.aryafatthurahman.backend.order.OrderStatus;
import com.aryafatthurahman.backend.order.dto.CreateOrderRequest;
import com.aryafatthurahman.backend.order.dto.OrderResponse;
import com.aryafatthurahman.backend.order.dto.OrderSummaryResponse;
import com.aryafatthurahman.backend.order.dto.RevenueStatsResponse;
import com.aryafatthurahman.backend.order.dto.UpdateOrderStatusRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@Path("/api/v1/orders")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Orders", description = "Create, track, transition, cancel, and report on orders")
public class OrderResource {

    private final OrderService orderService;

    @Inject
    public OrderResource(OrderService orderService) {
        this.orderService = orderService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "createOrder",
            summary = "Create an order",
            description = "Validates line items, recalculates totals, and stores the order in PENDING status. "
                    + "Supply an Idempotency-Key header to make retries safe.")
    @APIResponse(responseCode = "201", description = "Order created")
    @APIResponse(responseCode = "200", description = "Existing order returned for a repeated idempotency key")
    @APIResponse(responseCode = "400", description = "Invalid payload or business rule violation")
    public Response create(@Valid CreateOrderRequest request,
                           @Parameter(description = "Optional 8-128 character retry key")
                           @HeaderParam("Idempotency-Key") String idempotencyKey) {
        CreateOrderResult result = orderService.create(request, idempotencyKey);
        Response.ResponseBuilder builder = result.created()
                ? Response.created(URI.create("/api/v1/orders/" + result.order().id()))
                : Response.ok();
        return builder.entity(result.order()).build();
    }

    @GET
    @Operation(operationId = "listOrders", summary = "List orders with pagination and optional status filter")
    @APIResponse(responseCode = "200", description = "Page of order summaries")
    @APIResponse(responseCode = "400", description = "Invalid pagination or status filter")
    public PageResponse<OrderSummaryResponse> list(
            @Parameter(description = "Optional order status filter, for example PENDING")
            @QueryParam("status") String status,
            @Parameter(description = "Zero-based page index")
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size, 1-100")
            @QueryParam("size") @DefaultValue("20") int size) {
        return orderService.list(parseStatus(status), page, size);
    }

    @GET
    @Path("/stats/revenue")
    @Operation(operationId = "getRevenueStats",
            summary = "Revenue grouped by status",
            description = "Aggregates non-cancelled orders with a single JPQL GROUP BY query. Cached for a short TTL.")
    @APIResponse(responseCode = "200", description = "Revenue aggregate")
    public RevenueStatsResponse revenue() {
        return orderService.revenue();
    }

    @GET
    @Path("/{id}")
    @Operation(operationId = "getOrder", summary = "Get an order by public UUID")
    @APIResponse(responseCode = "200", description = "Order found")
    @APIResponse(responseCode = "404", description = "Order not found")
    public OrderResponse get(@PathParam("id") UUID id) {
        return orderService.get(id);
    }

    @PATCH
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateOrderStatus",
            summary = "Transition an order to a new status",
            description = "Rejects transitions that are not allowed by the order state machine.")
    @APIResponse(responseCode = "200", description = "Status updated")
    @APIResponse(responseCode = "409", description = "Invalid transition or concurrent modification")
    public OrderResponse updateStatus(@PathParam("id") UUID id,
                                      @Valid UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    @DELETE
    @Path("/{id}")
    @Operation(operationId = "cancelOrder",
            summary = "Cancel an order (soft delete)",
            description = "Moves a non-shipped order to CANCELLED. The row is retained for audit and reporting.")
    @APIResponse(responseCode = "200", description = "Order cancelled or already cancelled")
    @APIResponse(responseCode = "409", description = "Order already shipped or delivered")
    public OrderResponse cancel(@PathParam("id") UUID id) {
        return orderService.cancel(id);
    }

    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("INVALID_STATUS_FILTER", "Unknown order status: " + status);
        }
    }
}
