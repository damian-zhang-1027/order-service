package com.ecommerce.order.client.product;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ecommerce.order.client.product.dto.ProductApiResponse;

//Feign Client will automatically propagate Tracing Headers (traceId)
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/v1/products/{productId}")
    ProductApiResponse getProductById(@PathVariable("productId") Long productId);
}
