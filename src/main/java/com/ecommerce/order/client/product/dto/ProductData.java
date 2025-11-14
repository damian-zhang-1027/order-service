package com.ecommerce.order.client.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductData(
        Long id,
        Long sellerAdminId,
        Long price,
        Integer stockAvailable) {
}
