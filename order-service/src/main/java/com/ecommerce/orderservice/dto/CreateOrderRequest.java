package com.ecommerce.orderservice.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotEmpty List<OrderRequestItem> items) {

}
