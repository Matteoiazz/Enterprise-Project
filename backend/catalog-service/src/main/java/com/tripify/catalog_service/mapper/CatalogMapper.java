package com.tripify.catalog_service.mapper;

import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.dto.FareClassDTO;
import com.tripify.catalog_service.dto.RoomTypeDTO;
import com.tripify.catalog_service.entity.CatalogImage;
import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.entity.FareClass;
import com.tripify.catalog_service.entity.RoomType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
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
                .hostId(item.getHostId() != null ? item.getHostId().toString() : null)
                .itemType(item.getClass().getSimpleName());

        if (item instanceof com.tripify.catalog_service.entity.Flight flight) {
            List<FareClassDTO> fareClasses = flight.getFareClasses().stream()
                    .map(this::toDto)
                    .toList();
            builder.departureAirport(flight.getDepartureAirport())
                    .arrivalAirport(flight.getArrivalAirport())
                    .departureCity(flight.getDepartureCity())
                    .arrivalCity(flight.getArrivalCity())
                    .departureTime(flight.getDepartureTime())
                    .arrivalTime(flight.getArrivalTime())
                    .totalSeats(flight.getTotalSeats())
                    .stops(flight.getStops())
                    .fareClasses(fareClasses)
                    .price(cheapestPrice(fareClasses.stream().map(FareClassDTO::getPrice), flight.getPrice()));
        } else if (item instanceof com.tripify.catalog_service.entity.Hotel hotel) {
            List<RoomTypeDTO> roomTypes = hotel.getRoomTypes().stream()
                    .map(this::toDto)
                    .toList();
            builder.locationLat(hotel.getLocationLat())
                    .locationLng(hotel.getLocationLng())
                    .address(hotel.getAddress())
                    .city(hotel.getCity())
                    .amenities(hotel.getAmenities())
                    .roomTypes(roomTypes)
                    .price(cheapestPrice(roomTypes.stream().map(RoomTypeDTO::getPrice), hotel.getPrice()));
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

    private RoomTypeDTO toDto(RoomType roomType) {
        return RoomTypeDTO.builder()
                .id(roomType.getId())
                .name(roomType.getName())
                .description(roomType.getDescription())
                .price(roomType.getPrice())
                .totalRooms(roomType.getTotalRooms())
                .maxOccupancy(roomType.getMaxOccupancy())
                .benefits(roomType.getBenefits())
                .imageUrls(roomType.getImageUrls())
                .build();
    }

    private FareClassDTO toDto(FareClass fareClass) {
        return FareClassDTO.builder()
                .id(fareClass.getId())
                .name(fareClass.getName())
                .price(fareClass.getPrice())
                .totalSeats(fareClass.getTotalSeats())
                .build();
    }

    private BigDecimal cheapestPrice(java.util.stream.Stream<BigDecimal> prices, BigDecimal fallback) {
        return prices.filter(java.util.Objects::nonNull).min(Comparator.naturalOrder()).orElse(fallback);
    }
}