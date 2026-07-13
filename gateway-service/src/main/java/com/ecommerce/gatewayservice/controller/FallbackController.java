package com.ecommerce.gatewayservice.controller;

import com.ecommerce.gatewayservice.dto.FallbackResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final String SERVICE_UNAVAILABLE_ERROR = "Service Unavailable";
    private static final String SERVICE_UNAVAILABLE_MESSAGE_TEMPLATE = "%s Service is temporarily unavailable. Please try again later.";

    @GetMapping("/product")
    public Mono<FallbackResponseDto> productFallback() {
        return createFallbackResponse("Product");
    }

    @GetMapping("/order")
    public Mono<FallbackResponseDto> orderFallback() {
        return createFallbackResponse("Order");
    }

    private Mono<FallbackResponseDto> createFallbackResponse(String serviceName) {
        return Mono.just(new FallbackResponseDto(
                SERVICE_UNAVAILABLE_ERROR,
                SERVICE_UNAVAILABLE_MESSAGE_TEMPLATE.formatted(serviceName),
                Instant.now().toString()
        ));
    }
}
