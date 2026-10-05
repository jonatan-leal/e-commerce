package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.ErrorResponse;
import com.ecommerce.productservice.exception.InsufficientStockException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String NOT_FOUND_MESSAGE = "Not Found";
    private static final String BAD_REQUEST_MESSAGE = "Bad Request";
    private static final String CONFLICT_MESSAGE = "Conflict";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal Server Error";
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";
    private static final String TIMEZONE = "UTC";

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(ZoneId.of(TIMEZONE)),
                HttpStatus.NOT_FOUND.value(),
                NOT_FOUND_MESSAGE,
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // No todo error de binding es de campo: un error global (ObjectError)
            // haría ClassCastException y devolvería 500 en lugar de 400.
            String key = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();
            validationErrors.put(key, error.getDefaultMessage());
        });

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(ZoneId.of(TIMEZONE)),
                HttpStatus.BAD_REQUEST.value(),
                validationErrors,
                ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(ZoneId.of(TIMEZONE)),
                HttpStatus.BAD_REQUEST.value(),
                BAD_REQUEST_MESSAGE,
                ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(ZoneId.of(TIMEZONE)),
                HttpStatus.CONFLICT.value(),
                CONFLICT_MESSAGE,
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Se registra la causa completa y no se filtra ex.getMessage() al cliente:
        // puede exponer SQL, nombres de clase o URLs internas.
        log.error("Unhandled exception", ex);

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(ZoneId.of(TIMEZONE)),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                INTERNAL_SERVER_ERROR_MESSAGE,
                UNEXPECTED_ERROR_MESSAGE
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}