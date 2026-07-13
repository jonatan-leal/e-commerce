package com.ecommerce.gatewayservice.dto;

public record FallbackResponseDto(
        String error,
        String message,
        String timestamp
) {
}
