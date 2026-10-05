package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.dto.OrderItemResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

// Mapeo escrito a mano y no con MapStruct: con Lombok + MapStruct + JDK 25 el
// .class generado pierde "implements OrderMapper" y Spring no encuentra el bean.
@Component
public class OrderMapper {

    public OrderResponse toDto(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getItems().stream().map(this::toDtoItem).toList(),
                order.getTotalAmount(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public List<OrderResponse> toDtoList(List<Order> orders) {
        if (orders == null) {
            return null;
        }
        return orders.stream().map(this::toDto).toList();
    }

    private OrderItemResponse toDtoItem(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}
