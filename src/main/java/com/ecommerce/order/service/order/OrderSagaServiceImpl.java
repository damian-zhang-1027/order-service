package com.ecommerce.order.service.order;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.kafka.dto.EventMetadata;
import com.ecommerce.order.kafka.dto.SagaEventPayload;
import com.ecommerce.order.model.db.entity.Order;
import com.ecommerce.order.model.db.entity.OutboxEvent;
import com.ecommerce.order.repository.db.OrderRepository;
import com.ecommerce.order.repository.db.OutboxEventRepository;
import com.ecommerce.order.util.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaServiceImpl implements OrderSagaService {

    private static final String TOPIC_ORDERS = "orders";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String EVENT_TYPE_PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    private static final String EVENT_TYPE_PAYMENT_FAILED = "PAYMENT_FAILED";
    private static final String EVENT_TYPE_ORDER_SUCCEEDED = "ORDER_SUCCEEDED";
    private static final String EVENT_TYPE_ORDER_FAILED = "ORDER_FAILED";

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonUtil jsonUtil;

    @Override
    @Transactional
    public void processPaymentResult(OutboxEvent incomingEvent) {
        String eventType = incomingEvent.getEventType();

        SagaEventPayload payload = jsonUtil.fromJson(incomingEvent.getPayload(), SagaEventPayload.class);
        EventMetadata metadata = jsonUtil.fromJson(incomingEvent.getMetadata(), EventMetadata.class);

        Long orderId = payload.orderId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("OrderNotFoundException for orderId: {}", orderId);
                    return new OrderNotFoundException(orderId);
                });

        String newOrderStatus;
        String newEventType;

        if (EVENT_TYPE_PAYMENT_SUCCEEDED.equals(eventType)) {
            newOrderStatus = STATUS_SUCCEEDED;
            newEventType = EVENT_TYPE_ORDER_SUCCEEDED;
        } else if (EVENT_TYPE_PAYMENT_FAILED.equals(eventType)) {
            newOrderStatus = STATUS_FAILED;
            newEventType = EVENT_TYPE_ORDER_FAILED;
        } else {
            log.warn("Ignoring unknown event type: {}", eventType);
            return;
        }

        order.setStatus(newOrderStatus);
        orderRepository.save(order);

        EventMetadata outgoingMetadata = EventMetadata.builder()
                .traceId(metadata.traceId())
                .causationId(incomingEvent.getEventId())
                .userId(metadata.userId())
                .timestamp(Instant.now().toEpochMilli())
                .build();

        OutboxEvent outgoingEvent = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType(TOPIC_ORDERS)
                .aggregateId(order.getId().toString())
                .eventType(newEventType)
                .payload(incomingEvent.getPayload())
                .metadata(jsonUtil.toJson(outgoingMetadata))
                .status(STATUS_PENDING)
                .build();

        outboxEventRepository.save(outgoingEvent);

        log.info("[Saga] Processed event '{}'. Updated orderId={} to status={}. Created outbox event '{}'.",
                eventType, orderId, newOrderStatus, newEventType);
    }
}
