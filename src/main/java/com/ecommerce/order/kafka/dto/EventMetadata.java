package com.ecommerce.order.kafka.dto;

public record EventMetadata(
        String traceId,
        String causationId,
        String userId,
        Long timestamp) {
}
