package com.ecommerce.order.controller.ordercreation;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.controller.ordercreation.dto.CreateOrderRequest;
import com.ecommerce.order.controller.ordercreation.dto.OrderDetailResponse;
import com.ecommerce.order.framework.response.GlobalResponse;
import com.ecommerce.order.service.ordercreation.OrderCreationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Order (Buyer API)", description = "APIs for buyers to manage their orders. Requires ROLE_BUYER_USER.")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OrderCreationController {

    private final OrderCreationService orderCreationService;

    @Operation(summary = "Create a new order (Saga Initiator)", description = "Create a new order (status PENDING) and trigger the purchase Saga. Requires ROLE_BUYER_USER.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Order accepted for asynchronous processing", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDetailResponseWrapper.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., validation error, stock unavailable, seller buying own product)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GlobalResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed (Invalid or missing token)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GlobalResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('ROLE_BUYER_USER')")
    public GlobalResponse<OrderDetailResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderDetailResponse responseData = orderCreationService.createOrder(request);
        return GlobalResponse.success(responseData);
    }

    @Schema(description = "Response wrapper for a Single Order Detail")
    private static class OrderDetailResponseWrapper {
        @Schema(example = "0")
        public int retCode;
        @Schema(description = "The order details (status PENDING)")
        public OrderDetailResponse data;
        @Schema(nullable = true)
        public Object meta;
    }
}
