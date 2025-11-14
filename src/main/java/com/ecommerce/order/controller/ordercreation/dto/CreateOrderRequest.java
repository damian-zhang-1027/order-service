package com.ecommerce.order.controller.ordercreation.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request body for creating a new order")
public record CreateOrderRequest(
        @Schema(description = "List of items to purchase") @NotNull @NotEmpty @Valid List<ItemRequest> items) {
    @Schema(description = "A single item to purchase")
    public record ItemRequest(
            @Schema(description = "Product ID", example = "101") @NotNull Long productId,
            @Schema(description = "Quantity", example = "1") @NotNull @Positive Integer quantity) {
    }
}
