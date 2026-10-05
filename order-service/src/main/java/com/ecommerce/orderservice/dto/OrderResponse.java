package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
                String id,
                String customerId,
                List<OrderItemResponse> items,
                BigDecimal totalAmount,
                String status,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}
