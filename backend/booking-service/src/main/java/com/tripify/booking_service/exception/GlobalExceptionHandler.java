package com.tripify.booking_service.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 404: la risorsa richiesta (es. una Booking con un certo id) non esiste
    @ExceptionHandler({ResourceNotFoundException.class, CatalogItemNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex) {
        return buildError(ex.getMessage(), HttpStatus.NOT_FOUND, "Risorsa non trovata");
    }

    // 403: l'utente è identificato ma non ha il permesso per l'operazione richiesta
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccessDeniedException ex) {
        return buildError(ex.getMessage(), HttpStatus.FORBIDDEN, "Accesso negato");
    }

    // 400: input del client non valido/incompleto
    @ExceptionHandler({EmptyCartException.class, PaymentValidationException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST, "Richiesta non valida");
    }

    // 409: l'operazione richiesta non è compatibile con lo stato attuale della risorsa
    // (es. pagare una prenotazione già confermata, invitare due volte lo stesso amico)
    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(InvalidBookingStateException ex) {
        return buildError(ex.getMessage(), HttpStatus.CONFLICT, "Stato non valido");
    }

    // 400: uno o più campi del body non rispettano le regole di validazione (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildError(message.isBlank() ? "Dati non validi" : message, HttpStatus.BAD_REQUEST, "Dati non validi");
    }

    // Propaga (con lo stesso status HTTP quando possibile) gli errori restituiti
    // dagli altri microservizi chiamati via Feign (es. catalog-service: hold
    // scaduto -> 409, disponibilità insufficiente -> 409, articolo non trovato -> 404),
    // invece di far risalire un 500 generico che nasconderebbe la causa reale.
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex) {
        // Logghiamo sempre chi era il destinatario della chiamata (ex.request()) e
        // il body della risposta d'errore: senza, un fallimento verso un altro
        // microservizio non lascia alcuna traccia utile per capire quale servizio
        // è irraggiungibile o cosa ha risposto.
        log.warn("Chiamata verso un altro microservizio fallita: {} {} -> HTTP {} - {}",
                ex.request() != null ? ex.request().httpMethod() : "?",
                ex.request() != null ? ex.request().url() : "?",
                ex.status(), ex.contentUTF8());
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null || status == HttpStatus.INTERNAL_SERVER_ERROR) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return buildError("Errore comunicando con un servizio esterno.", status, "Errore di integrazione");
    }

    // Fallback finale: qualunque altra eccezione non prevista è un vero errore
    // interno (bug, NPE, ecc.), non un 400 "colpa del client" come accadeva
    // prima intercettando genericamente RuntimeException.
    // Loggata per intero: un @ExceptionHandler che la cattura impedisce a Spring
    // di stampare lo stack trace di default, quindi senza questo log un 500
    // non lascia alcuna traccia in console.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Errore interno non gestito", ex);
        return buildError("Si è verificato un errore imprevisto.", HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno");
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
