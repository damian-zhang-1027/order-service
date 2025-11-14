package com.ecommerce.order.client.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductApiResponse(
        ProductData data) {
}
