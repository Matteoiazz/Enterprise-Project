package com.tripify.itinerary_service.exception;

/**
 * Solo per liste PUBLIC: l'esistenza è già nota (visibile via GET/feed), quindi
 * negare una modifica a chi non è proprietario può dirlo esplicitamente (403)
 * invece del 404 anti-enumerazione usato per le liste PRIVATE/SHARED (vedi
 * ItineraryService.getOwnedList/getEditableList).
 */
public class NotListOwnerException extends RuntimeException {
    public NotListOwnerException(Long listId) {
        super("Non sei il proprietario della lista: " + listId);
    }
}
