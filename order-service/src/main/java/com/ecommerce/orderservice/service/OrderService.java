package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.OrderRequestItem;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.ProductDto;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.OrderNotCancelableException;
import com.ecommerce.orderservice.mapper.OrderMapper;
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
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderMapper.toDtoList(orderRepository.findAll());
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(String id) {
        return orderMapper.toDto(getEntity(id));
    }

    @Transactional
    public OrderResponse createOrder(String customerId, List<OrderRequestItem> items) {
        Order order = Order.builder()
                .customerId(customerId)
                .status(Order.OrderStatus.PENDING)
                .build();

        // Validar stock y construir items
        for (OrderRequestItem requestItem : items) {
            // Llamada síncrona a Product Service via Feign
            ProductDto product = productClient.getProduct(requestItem.productId()).data();

            if (product.stockQuantity() == null || product.stockQuantity() < requestItem.quantity()) {
                throw new InsufficientStockException(
                        String.format("Insufficient stock for product %s. Available: %s, Requested: %d",
                                requestItem.productId(),
                                product.stockQuantity() == null ? 0 : product.stockQuantity(),
                                requestItem.quantity())
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
        return orderMapper.toDto(orderRepository.save(savedOrder));
    }

    @Transactional
    public OrderResponse updateStatus(String id, Order.OrderStatus newStatus) {
        Order order = getEntity(id);
        order.setStatus(newStatus);
        return orderMapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public void cancelOrder(String id) {
        Order order = getEntity(id);
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new OrderNotCancelableException("Cannot cancel delivered order");
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} cancelled", id);
    }

    private Order getEntity(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
    }
}
