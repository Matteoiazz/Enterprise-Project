package com.tripify.catalog_service.service.impl;

import com.tripify.catalog_service.entity.Itinerary;
import com.tripify.catalog_service.entity.Visibility;
import com.tripify.catalog_service.repository.CatalogItemRepository;
import com.tripify.catalog_service.repository.ItineraryRepository;
import com.tripify.catalog_service.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private final ItineraryRepository itineraryRepository;
    // Aggiungiamo il repository del catalogo per fare controlli incrociati
    private final CatalogItemRepository catalogItemRepository;

    @Override
    @Transactional
    public Itinerary createCommercialPackage(Itinerary itinerary, Long hostId) {
        itinerary.setCreatorId(hostId);
        itinerary.setCommercialPackage(true);
        itinerary.setVisibility(Visibility.PUBLIC);

        if (itinerary.getCatalogItemIds() == null) {
            itinerary.setCatalogItemIds(new ArrayList<>());
        }
        return itineraryRepository.save(itinerary);
    }

    @Override
    public List<Itinerary> getAllCommercialPackages() {
        return itineraryRepository.findByIsCommercialPackageTrue();
    }

    @Override
    @Transactional
    public Itinerary createFavoriteList(String title, Long travelerId, boolean isPrivate) {
        // Controllo validità titolo
        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il titolo della lista non può essere vuoto");
        }

        Itinerary list = new Itinerary();
        list.setTitle(title);
        list.setCreatorId(travelerId);
        list.setCommercialPackage(false);
        list.setVisibility(isPrivate ? Visibility.PRIVATE : Visibility.SHARED);
        list.setCatalogItemIds(new ArrayList<>());

        return itineraryRepository.save(list);
    }

    @Override
    @Transactional
    public Itinerary addItemToList(Long itineraryId, Long catalogItemId, Long travelerId) {
        Itinerary itinerary = getItineraryById(itineraryId); // Riutilizziamo il metodo sotto!

        if (!itinerary.getCreatorId().equals(travelerId)) {
            // Sostituito RuntimeException con ResponseStatusException
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non sei autorizzato a modificare questa lista");
        }

        // CONTROLLO FONDAMENTALE: L'item esiste davvero nel database?
        if (!catalogItemRepository.existsById(catalogItemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'elemento del catalogo che cerchi di aggiungere non esiste");
        }

        // EVITIAMO DUPLICATI: Aggiungiamo solo se non c'è già
        if (!itinerary.getCatalogItemIds().contains(catalogItemId)) {
            itinerary.getCatalogItemIds().add(catalogItemId);
        }

        return itineraryRepository.save(itinerary);
    }

    @Override
    public List<Itinerary> getMyLists(Long travelerId) {
        return itineraryRepository.findByCreatorId(travelerId);
    }

    @Override
    public Itinerary getItineraryById(Long id) {
        // Srotoliamo l'Optional direttamente nel Service
        return itineraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerario non trovato con ID: " + id));
    }

    @Override
    @Transactional
    public void deleteItinerary(Long id, Long requesterId) {
        Itinerary itinerary = getItineraryById(id);

        if (!itinerary.getCreatorId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non sei autorizzato a eliminare questo itinerario");
        }

        itineraryRepository.delete(itinerary);
    }
}