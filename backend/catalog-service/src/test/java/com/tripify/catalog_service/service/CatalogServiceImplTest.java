package com.tripify.catalog_service.service;

import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.entity.*;
import com.tripify.catalog_service.mapper.CatalogMapper;
import com.tripify.catalog_service.repository.*;
import com.tripify.catalog_service.service.impl.AvailabilityServiceImpl;
import com.tripify.catalog_service.service.impl.CatalogServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Copre la disattivazione (soft delete) di un item del catalogo: prima di questo fix,
 * repository.delete(...) con ereditarietà JOINED eseguiva comunque una vera DELETE sulle
 * tabelle figlie (fare_classes/flight_details), orfanando la riga padre o violando la
 * foreign key di un hold esistente — l'annuncio non doveva mai poter sparire dal DB.
 */
@DataJpaTest
@Import({CatalogServiceImpl.class, CatalogMapper.class, AvailabilityServiceImpl.class})
@ActiveProfiles("test")
class CatalogServiceImplTest {

    @Autowired
    private CatalogService catalogService;
    @Autowired
    private CatalogItemRepository catalogItemRepository;
    @Autowired
    private FareClassRepository fareClassRepository;
    @Autowired
    private SeatHoldRepository seatHoldRepository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;

    private Flight saveFlight() {
        Flight flight = new Flight();
        flight.setHostId(UUID.randomUUID());
        flight.setTitle("Volo di test");
        flight.setPrice(new BigDecimal("50"));
        flight.setCurrency("EUR");
        flight.setCategory("Voli");
        flight.setDepartureAirport("AAA");
        flight.setArrivalAirport("BBB");
        flight.setDepartureCity("Città A");
        flight.setArrivalCity("Città B");
        flight.setDepartureTime(LocalDateTime.now().plusDays(5));
        flight.setArrivalTime(LocalDateTime.now().plusDays(5).plusHours(2));
        flight.setTotalSeats(10);
        flight.setStops(0);
        return (Flight) catalogItemRepository.save(flight);
    }

    @Test
    void disattivareUnItemSenzaHoldNonLoCancellaERestaLeggibile() {
        Flight flight = saveFlight();
        Long id = flight.getId();

        assertThatCode(() -> catalogService.deactivateItem(id)).doesNotThrowAnyException();

        CatalogItem stillThere = catalogItemRepository.findById(id).orElseThrow();
        assertThat(stillThere.isActive()).isFalse();
        // Prima del fix: risolvere di nuovo il sottotipo poteva fallire (riga figlia
        // cancellata dalla vera DELETE cascata da repository.delete()).
        assertThatCode(() -> catalogService.getItemById(id)).doesNotThrowAnyException();
    }

    @Test
    void disattivareUnVoloConUnHoldConfermatoNonViolaLaForeignKey() {
        Flight flight = saveFlight();
        FareClass fareClass = fareClassRepository.save(FareClass.builder()
                .flight(flight).name("Economy").price(new BigDecimal("50")).totalSeats(10).build());
        seatHoldRepository.save(SeatHold.builder()
                .fareClass(fareClass).seats(2).userId("user-1")
                .status(HoldStatus.CONFIRMED).createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build());
        Long id = flight.getId();

        // Prima del fix: la DELETE cascata su fare_classes violava la FK di seat_holds,
        // impedendo per sempre di disattivare un volo che era stato davvero prenotato.
        assertThatCode(() -> catalogService.deactivateItem(id)).doesNotThrowAnyException();

        assertThat(catalogItemRepository.findById(id).orElseThrow().isActive()).isFalse();
        assertThat(fareClassRepository.findById(fareClass.getId())).isPresent();
    }

    @Test
    void unRatingMedioNonFinitoVieneTrattatoComeNessunaRecensioneInveceDiCorromperlaValidazione() {
        Flight flight = saveFlight();
        Long id = flight.getId();

        // Prima del fix: Math.round(NaN) = 0, e item.setRating(0) violava @Min(1) al
        // salvataggio invece di essere trattato come "nessun voto valido" (come null).
        assertThatCode(() -> catalogService.updateRating(id, Double.NaN, 3)).doesNotThrowAnyException();

        CatalogItem item = catalogItemRepository.findById(id).orElseThrow();
        assertThat(item.getRating()).isNull();
        assertThat(item.getRatingAvg()).isNull();
        assertThat(item.getReviewCount()).isZero();
    }

    @Test
    void cercareHotelConDateEscludeQuelliConCapienzaStrutturalmenteInsufficiente() {
        Hotel hotelGrande = new Hotel();
        hotelGrande.setHostId(UUID.randomUUID());
        hotelGrande.setTitle("Hotel con suite");
        hotelGrande.setPrice(new BigDecimal("200"));
        hotelGrande.setCurrency("EUR");
        hotelGrande.setCategory("Hotel");
        hotelGrande.setCity("Roma");
        hotelGrande.setLocationLat(41.9);
        hotelGrande.setLocationLng(12.5);
        hotelGrande.setAddress("Via Roma 1");
        hotelGrande = (Hotel) catalogItemRepository.save(hotelGrande);
        RoomType suite = roomTypeRepository.save(RoomType.builder().hotel(hotelGrande).name("Suite").price(new BigDecimal("200")).totalRooms(3).build());
        // roomTypes è @OneToMany(mappedBy="hotel"): salvare la RoomType dal suo lato non
        // aggiorna da sola la collezione già in memoria su hotelGrande in questa stessa
        // transazione (a differenza di un caricamento a freddo in una richiesta reale).
        hotelGrande.getRoomTypes().add(suite);

        Hotel hotelPiccolo = new Hotel();
        hotelPiccolo.setHostId(UUID.randomUUID());
        hotelPiccolo.setTitle("Hotel con singola");
        hotelPiccolo.setPrice(new BigDecimal("80"));
        hotelPiccolo.setCurrency("EUR");
        hotelPiccolo.setCategory("Hotel");
        hotelPiccolo.setCity("Roma");
        hotelPiccolo.setLocationLat(41.9);
        hotelPiccolo.setLocationLng(12.5);
        hotelPiccolo.setAddress("Via Roma 2");
        hotelPiccolo = (Hotel) catalogItemRepository.save(hotelPiccolo);
        roomTypeRepository.save(RoomType.builder().hotel(hotelPiccolo).name("Singola").price(new BigDecimal("80")).totalRooms(1).build());

        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        // Prima del fix: findAll(spec, pageable.getSort()) caricava in memoria OGNI
        // hotel attivo, indipendentemente dalla capienza, prima del controllo preciso.
        Page<CatalogItemDTO> risultati = catalogService.search(
                "Hotel", null, null, null, null, null, null, null, null, null, null,
                checkIn, checkOut, 2, PageRequest.of(0, 10)
        );

        assertThat(risultati.getContent()).extracting(CatalogItemDTO::getTitle).contains("Hotel con suite");
        assertThat(risultati.getContent()).extracting(CatalogItemDTO::getTitle).doesNotContain("Hotel con singola");
    }
}
