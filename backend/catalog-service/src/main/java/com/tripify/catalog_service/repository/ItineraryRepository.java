package com.tripify.catalog_service.repository;

import com.tripify.catalog_service.entity.Itinerary;
import com.tripify.catalog_service.entity.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    // Ottiene TUTTI i pacchetti commerciali in vendita (per il catalogo pubblico)
    List<Itinerary> findByIsCommercialPackageTrue();

    // Ottiene tutte le liste/pacchetti creati da un utente specifico (per la sua area personale)
    List<Itinerary> findByCreatorId(Long creatorId);

    // Ottiene gli itinerari in base al livello di privacy
    List<Itinerary> findByVisibility(Visibility visibility);
}