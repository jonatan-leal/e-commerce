package com.ecommerce.orderservice.exception;

/**
 * Regla de negocio: la orden ya está entregada y no se puede cancelar.
 * Se mapea a 409 Conflict, no a 500.
 */
public class OrderNotCancelableException extends RuntimeException {

    public OrderNotCancelableException(String message) {
        super(message);
    }
}