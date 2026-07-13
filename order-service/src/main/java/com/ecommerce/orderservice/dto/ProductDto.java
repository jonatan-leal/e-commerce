package com.ecommerce.orderservice.dto;

public record ProductDto(
        String id,
        String name,
        String description,
        java.math.BigDecimal price,
        Integer stockQuantity,
        String category,
        String imageUrl
) {
}
