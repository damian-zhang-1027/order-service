package com.ecommerce.order.service.orderbrowse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ecommerce.order.controller.orderbrowse.dto.OrderDetailResponse;
import com.ecommerce.order.controller.orderbrowse.dto.OrderSummaryResponse;

public interface OrderBrowseService {

    Page<OrderSummaryResponse> getMyOrders(Pageable pageable);

    OrderDetailResponse getMyOrderById(Long orderId);
}
