package com.tripify.booking_service.dto;

// Vista pubblica di un Passenger, compreso il suo "biglietto": qrCodeData resta
// null finché CheckInService non apre il check-in per quella riga (24h prima
// per gli hotel, subito dopo la conferma per voli/attività senza una data di
// check-in propria) - l'app mostra "check-in non ancora aperto" finché è null.
public record PassengerResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String documentType,
        String documentNumber,
        String qrCodeData,
        boolean checkedIn
) {}
