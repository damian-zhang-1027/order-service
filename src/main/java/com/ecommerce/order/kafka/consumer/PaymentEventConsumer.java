package com.ecommerce.order.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.ecommerce.order.model.db.entity.OutboxEvent;
import com.ecommerce.order.service.order.OrderSagaService;
import com.ecommerce.order.util.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderSagaService orderSagaService;
    private final JsonUtil jsonUtil;

    @KafkaListener(topics = "payments", groupId = "order-service-group")
    public void handlePaymentEvent(String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("[Consumer] Received event from 'payments' topic. Key: {}", key);

        try {
            OutboxEvent incomingEvent = jsonUtil.fromJson(message, OutboxEvent.class);
            orderSagaService.processPaymentResult(incomingEvent);
        } catch (Exception e) {
            // should add DLQ mechanism
            log.error("[Consumer] Failed to process event from 'payments' topic. Key: {}. Error: {}",
                    key, e.getMessage(), e);
        }
    }
}
