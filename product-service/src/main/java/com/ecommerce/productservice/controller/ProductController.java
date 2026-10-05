package com.ecommerce.productservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.productservice.dto.ApiResponse;
import com.ecommerce.productservice.dto.ProductDto;
import com.ecommerce.productservice.service.ProductService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.of("Products retrieved", productService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.of("Product retrieved", productService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(@RequestBody @Valid ProductDto product) {
        ProductDto created = productService.create(product);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Product created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable String id,
            @RequestBody @Valid ProductDto product) {
        return ResponseEntity.ok(ApiResponse.of("Product updated", productService.update(id, product)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.of("Products retrieved", productService.findByCategory(category)));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductDto>> updateStock(
            @PathVariable String id,
            @RequestBody Map<String, Integer> stockUpdate) {
        Integer quantity = stockUpdate.get("quantity");
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        }
        return ResponseEntity.ok(ApiResponse.of("Stock updated", productService.updateStock(id, quantity)));
    }

    @PatchMapping("/{id}/stock/decrement")
    public ResponseEntity<ApiResponse<ProductDto>> decrementStock(
            @PathVariable String id,
            @RequestBody Map<String, Integer> request) {
        Integer amount = request.get("amount");
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return ResponseEntity.ok(ApiResponse.of("Stock decremented", productService.decrementStock(id, amount)));
    }

    // Health check endpoint personalizado
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.of("Service is up", Map.of(
                "service", "product-service",
                "status", "UP",
                "timestamp", java.time.Instant.now().toString())));
    }
}
