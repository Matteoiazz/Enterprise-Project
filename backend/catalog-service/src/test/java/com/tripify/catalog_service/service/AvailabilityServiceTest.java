package com.tripify.catalog_service.service;

import com.tripify.catalog_service.dto.HoldResultDTO;
import com.tripify.catalog_service.entity.*;
import com.tripify.catalog_service.exception.InsufficientAvailabilityException;
import com.tripify.catalog_service.exception.InvalidHoldStateException;
import com.tripify.catalog_service.repository.*;
import com.tripify.catalog_service.service.impl.AvailabilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Copre il cuore del meccanismo hold/confirm/release: la disponibilità calcolata (non un
 * contatore fisico), il rifiuto quando manca capacità, e il fatto che un blocco scaduto
 * smette di contare da solo senza bisogno di un job di pulizia.
 */
@DataJpaTest
@Import(AvailabilityServiceImpl.class)
@ActiveProfiles("test")
class AvailabilityServiceTest {

    @Autowired
    private AvailabilityService availabilityService;
    @Autowired
    private CatalogItemRepository catalogItemRepository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private FareClassRepository fareClassRepository;
    @Autowired
    private RoomHoldRepository roomHoldRepository;
    @Autowired
    private SeatHoldRepository seatHoldRepository;

    private RoomType roomType;
    private FareClass fareClass;

    @BeforeEach
    void setUp() {
        Hotel hotel = new Hotel();
        hotel.setHostId(UUID.randomUUID());
        hotel.setTitle("Hotel di test");
        hotel.setPrice(new BigDecimal("100"));
        hotel.setCurrency("EUR");
        hotel.setCategory("Hotel");
        hotel.setLocationLat(0.0);
        hotel.setLocationLng(0.0);
        hotel.setAddress("Via Test 1");
        hotel.setCity("Test City");
        hotel = (Hotel) catalogItemRepository.save(hotel);

        roomType = roomTypeRepository.save(RoomType.builder()
                .hotel(hotel).name("Doppia Test").price(new BigDecimal("100")).totalRooms(3).build());

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
        flight = (Flight) catalogItemRepository.save(flight);

        fareClass = fareClassRepository.save(FareClass.builder()
                .flight(flight).name("Economy").price(new BigDecimal("50")).totalSeats(10).build());
    }

    @Test
    void bloccaCameraQuandoCEDisponibilita() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        HoldResultDTO result = availabilityService.holdRoom(roomType.getId(), checkIn, checkOut, 2, "user-1");

        assertThat(result.holdId()).startsWith("room-");
        assertThat(availabilityService.computeRoomAvailability(roomType.getId(), checkIn, checkOut)).isEqualTo(1);
    }

    @Test
    void rifiutaLaCameraQuandoLaCapacitaNonBasta() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        availabilityService.holdRoom(roomType.getId(), checkIn, checkOut, 3, "user-1"); // esaurisce le 3 camere

        assertThatThrownBy(() -> availabilityService.holdRoom(roomType.getId(), checkIn, checkOut, 1, "user-2"))
                .isInstanceOf(InsufficientAvailabilityException.class);
    }

    @Test
    void hostRifiutaSoloLeNottiCheSiSovrappongonoAlBlocco() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        availabilityService.holdRoom(roomType.getId(), checkIn, checkOut, 3, "user-1");

        // Un soggiorno che inizia il giorno del checkout del primo non si sovrappone: deve funzionare.
        HoldResultDTO result = availabilityService.holdRoom(roomType.getId(), checkOut, checkOut.plusDays(1), 3, "user-2");
        assertThat(result.holdId()).isNotBlank();
    }

    @Test
    void confermaRendePermanenteIlBlocco() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        HoldResultDTO hold = availabilityService.holdRoom(roomType.getId(), checkIn, checkOut, 3, "user-1");

        availabilityService.confirm(hold.holdId(), "user-1");

        // Confermato: continua a contare, e non e' piu' rilasciabile.
        assertThat(availabilityService.computeRoomAvailability(roomType.getId(), checkIn, checkOut)).isZero();
        assertThatThrownBy(() -> availabilityService.release(hold.holdId(), "user-1"))
                .isInstanceOf(InvalidHoldStateException.class);
    }

    @Test
    void rilasciareLiberaSubitoLaDisponibilita() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        HoldResultDTO hold = availabilityService.holdRoom(roomType.getId(), checkIn, checkOut, 3, "user-1");

        availabilityService.release(hold.holdId(), "user-1");

        assertThat(availabilityService.computeRoomAvailability(roomType.getId(), checkIn, checkOut)).isEqualTo(3);
    }

    @Test
    void unBloccoScadutoNonContaPiuNellaDisponibilita() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        RoomHold scaduto = roomHoldRepository.save(RoomHold.builder()
                .roomType(roomType).checkIn(checkIn).checkOut(checkOut).rooms(3).userId("user-1")
                .status(HoldStatus.HELD).createdAt(LocalDateTime.now().minusMinutes(30))
                .expiresAt(LocalDateTime.now().minusMinutes(15)) // scaduto 15 minuti fa
                .build());

        assertThat(availabilityService.computeRoomAvailability(roomType.getId(), checkIn, checkOut)).isEqualTo(3);
        // Essendo scaduto, si può comunque bloccare di nuovo tutta la capacità.
        HoldResultDTO nuovoHold = availabilityService.holdRoom(roomType.getId(), checkIn, checkOut, 3, "user-2");
        assertThat(nuovoHold.holdId()).isNotBlank();
        assertThat(scaduto.getId()).isNotNull();
    }

    @Test
    void bloccaPostiQuandoCEDisponibilita() {
        HoldResultDTO result = availabilityService.holdSeats(fareClass.getId(), 4, "user-1");

        assertThat(result.holdId()).startsWith("seat-");
        assertThat(availabilityService.computeSeatAvailability(fareClass.getId())).isEqualTo(6);
    }

    @Test
    void rifiutaIPostiQuandoLaCapacitaNonBasta() {
        availabilityService.holdSeats(fareClass.getId(), 10, "user-1"); // esaurisce tutti i posti

        assertThatThrownBy(() -> availabilityService.holdSeats(fareClass.getId(), 1, "user-2"))
                .isInstanceOf(InsufficientAvailabilityException.class);
    }
}
