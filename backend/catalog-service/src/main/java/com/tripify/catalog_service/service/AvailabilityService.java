package com.tripify.catalog_service.service;

import com.tripify.catalog_service.dto.HoldResultDTO;

import java.time.LocalDate;

public interface AvailabilityService {

    HoldResultDTO holdRoom(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms, String userId);

    HoldResultDTO holdSeats(Long fareClassId, int seats, String userId);

    void confirm(String holdId, String userId);

    void release(String holdId, String userId);

    /**
     * Come release, ma forza indietro anche un hold già CONFIRMED: serve a booking-service
     * per compensare un hold confermato quando il resto della transazione (altri hold, il
     * pagamento) fallisce dopo. Non passa da nextStatusOnRelease apposta: quel controllo
     * esiste per bloccare un utente che prova a rilasciare da solo un hold già confermato,
     * non per questo caso. Nessun controllo di proprietario: non la chiama l'utente finale
     * (vedi la chiave di servizio in AvailabilityController.compensate).
     */
    void compensate(String holdId);

    int computeRoomAvailability(Long roomTypeId, LocalDate checkIn, LocalDate checkOut);

    int computeSeatAvailability(Long fareClassId);
}
