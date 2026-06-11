package com.tripify.catalog_service.service.impl;

import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.repository.CatalogItemRepository;
import com.tripify.catalog_service.repository.spec.CatalogItemSpecification;
import com.tripify.catalog_service.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CatalogItemRepository catalogItemRepository;

    @Override
    public List<CatalogItem> getAllItems() {
        return catalogItemRepository.findAll();
    }

    // IL NUOVO SUPER-MOTORE DI RICERCA
    @Override
    public List<CatalogItemDTO> search(String category, String query, BigDecimal maxPrice, Integer minRating) {

        // 1. Costruiamo la query dinamica tramite la Specification
        Specification<CatalogItem> spec = CatalogItemSpecification.withDynamicFilters(category, query, maxPrice, minRating);

        // 2. Eseguiamo la query sul database
        List<CatalogItem> items = catalogItemRepository.findAll(spec);

        // 3. Traduciamo la lista di entità in una lista di DTO per il Controller
        return items.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // AGGIORNATO A UUID
    @Override
    public List<CatalogItem> getItemsByHost(UUID hostId) {
        return catalogItemRepository.findByHostId(hostId);
    }

    @Override
    public CatalogItem saveItem(CatalogItem item) {
        return catalogItemRepository.save(item);
    }

    // Il traduttore: converte l'entità grezza nel "pacchetto postale" DTO
    private CatalogItemDTO mapToDTO(CatalogItem item) {

        // 1. Costruiamo la base con i campi comuni
        var builder = CatalogItemDTO.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .price(item.getPrice())
                .currency(item.getCurrency())
                .category(item.getCategory())
                .rating(item.getRating())
                .itemType(item.getClass().getSimpleName());

        // 2. MAGIA DEL POLIMORFISMO: Se è un Volo, estraiamo i dati dalla tabella dei voli
        if (item instanceof com.tripify.catalog_service.entity.Flight flight) {
            builder.departureAirport(flight.getDepartureAirport())
                    .arrivalAirport(flight.getArrivalAirport())
                    .departureTime(flight.getDepartureTime())
                    .arrivalTime(flight.getArrivalTime())
                    .availableSeats(flight.getAvailableSeats());
        }
        // 3. Se è un Hotel, estraiamo i dati dalla tabella degli hotel
        else if (item instanceof com.tripify.catalog_service.entity.Hotel hotel) {
            builder.roomType(hotel.getRoomType())
                    .availableRooms(hotel.getAvailableRooms())
                    .locationLat(hotel.getLocationLat())
                    .locationLng(hotel.getLocationLng());
        }

        // 4. Chiudiamo il pacchetto e lo spediamo!
        return builder.build();
    }
}