package com.tripify.catalog_service.mapper;

import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.entity.CatalogImage;
import com.tripify.catalog_service.entity.CatalogItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CatalogMapper {

    public CatalogItemDTO toDto(CatalogItem item) {

        List<String> extractedUrls = item.getImages() != null ?
                item.getImages().stream()
                        .map(CatalogImage::getImageUrl)
                        .collect(Collectors.toList())
                : List.of();

        var builder = CatalogItemDTO.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .price(item.getPrice())
                .currency(item.getCurrency())
                .category(item.getCategory())
                .rating(item.getRating())
                .imageUrls(extractedUrls)
                .itemType(item.getClass().getSimpleName());

        if (item instanceof com.tripify.catalog_service.entity.Flight flight) {
            builder.departureAirport(flight.getDepartureAirport())
                    .arrivalAirport(flight.getArrivalAirport())
                    .departureCity(flight.getDepartureCity())
                    .arrivalCity(flight.getArrivalCity())
                    .departureTime(flight.getDepartureTime())
                    .arrivalTime(flight.getArrivalTime())
                    .availableSeats(flight.getAvailableSeats())
                    .stops(flight.getStops());
        } else if (item instanceof com.tripify.catalog_service.entity.Hotel hotel) {
            builder.roomType(hotel.getRoomType())
                    .availableRooms(hotel.getAvailableRooms())
                    .locationLat(hotel.getLocationLat())
                    .locationLng(hotel.getLocationLng())
                    .address(hotel.getAddress())
                    .city(hotel.getCity())
                    .amenities(hotel.getAmenities());
        } else if (item instanceof com.tripify.catalog_service.entity.Activity activity) {
            builder.activityType(activity.getActivityType())
                    .duration(activity.getDuration())
                    .meetingPoint(activity.getMeetingPoint())
                    .city(activity.getCity())
                    .maxParticipants(activity.getMaxParticipants())
                    .guideIncluded(activity.isGuideIncluded());
        }

        return builder.build();
    }
}