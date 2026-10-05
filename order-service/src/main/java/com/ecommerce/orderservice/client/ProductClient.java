package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ApiResponse;
import com.ecommerce.orderservice.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// name = nombre del servicio en Eureka
// fallback = clase de respaldo si falla
// Los endpoints de product-service responden ApiResponse<ProductDto>, por eso el
// cliente devuelve el envoltorio y el servicio usa .data().
@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("api/v1/products/{id}")
    ApiResponse<ProductDto> getProduct(@PathVariable String id);

    @PatchMapping("api/v1/products/{id}/stock/decrement")
    ApiResponse<ProductDto> decrementStock(@PathVariable String id, @RequestBody Map<String, Integer> stockUpdate);
}
