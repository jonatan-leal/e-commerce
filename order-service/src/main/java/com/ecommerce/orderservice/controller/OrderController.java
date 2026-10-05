package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.ApiResponse;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.of("Orders retrieved", orderService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.of("Order retrieved", orderService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        OrderResponse order = orderService.createOrder(request.customerId(), request.items());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("Order created", order));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> statusUpdate) {
        Order.OrderStatus status = Order.OrderStatus.valueOf(statusUpdate.get("status"));
        return ResponseEntity.ok(ApiResponse.of("Order status updated", orderService.updateStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    // Health check endpoint personalizado
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.of("Service is up", Map.of(
                "service", "order-service",
                "status", "UP",
                "timestamp", java.time.Instant.now().toString())));
    }
}
