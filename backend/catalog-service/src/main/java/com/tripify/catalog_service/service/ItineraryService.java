package com.tripify.catalog_service.service;

import com.tripify.catalog_service.entity.Itinerary;
import java.util.List;

public interface ItineraryService {

    // --- FUNZIONALITÀ PER L'ORGANIZZATORE (HOST) ---
    Itinerary createCommercialPackage(Itinerary itinerary, Long hostId);
    List<Itinerary> getAllCommercialPackages();

    // --- FUNZIONALITÀ PER IL VIAGGIATORE (TRAVELER) ---
    Itinerary createFavoriteList(String title, Long travelerId, boolean isPrivate);
    Itinerary addItemToList(Long itineraryId, Long catalogItemId, Long travelerId);
    List<Itinerary> getMyLists(Long travelerId);

    // --- FUNZIONALITÀ COMUNI ---

    // MODIFICA: Restituiamo direttamente l'Itinerary, non un Optional
    Itinerary getItineraryById(Long id);
    void deleteItinerary(Long id, Long requesterId);
}