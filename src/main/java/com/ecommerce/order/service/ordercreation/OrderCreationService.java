package com.ecommerce.order.service.ordercreation;

import com.ecommerce.order.controller.ordercreation.dto.CreateOrderRequest;
import com.ecommerce.order.controller.ordercreation.dto.OrderDetailResponse;

public interface OrderCreationService {

    OrderDetailResponse createOrder(CreateOrderRequest request);
}
