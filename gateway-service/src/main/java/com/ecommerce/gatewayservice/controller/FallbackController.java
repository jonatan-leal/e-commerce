package com.ecommerce.gatewayservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/product")
    public Mono<Map<String, Object>> productFallback() {
        return Mono.just(Map.of(
                "error", "Service Unavailable",
                "message", "Product Service is temporarily unavailable. Please try again later.",
                "timestamp", java.time.Instant.now().toString()
        ));
    }

    @GetMapping("/order")
    public Mono<Map<String, Object>> orderFallback() {
        return Mono.just(Map.of(
                "error", "Service Unavailable",
                "message", "Order Service is temporarily unavailable. Please try again later.",
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}
