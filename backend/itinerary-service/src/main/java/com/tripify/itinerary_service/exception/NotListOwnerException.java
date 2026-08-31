package com.tripify.itinerary_service.exception;

public class NotListOwnerException extends RuntimeException {
    public NotListOwnerException() {
        super("Solo il proprietario della lista può eseguire questa operazione");
    }

    public NotListOwnerException(String message) {
        super(message);
    }
}
