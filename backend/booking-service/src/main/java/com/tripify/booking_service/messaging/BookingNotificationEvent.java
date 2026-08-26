package com.tripify.booking_service.messaging;

// Rispecchia nella forma NotificationEvent di communication-service, ma NON è
// ancora compatibile con essa: lì userId è tipizzato Long, mentre nell'intero
// sistema (Keycloak, User.id, claim JWT "sub") l'identità utente è sempre una
// UUID stringa. Non esiste da nessuna parte un id numerico utente con cui
// popolare correttamente un Long, quindi questo evento non va pubblicato sulla
// coda "notification_queue" finché communication-service non allinea il tipo:
// pubblicarlo così com'è farebbe fallire la deserializzazione lato consumer
// (o, peggio, potrebbe recapitare la notifica alla persona sbagliata).
// Vedi BookingEventPublisher per il punto di collegamento predisposto ma non attivo.
public record BookingNotificationEvent(
        String userId,
        String title,
        String message
) {}
