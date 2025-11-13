package com.ecommerce.order.controller.orderbrowse.dto;

import java.time.Instant;

import com.ecommerce.order.model.db.entity.Order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary view of an order (for lists)")
public record OrderSummaryResponse(
        @Schema(description = "Order ID", example = "12345") Long orderId,

        @Schema(description = "Saga Status", example = "PENDING") String status,

        @Schema(description = "Total amount in cents", example = "5999") Long totalAmount,

        @Schema(description = "Creation timestamp") Instant createdAt) {
    public OrderSummaryResponse(Order entity) {
        this(
                entity.getId(),
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getCreatedAt());
    }
}
