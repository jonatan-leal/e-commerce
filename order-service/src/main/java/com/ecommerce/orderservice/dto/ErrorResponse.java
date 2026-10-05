package com.ecommerce.orderservice.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        Integer status,
        Object error,
        String message) {
}
