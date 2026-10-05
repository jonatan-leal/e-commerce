package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ApiResponse;
import com.ecommerce.orderservice.dto.ProductDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public ApiResponse<ProductDto> getProduct(String id) {
        // Retorna un producto "dummy" o null para indicar fallo
        return ApiResponse.of("Product service is down", new ProductDto(
                id,
                "UNKNOWN (Service Unavailable)",
                "Product service is down",
                BigDecimal.ZERO,
                0,
                "UNKNOWN",
                null
        ));
    }

    @Override
    public ApiResponse<ProductDto> decrementStock(String id, Map<String, Integer> stockUpdate) {
        // No se puede actualizar stock si el servicio está caído
        return ApiResponse.of("Product service is down", null);
    }
}
