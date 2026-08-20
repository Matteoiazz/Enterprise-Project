package com.tripify.catalog_service.mapper;

import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.entity.Activity;
import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.entity.Hotel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogMapperTest {

    private final CatalogMapper mapper = new CatalogMapper();

    @Test
    void mappaCorrettamenteUnVolo() {
        Flight flight = new Flight();
        flight.setId(1L);
        flight.setHostId(UUID.randomUUID());
        flight.setTitle("Volo Roma - Milano");
        flight.setDescription("Volo diretto");
        flight.setPrice(new BigDecimal("89.00"));
        flight.setCurrency("EUR");
        flight.setCategory("Voli");
        flight.setRating(4);
        flight.setDepartureAirport("FCO");
        flight.setArrivalAirport("LIN");
        flight.setDepartureCity("Roma");
        flight.setArrivalCity("Milano");
        flight.setDepartureTime(LocalDateTime.of(2026, 9, 5, 7, 30));
        flight.setArrivalTime(LocalDateTime.of(2026, 9, 5, 8, 45));
        flight.setAvailableSeats(60);
        flight.setStops(0);

        CatalogItemDTO dto = mapper.toDto(flight);

        assertThat(dto.getItemType()).isEqualTo("Flight");
        assertThat(dto.getDepartureAirport()).isEqualTo("FCO");
        assertThat(dto.getArrivalAirport()).isEqualTo("LIN");
        assertThat(dto.getDepartureCity()).isEqualTo("Roma");
        assertThat(dto.getArrivalCity()).isEqualTo("Milano");
        assertThat(dto.getStops()).isEqualTo(0);
        assertThat(dto.getRoomType()).isNull();
        assertThat(dto.getActivityType()).isNull();
    }

    @Test
    void mappaCorrettamenteUnHotelConAmenities() {
        Hotel hotel = new Hotel();
        hotel.setId(2L);
        hotel.setHostId(UUID.randomUUID());
        hotel.setTitle("Hotel Hilton Times Square");
        hotel.setPrice(new BigDecimal("250.00"));
        hotel.setCurrency("EUR");
        hotel.setCategory("Hotel");
        hotel.setRating(5);
        hotel.setLocationLat(40.7589);
        hotel.setLocationLng(-73.9851);
        hotel.setRoomType("Double Deluxe");
        hotel.setAvailableRooms(15);
        hotel.setAddress("234 W 42nd St");
        hotel.setCity("New York");
        hotel.setAmenities(List.of("Wi-Fi", "Palestra"));

        CatalogItemDTO dto = mapper.toDto(hotel);

        assertThat(dto.getItemType()).isEqualTo("Hotel");
        assertThat(dto.getCity()).isEqualTo("New York");
        assertThat(dto.getAmenities()).containsExactly("Wi-Fi", "Palestra");
        assertThat(dto.getLocationLat()).isEqualTo(40.7589);
        assertThat(dto.getDepartureAirport()).isNull();
    }

    @Test
    void mappaCorrettamenteUnaAttivitaConGuidaInclusa() {
        Activity activity = new Activity();
        activity.setId(3L);
        activity.setHostId(UUID.randomUUID());
        activity.setTitle("Trekking in Sila");
        activity.setPrice(new BigDecimal("45.00"));
        activity.setCurrency("EUR");
        activity.setCategory("Attività");
        activity.setActivityType("Sport e Natura");
        activity.setDuration("6 ore");
        activity.setMeetingPoint("Centro Visite Cupone");
        activity.setCity("Sila");
        activity.setMaxParticipants(15);
        activity.setGuideIncluded(true);

        CatalogItemDTO dto = mapper.toDto(activity);

        assertThat(dto.getItemType()).isEqualTo("Activity");
        assertThat(dto.getGuideIncluded()).isTrue();
        assertThat(dto.getActivityType()).isEqualTo("Sport e Natura");
        assertThat(dto.getMaxParticipants()).isEqualTo(15);
    }

    @Test
    void gestisceUnItemSenzaImmagini() {
        Hotel hotel = new Hotel();
        hotel.setId(4L);
        hotel.setHostId(UUID.randomUUID());
        hotel.setTitle("Hotel senza foto");
        hotel.setPrice(BigDecimal.TEN);
        hotel.setCurrency("EUR");
        hotel.setCategory("Hotel");
        hotel.setLocationLat(0.0);
        hotel.setLocationLng(0.0);
        hotel.setRoomType("Standard");
        hotel.setAvailableRooms(1);
        hotel.setAddress("Via Test");
        hotel.setCity("Test City");

        CatalogItemDTO dto = mapper.toDto(hotel);

        assertThat(dto.getImageUrls()).isEmpty();
    }
}