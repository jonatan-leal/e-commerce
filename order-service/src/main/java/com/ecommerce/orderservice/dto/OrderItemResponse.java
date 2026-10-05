package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String id,
        String productId,
        String productName,
        Integer quantity,
        BigDecimal price) {
}
