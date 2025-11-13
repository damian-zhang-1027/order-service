package com.ecommerce.order.service.orderbrowse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.order.controller.orderbrowse.dto.OrderDetailResponse;
import com.ecommerce.order.controller.orderbrowse.dto.OrderSummaryResponse;
import com.ecommerce.order.exception.OrderAccessDeniedException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.model.db.entity.Order;
import com.ecommerce.order.repository.db.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderBrowseServiceImpl implements OrderBrowseService {

    private final OrderRepository orderRepository;

    @Override
    public Page<OrderSummaryResponse> getMyOrders(Pageable pageable) {
        Long buyerUserId = getAuthenticatedBuyerId();

        log.info("Fetching order list for buyerUserId: {} (Page: {}, Size: {})",
                buyerUserId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Order> orderPage = orderRepository.findByBuyerUserId(buyerUserId, pageable);

        return orderPage.map(OrderSummaryResponse::new);
    }

    @Override
    public OrderDetailResponse getMyOrderById(Long orderId) {
        Long buyerUserId = getAuthenticatedBuyerId();

        log.info("Fetching order detail for buyerUserId: {}, orderId: {}",
                buyerUserId, orderId);

        Order order = orderRepository.findByIdAndBuyerUserIdWithItems(orderId, buyerUserId)
                .orElseGet(() -> getOrderAndVerifyOwnership(orderId, buyerUserId));

        return new OrderDetailResponse(order);

    }

    private Long getAuthenticatedBuyerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Long.parseLong(jwt.getSubject());
    }

    private Order getOrderAndVerifyOwnership(Long orderId, Long buyerUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("OrderNotFoundException for orderId: {}", orderId);
                    return new OrderNotFoundException(orderId);
                });

        if (!order.getBuyerUserId().equals(buyerUserId)) {
            log.warn("OrderAccessDeniedException: BuyerId {} attempted to access orderId {} owned by {}",
                    buyerUserId, orderId, order.getBuyerUserId());
            throw new OrderAccessDeniedException("You do not have permission to access this order.");
        }

        return order;
    }
}
