package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDto(String id,
                         @NotBlank(message = "Name is required")
                         String name,
                         @Size(max = 2000, message = "Description must be at most 2000 characters")
                         String description,
                         @NotNull(message = "Price is required")
                         @Positive(message = "Price must be positive")
                         BigDecimal price,
                         @PositiveOrZero(message = "Stock quantity must be non-negative")
                         Integer stockQuantity,
                         String category,
                         String imageUrl,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt) {
}
