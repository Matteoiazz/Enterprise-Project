package com.tripify.catalog_service.service.impl;

import com.tripify.catalog_service.entity.Itinerary;
import com.tripify.catalog_service.entity.Visibility;
import com.tripify.catalog_service.repository.ItineraryRepository;
import com.tripify.catalog_service.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private final ItineraryRepository itineraryRepository;

    @Override
    @Transactional
    public Itinerary createCommercialPackage(Itinerary itinerary, Long hostId) {
        // Forza i flag corretti per un pacchetto commerciale
        itinerary.setCreatorId(hostId);
        itinerary.setCommercialPackage(true);
        itinerary.setVisibility(Visibility.PUBLIC); // I pacchetti venduti sono sempre pubblici

        // Assicurati che la lista di ID non sia nulla
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
        Itinerary list = new Itinerary();
        list.setTitle(title);
        list.setCreatorId(travelerId);
        list.setCommercialPackage(false); // È una lista utente, non in vendita
        list.setVisibility(isPrivate ? Visibility.PRIVATE : Visibility.SHARED);
        list.setCatalogItemIds(new ArrayList<>());

        return itineraryRepository.save(list);
    }

    @Override
    @Transactional
    public Itinerary addItemToList(Long itineraryId, Long catalogItemId, Long travelerId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerario non trovato"));

        // Controllo di sicurezza: solo il creatore può modificare la sua lista
        if (!itinerary.getCreatorId().equals(travelerId)) {
            throw new RuntimeException("Non sei autorizzato a modificare questa lista");
        }

        itinerary.getCatalogItemIds().add(catalogItemId);
        return itineraryRepository.save(itinerary);
    }

    @Override
    public List<Itinerary> getMyLists(Long travelerId) {
        return itineraryRepository.findByCreatorId(travelerId);
    }

    @Override
    public Optional<Itinerary> getItineraryById(Long id) {
        return itineraryRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteItinerary(Long id, Long requesterId) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Itinerario non trovato"));

        if (!itinerary.getCreatorId().equals(requesterId)) {
            throw new RuntimeException("Non sei autorizzato a eliminare questo itinerario");
        }

        itineraryRepository.delete(itinerary);
    }
}