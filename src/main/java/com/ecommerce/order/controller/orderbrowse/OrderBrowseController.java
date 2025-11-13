package com.ecommerce.order.controller.orderbrowse;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.controller.orderbrowse.dto.OrderDetailResponse;
import com.ecommerce.order.controller.orderbrowse.dto.OrderSummaryResponse;
import com.ecommerce.order.framework.response.GlobalResponse;
import com.ecommerce.order.framework.response.dto.PaginationMeta;
import com.ecommerce.order.service.orderbrowse.OrderBrowseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Order (Buyer API)", description = "APIs for buyers to manage their orders. Requires ROLE_BUYER_USER.")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('ROLE_BUYER_USER')")
public class OrderBrowseController {

    private final OrderBrowseService orderBrowseService;

    @Operation(summary = "Get my order list (Paginated)", description = "Get a paginated list of orders *owned* by the currently authenticated buyer.")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedOrderSummaryResponseWrapper.class)))
    @GetMapping
    public GlobalResponse<List<OrderSummaryResponse>> getMyOrders(
            @Parameter(hidden = true) @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderSummaryResponse> orderPage = orderBrowseService.getMyOrders(pageable);
        PaginationMeta meta = new PaginationMeta(orderPage);
        return GlobalResponse.success(orderPage.getContent(), meta);
    }

    @Operation(summary = "Get my single order details", description = "Get details for a *specific* order *owned* by the currently authenticated buyer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order details retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDetailResponseWrapper.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden (User does not own this order)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GlobalResponse.class)))
    })
    @GetMapping("/{orderId}")
    public GlobalResponse<OrderDetailResponse> getMyOrderById(
            @Parameter(description = "The ID of the order to retrieve", example = "12345") @PathVariable Long orderId) {
        OrderDetailResponse order = orderBrowseService.getMyOrderById(orderId);
        return GlobalResponse.success(order);
    }

    @Schema(description = "Paginated response wrapper for Order Summaries")
    private static class PaginatedOrderSummaryResponseWrapper {
        @Schema(example = "0")
        public int retCode;
        @Schema(description = "The list of order summaries for the current page")
        public List<OrderSummaryResponse> data;
        @Schema(description = "Pagination metadata")
        public PaginationMeta meta;
    }

    @Schema(description = "Response wrapper for a Single Order Detail")
    private static class OrderDetailResponseWrapper {
        @Schema(example = "0")
        public int retCode;
        @Schema(description = "The order details")
        public OrderDetailResponse data;
        @Schema(nullable = true)
        public Object meta;
    }
}
