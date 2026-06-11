package com.tripify.catalog_service.mapper;

import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.entity.CatalogImage;
import com.tripify.catalog_service.entity.CatalogItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component // Lo rende iniettabile in tutta l'app
public class CatalogMapper {

    public CatalogItemDTO toDto(CatalogItem item) {

        // 1. IL FIX: Estraiamo solo gli URL dalla lista di oggetti CatalogImage
        List<String> extractedUrls = item.getImages() != null ?
                item.getImages().stream()
                        .map(CatalogImage::getImageUrl)
                        .collect(Collectors.toList())
                : List.of();

        // 2. Costruiamo la base
        var builder = CatalogItemDTO.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .price(item.getPrice())
                .currency(item.getCurrency())
                .category(item.getCategory())
                .rating(item.getRating())
                .imageUrls(extractedUrls) // <-- ECCO I TUOI DATI!
                .itemType(item.getClass().getSimpleName());

        // 3. Polimorfismo
        if (item instanceof com.tripify.catalog_service.entity.Flight flight) {
            builder.departureAirport(flight.getDepartureAirport())
                    .arrivalAirport(flight.getArrivalAirport())
                    .departureTime(flight.getDepartureTime())
                    .arrivalTime(flight.getArrivalTime())
                    .availableSeats(flight.getAvailableSeats());
        } else if (item instanceof com.tripify.catalog_service.entity.Hotel hotel) {
            builder.roomType(hotel.getRoomType())
                    .availableRooms(hotel.getAvailableRooms())
                    .locationLat(hotel.getLocationLat())
                    .locationLng(hotel.getLocationLng());
        }

        return builder.build();
    }
}