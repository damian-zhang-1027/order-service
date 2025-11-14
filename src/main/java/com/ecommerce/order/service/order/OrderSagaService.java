package com.ecommerce.order.service.order;

import com.ecommerce.order.model.db.entity.OutboxEvent;

public interface OrderSagaService {

    void processPaymentResult(OutboxEvent incomingEvent);
}
