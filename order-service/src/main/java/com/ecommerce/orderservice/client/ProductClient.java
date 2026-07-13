package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// name = nombre del servicio en Eureka
// fallback = clase de respaldo si falla
@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductDto getProduct(@PathVariable String id);

    @PatchMapping("/products/{id}/stock/decrement")
    ProductDto decrementStock(@PathVariable String id, @RequestBody Map<String, Integer> stockUpdate);
}
