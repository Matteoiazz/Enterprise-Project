package com.tripify.booking_service.service;

import com.tripify.booking_service.client.CatalogClient;
import com.tripify.booking_service.client.UserAuthClient;
import com.tripify.booking_service.dto.AddToCartRequestDTO;
import com.tripify.booking_service.dto.BookingResponseDTO;
import com.tripify.booking_service.dto.CatalogItemSummaryDTO;
import com.tripify.booking_service.dto.HoldResultDTO;
import com.tripify.booking_service.dto.PassengerRequestDTO;
import com.tripify.booking_service.dto.PassengerResponseDTO;
import com.tripify.booking_service.entity.Passenger;
import com.tripify.booking_service.exception.AccessDeniedException;
import com.tripify.booking_service.messaging.BookingEventPublisher;
import com.tripify.booking_service.repository.PassengerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Copre l'apertura del check-in (generazione del QR): righe senza una data
 * propria (voli/attività) idonee subito dopo la conferma del pagamento, righe
 * hotel idonee solo entro 24h dal check-in, prenotazioni non ancora CONFERMATE
 * mai idonee, e nessuna rigenerazione per chi ha già un qrCodeData. Copre anche
 * l'autorizzazione di getPassengersForLine (leader/partecipanti soltanto).
 */
@DataJpaTest
@Import({BookingService.class, ShoppingCartService.class, BookingAuditService.class, CheckInService.class})
@ActiveProfiles("test")
class CheckInServiceTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private ShoppingCartService cartService;
    @Autowired
    private CheckInService checkInService;
    @Autowired
    private PassengerRepository passengerRepository;
    @MockitoBean
    private CatalogClient catalogClient;
    @MockitoBean
    private UserAuthClient userAuthClient;
    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private BookingEventPublisher eventPublisher;
    @MockitoBean
    private RabbitTemplate rabbitTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    private static final String LEADER_ID = "leader-1";

    @BeforeEach
    void setUp() {
        when(catalogClient.getItem(anyLong())).thenReturn(
                new CatalogItemSummaryDTO(1L, "Activity", BigDecimal.valueOf(50.0), "EUR", null, null));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Long addPassengerTo(Long lineId) {
        bookingService.addPassenger(lineId, LEADER_ID, new PassengerRequestDTO(
                "Mario", "Rossi", "3331234567", "RSSMRA80A01H501U",
                "PASSPORT", "AB1234567", LocalDate.now().plusYears(2), "ITA"));
        flushAndClear();
        return passengerRepository.findByBookingLineId(lineId).get(0).getId();
    }

    // Volo/attività: nessun roomTypeId, quindi checkIn/checkOut restano null
    // sulla riga - idonea al check-in subito dopo la conferma.
    private Long bookAndConfirmSimpleItem() {
        cartService.addItem(LEADER_ID, new AddToCartRequestDTO(1L, 1, null, null, null, null));
        flushAndClear();
        BookingResponseDTO booking = bookingService.checkout(LEADER_ID);
        flushAndClear();
        bookingService.confirmPayment(booking.id(), LEADER_ID, booking.totalAmount());
        flushAndClear();
        return booking.lines().get(0).id();
    }

    // Hotel: richiede un hold di stanza, quindi roomTypeId + checkIn/checkOut.
    private Long bookAndConfirmHotelItem(LocalDate checkIn) {
        when(catalogClient.holdRoom(eq(5L), any())).thenReturn(new HoldResultDTO("room-1", LocalDateTime.now().plusMinutes(15)));

        cartService.addItem(LEADER_ID, new AddToCartRequestDTO(1L, 1, 5L, null, checkIn, checkIn.plusDays(2)));
        flushAndClear();
        BookingResponseDTO booking = bookingService.checkout(LEADER_ID);
        flushAndClear();
        bookingService.confirmPayment(booking.id(), LEADER_ID, booking.totalAmount());
        flushAndClear();
        return booking.lines().get(0).id();
    }

    @Test
    void apreIlCheckInSubitoPerUnaRigaSenzaDataDiCheckIn() {
        Long passengerId = addPassengerTo(bookAndConfirmSimpleItem());

        checkInService.openCheckIn();
        flushAndClear();

        assertThat(passengerRepository.findById(passengerId).orElseThrow().getQrCodeData()).isNotBlank();
    }

    @Test
    void nonApreIlCheckInSeLaPrenotazioneNonEAncoraConfermata() {
        cartService.addItem(LEADER_ID, new AddToCartRequestDTO(1L, 1, null, null, null, null));
        flushAndClear();
        BookingResponseDTO booking = bookingService.checkout(LEADER_ID); // resta PENDING, niente confirmPayment
        Long passengerId = addPassengerTo(booking.lines().get(0).id());

        checkInService.openCheckIn();
        flushAndClear();

        assertThat(passengerRepository.findById(passengerId).orElseThrow().getQrCodeData()).isNull();
    }

    @Test
    void nonRigeneraIlQrDiUnPasseggeroGiaProcessato() {
        Long passengerId = addPassengerTo(bookAndConfirmSimpleItem());

        checkInService.openCheckIn();
        flushAndClear();
        String primoQr = passengerRepository.findById(passengerId).orElseThrow().getQrCodeData();

        checkInService.openCheckIn();
        flushAndClear();
        String secondoQr = passengerRepository.findById(passengerId).orElseThrow().getQrCodeData();

        assertThat(secondoQr).isEqualTo(primoQr);
    }

    @Test
    void nonApreIlCheckInPerUnaRigaHotelConCheckInLontano() {
        Long passengerId = addPassengerTo(bookAndConfirmHotelItem(LocalDate.now().plusDays(10)));

        checkInService.openCheckIn();
        flushAndClear();

        assertThat(passengerRepository.findById(passengerId).orElseThrow().getQrCodeData()).isNull();
    }

    @Test
    void apreIlCheckInPerUnaRigaHotelConCheckInDomani() {
        Long passengerId = addPassengerTo(bookAndConfirmHotelItem(LocalDate.now().plusDays(1)));

        checkInService.openCheckIn();
        flushAndClear();

        assertThat(passengerRepository.findById(passengerId).orElseThrow().getQrCodeData()).isNotBlank();
    }

    @Test
    void getPassengersForLineRifiutaUnEstraneoAllaPrenotazione() {
        Long lineId = bookAndConfirmSimpleItem();
        addPassengerTo(lineId);

        assertThatThrownBy(() -> bookingService.getPassengersForLine(lineId, "estraneo"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getPassengersForLineRestituisceIlQrAlLeaderDopoLApertura() {
        Long lineId = bookAndConfirmSimpleItem();
        addPassengerTo(lineId);
        checkInService.openCheckIn();
        flushAndClear();

        List<PassengerResponseDTO> passeggeri = bookingService.getPassengersForLine(lineId, LEADER_ID);

        assertThat(passeggeri).hasSize(1);
        assertThat(passeggeri.get(0).qrCodeData()).isNotBlank();
        assertThat(passeggeri.get(0).checkedIn()).isFalse();
    }
}
