package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String id,
        String userId,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        String status,
        LocalDateTime orderDate
) {
    public record OrderItemResponse(
            String productId,
            String productName,
            Integer quantity,
            BigDecimal price
    ) {
    }
}
