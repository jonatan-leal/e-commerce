package com.ecommerce.productservice.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.NOT_FOUND.value());
        error.put(ERROR, "Not Found");
        error.put(MESSAGE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            validationErrors.put(fieldName, message);
        });

        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.BAD_REQUEST.value());
        error.put(ERROR, validationErrors);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put(ERROR, "Internal Server Error");
        error.put(MESSAGE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}