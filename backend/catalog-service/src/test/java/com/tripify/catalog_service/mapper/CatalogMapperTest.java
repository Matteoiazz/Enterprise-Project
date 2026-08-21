package com.tripify.catalog_service.mapper;

import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.entity.Activity;
import com.tripify.catalog_service.entity.FareClass;
import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.entity.Hotel;
import com.tripify.catalog_service.entity.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogMapperTest {

    private final CatalogMapper mapper = new CatalogMapper();

    @Test
    void mappaCorrettamenteUnVoloConLeSueClassiTariffarie() {
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
        flight.setTotalSeats(60);
        flight.setStops(0);
        flight.setFareClasses(List.of(
                FareClass.builder().id(1L).flight(flight).name("Economy").price(new BigDecimal("89.00")).totalSeats(48).build(),
                FareClass.builder().id(2L).flight(flight).name("Business").price(new BigDecimal("205.00")).totalSeats(12).build()
        ));

        CatalogItemDTO dto = mapper.toDto(flight);

        assertThat(dto.getItemType()).isEqualTo("Flight");
        assertThat(dto.getDepartureAirport()).isEqualTo("FCO");
        assertThat(dto.getArrivalAirport()).isEqualTo("LIN");
        assertThat(dto.getDepartureCity()).isEqualTo("Roma");
        assertThat(dto.getArrivalCity()).isEqualTo("Milano");
        assertThat(dto.getStops()).isEqualTo(0);
        assertThat(dto.getActivityType()).isNull();
        assertThat(dto.getFareClasses()).extracting("name").containsExactly("Economy", "Business");
        // "Da €X" = il minimo tra le classi, non il prezzo di listino del volo.
        assertThat(dto.getPrice()).isEqualByComparingTo("89.00");
    }

    @Test
    void mappaCorrettamenteUnHotelConAmenitiesETipologie() {
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
        hotel.setAddress("234 W 42nd St");
        hotel.setCity("New York");
        hotel.setAmenities(List.of("Wi-Fi", "Palestra"));
        hotel.setRoomTypes(List.of(
                RoomType.builder().id(1L).hotel(hotel).name("Doppia Deluxe").price(new BigDecimal("250.00")).totalRooms(10).maxOccupancy(2).build(),
                RoomType.builder().id(2L).hotel(hotel).name("Suite Panoramica").price(new BigDecimal("420.00")).totalRooms(5).maxOccupancy(3).build()
        ));

        CatalogItemDTO dto = mapper.toDto(hotel);

        assertThat(dto.getItemType()).isEqualTo("Hotel");
        assertThat(dto.getCity()).isEqualTo("New York");
        assertThat(dto.getAmenities()).containsExactly("Wi-Fi", "Palestra");
        assertThat(dto.getLocationLat()).isEqualTo(40.7589);
        assertThat(dto.getDepartureAirport()).isNull();
        assertThat(dto.getRoomTypes()).extracting("name").containsExactly("Doppia Deluxe", "Suite Panoramica");
        assertThat(dto.getPrice()).isEqualByComparingTo("250.00");
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
        hotel.setAddress("Via Test");
        hotel.setCity("Test City");
        hotel.setRoomTypes(List.of(
                RoomType.builder().id(3L).hotel(hotel).name("Standard").price(BigDecimal.TEN).totalRooms(1).build()
        ));

        CatalogItemDTO dto = mapper.toDto(hotel);

        assertThat(dto.getImageUrls()).isEmpty();
    }
}
