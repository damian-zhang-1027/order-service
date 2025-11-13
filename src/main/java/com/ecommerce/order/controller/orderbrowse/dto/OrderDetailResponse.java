package com.ecommerce.order.controller.orderbrowse.dto;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.ecommerce.order.model.db.entity.Order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed view of an order, including items")
public record OrderDetailResponse(
        @Schema(description = "Order ID", example = "12345") Long orderId,

        @Schema(description = "Buyer's User ID", example = "456") Long buyerUserId,

        @Schema(description = "Saga Status", example = "PENDING") String status,

        @Schema(description = "Total amount in cents", example = "5999") Long totalAmount,

        @Schema(description = "Creation timestamp") Instant createdAt,

        @Schema(description = "List of items in this order") List<OrderItemResponse> items) {
    public OrderDetailResponse(Order entity) {
        this(
                entity.getId(),
                entity.getBuyerUserId(),
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getCreatedAt(),
                entity.getItems().stream()
                        .map(OrderItemResponse::new)
                        .collect(Collectors.toList()));
    }
}
