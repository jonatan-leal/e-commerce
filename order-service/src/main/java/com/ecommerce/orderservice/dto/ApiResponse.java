package com.ecommerce.orderservice.dto;

// Duplicado por diseño de ApiResponse en product-service: los servicios son
// independientes, nada obliga a que los dos records coincidan.
public record ApiResponse<T>(
        String message,
        T data) {

    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(message, data);
    }
}
