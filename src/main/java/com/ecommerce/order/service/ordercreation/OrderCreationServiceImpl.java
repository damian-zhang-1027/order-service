package com.ecommerce.order.service.ordercreation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.order.client.product.ProductClient;
import com.ecommerce.order.client.product.dto.ProductApiResponse;
import com.ecommerce.order.client.product.dto.ProductData;
import com.ecommerce.order.controller.ordercreation.dto.CreateOrderRequest;
import com.ecommerce.order.controller.ordercreation.dto.OrderDetailResponse;
import com.ecommerce.order.exception.InsufficientStockException;
import com.ecommerce.order.exception.ProductFetchException;
import com.ecommerce.order.exception.SaaSValidationException;
import com.ecommerce.order.kafka.dto.EventMetadata;
import com.ecommerce.order.kafka.dto.OrderItemDto;
import com.ecommerce.order.model.db.entity.Order;
import com.ecommerce.order.model.db.entity.OrderItem;
import com.ecommerce.order.model.db.entity.OutboxEvent;
import com.ecommerce.order.repository.db.OrderItemRepository;
import com.ecommerce.order.repository.db.OrderRepository;
import com.ecommerce.order.repository.db.OutboxEventRepository;
import com.ecommerce.order.util.JsonUtil;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreationServiceImpl implements OrderCreationService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProductClient productClient;
    private final JsonUtil jsonUtil;
    private final Tracer tracer;

    private static final String EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED";
    private static final String TOPIC_ORDERS = "orders";
    private static final String STATUS_PENDING = "PENDING";

    @Override
    @Transactional
    public OrderDetailResponse createOrder(CreateOrderRequest request) {

        Long buyerUserId = getAuthenticatedBuyerId();
        log.info("Attempting to create order for buyerUserId: {}", buyerUserId);

        List<ProductData> fetchedProducts = new ArrayList<>();
        for (CreateOrderRequest.ItemRequest itemReq : request.items()) {
            try {
                ProductApiResponse response = productClient.getProductById(itemReq.productId());
                if (response == null || response.data() == null) {
                    throw new ProductFetchException("Product not found (null response): " + itemReq.productId());
                }
                fetchedProducts.add(response.data());
            } catch (Exception e) {
                log.warn("Failed to fetch product data for productId: {}", itemReq.productId(), e);
                throw new ProductFetchException("Product not found or service unavailable: " + itemReq.productId());
            }
        }

        Map<Long, Integer> requestedQuantities = request.items().stream()
                .collect(Collectors.toMap(CreateOrderRequest.ItemRequest::productId,
                        CreateOrderRequest.ItemRequest::quantity));
        long totalAmount = 0L;
        List<OrderItem> orderItems = new ArrayList<>();

        for (ProductData product : fetchedProducts) {
            int requestedQuantity = requestedQuantities.get(product.id());
            if (product.sellerAdminId().equals(buyerUserId)) {
                throw new SaaSValidationException("You cannot purchase your own products.");
            }
            if (product.stockAvailable() < requestedQuantity) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.id());
            }

            OrderItem item = new OrderItem();
            item.setProductId(product.id());
            item.setSellerAdminId(product.sellerAdminId());
            item.setQuantity(requestedQuantity);
            item.setUnitPrice(product.price());
            orderItems.add(item);
            totalAmount += (product.price() * requestedQuantity);
        }

        Order newOrder = new Order();
        newOrder.setBuyerUserId(buyerUserId);
        newOrder.setTotalAmount(totalAmount);
        newOrder.setStatus("PENDING");
        Order savedOrder = orderRepository.save(newOrder);

        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getId());
        }
        orderItemRepository.saveAll(orderItems);

        EventMetadata metadata = buildEventMetadata(buyerUserId);
        Map<String, Object> payloadMap = Map.of(
                "orderId", savedOrder.getId(),
                "totalAmount", totalAmount,
                "items", orderItems.stream()
                        .map(item -> new OrderItemDto(item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList()));

        OutboxEvent event = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType(TOPIC_ORDERS)
                .aggregateId(savedOrder.getId().toString())
                .eventType(EVENT_TYPE_ORDER_CREATED)
                .payload(jsonUtil.toJson(payloadMap))
                .metadata(jsonUtil.toJson(metadata))
                .status(STATUS_PENDING)
                .build();

        outboxEventRepository.save(event);

        log.info("Order {} created and Outbox event saved.", savedOrder.getId());

        savedOrder.setItems(orderItems);
        return new OrderDetailResponse(savedOrder);
    }

    private Long getAuthenticatedBuyerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Long.parseLong(jwt.getSubject());
    }

    private EventMetadata buildEventMetadata(Long currentUserId) {
        String traceId = "N/A_TRACE_ID";
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            traceId = currentSpan.context().traceId();
        }

        return new EventMetadata(
                traceId,
                traceId,
                currentUserId.toString(),
                Instant.now().toEpochMilli());
    }
}
