package com.ecommerce.productservice.exception;

/**
 * Regla de negocio: no hay stock suficiente para cubrir la cantidad pedida.
 * Se mapea a 409 Conflict, no a 500: el error es del cliente, no del servidor.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}