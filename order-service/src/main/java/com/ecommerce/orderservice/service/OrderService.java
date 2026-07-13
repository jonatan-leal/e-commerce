package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.ProductDto;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;  // Feign Client

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order findById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
    }

    @Transactional
    public Order createOrder(String customerId, List<OrderRequestItem> items) {
        Order order = Order.builder()
                .customerId(customerId)
                .status(Order.OrderStatus.PENDING)
                .build();

        // Validar stock y construir items
        for (OrderRequestItem requestItem : items) {
            // Llamada síncrona a Product Service via Feign
            ProductDto product = productClient.getProduct(requestItem.productId());
            IO.println(product);

            if (product.stockQuantity() == null || product.stockQuantity() < requestItem.quantity()) {
                throw new IllegalStateException(
                        String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                                requestItem.productId(), product.stockQuantity(), requestItem.quantity())
                );
            }

            OrderItem orderItem = OrderItem.builder()
                    .productId(requestItem.productId())
                    .productName(product.name())
                    .quantity(requestItem.quantity())
                    .price(product.price())
                    .build();

            order.addItem(orderItem);
        }

        order.calculateTotal();

        // Guardar orden
        Order savedOrder = orderRepository.save(order);

        // Actualizar stock en Product Service (llamada síncrona)
        for (OrderItem item : savedOrder.getItems()) {
            productClient.decrementStock(
                    item.getProductId(),
                    Map.of("amount", item.getQuantity())
            );
        }

        // Cambiar estado a CONFIRMED
        savedOrder.setStatus(Order.OrderStatus.CONFIRMED);

        log.info("Order {} created successfully for customer {}", savedOrder.getId(), customerId);
        return orderRepository.save(savedOrder);
    }

    @Transactional
    public Order updateStatus(String id, Order.OrderStatus newStatus) {
        Order order = findById(id);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(String id) {
        Order order = findById(id);
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel delivered order");
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} cancelled", id);
    }

    // Record para recibir items del controller
    public record OrderRequestItem(String productId, Integer quantity) {
    }
}
