package com.ecommerce.order.exception;

import org.springframework.security.access.AccessDeniedException;

public class OrderAccessDeniedException extends AccessDeniedException {

    public OrderAccessDeniedException(String message) {
        super(message);
    }
}
