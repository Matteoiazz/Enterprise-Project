package com.tripify.booking_service.dto;

import java.util.List;

// cartItemIds nullo o vuoto = checkout dell'intero carrello (comportamento
// storico); valorizzato = checkout solo di quegli articoli, lasciando gli
// altri nel carrello (vedi BookingService.checkout).
public record CheckoutRequestDTO(List<Long> cartItemIds) {}
