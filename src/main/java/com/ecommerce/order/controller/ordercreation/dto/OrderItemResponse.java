package com.ecommerce.order.controller.ordercreation.dto;

import com.ecommerce.order.model.db.entity.OrderItem;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details of a single item in an order")
public record OrderItemResponse(
        @Schema(description = "Product ID", example = "101") Long productId,
        @Schema(description = "Quantity purchased", example = "1") Integer quantity,
        @Schema(description = "Price of the product *at time of purchase*", example = "1999") Long unitPrice) {
    public OrderItemResponse(OrderItem entity) {
        this(
                entity.getProductId(),
                entity.getQuantity(),
                entity.getUnitPrice());
    }
}
