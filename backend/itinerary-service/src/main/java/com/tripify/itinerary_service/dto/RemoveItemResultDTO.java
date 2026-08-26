package com.tripify.itinerary_service.dto;

import java.util.List;

// alsoRemoved: titoli dei componenti troncati insieme a quello richiesto perché
// non erano più coerenti senza di esso (es. hotel rimasto senza il volo di andata).
public record RemoveItemResultDTO(List<String> alsoRemoved) {}
