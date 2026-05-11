package com.tripify.catalog_service.service;

import com.tripify.catalog_service.entity.Itinerary;
import java.util.List;
import java.util.Optional;

public interface ItineraryService {

    // --- FUNZIONALITÀ PER L'ORGANIZZATORE (HOST) ---

    // Crea un nuovo pacchetto in vendita (es. Volo + Hotel)
    Itinerary createCommercialPackage(Itinerary itinerary, Long hostId);

    // Mostra tutti i pacchetti commerciali disponibili sul catalogo pubblico
    List<Itinerary> getAllCommercialPackages();


    // --- FUNZIONALITÀ PER IL VIAGGIATORE (TRAVELER) ---

    // Crea una nuova lista preferiti vuota
    Itinerary createFavoriteList(String title, Long travelerId, boolean isPrivate);

    // Aggiunge un oggetto (es. un volo) alla lista
    Itinerary addItemToList(Long itineraryId, Long catalogItemId, Long travelerId);

    // Mostra le liste personali di un utente
    List<Itinerary> getMyLists(Long travelerId);


    // --- FUNZIONALITÀ COMUNI ---

    // Ottiene il dettaglio di un itinerario (serve per il Booking Service)
    Optional<Itinerary> getItineraryById(Long id);

    // Elimina un itinerario/pacchetto
    void deleteItinerary(Long id, Long requesterId);
}