package com.tripify.booking_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404: la risorsa richiesta (es. una Booking con un certo id) non esiste
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildError(ex.getMessage(), HttpStatus.NOT_FOUND, "Risorsa non trovata");
    }

    // 403: l'utente è identificato ma non ha il permesso per l'operazione richiesta
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccessDeniedException ex) {
        return buildError(ex.getMessage(), HttpStatus.FORBIDDEN, "Accesso negato");
    }

    // Fallback: qualunque altra RuntimeException (es. carrello vuoto) resta un 400,
    // esattamente come facevi già prima con l'handler unico.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeExceptions(RuntimeException ex) {
        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST, "Richiesta non valida");
    }

    private ResponseEntity<Map<String, Object>> buildError(String message, HttpStatus status, String errorLabel) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", LocalDateTime.now());
        errorBody.put("status", status.value());
        errorBody.put("error", errorLabel);
        errorBody.put("message", message);

        return new ResponseEntity<>(errorBody, status);
    }
}