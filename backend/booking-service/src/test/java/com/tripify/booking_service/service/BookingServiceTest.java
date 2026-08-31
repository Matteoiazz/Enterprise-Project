package com.tripify.booking_service.service;

import com.tripify.booking_service.client.CatalogClient;
import com.tripify.booking_service.dto.AddToCartRequestDTO;
import com.tripify.booking_service.dto.BookingResponseDTO;
import com.tripify.booking_service.dto.CatalogItemSummaryDTO;
import com.tripify.booking_service.dto.HoldResultDTO;
import com.tripify.booking_service.entity.Booking;
import com.tripify.booking_service.entity.BookingStatus;
import com.tripify.booking_service.exception.AccessDeniedException;
import com.tripify.booking_service.exception.EmptyCartException;
import com.tripify.booking_service.exception.InvalidBookingStateException;
import com.tripify.booking_service.exception.PaymentValidationException;
import com.tripify.booking_service.messaging.BookingEventPublisher;
import com.tripify.booking_service.repository.BookingRepository;
import feign.FeignException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Copre il cuore del ciclo di vita di una Booking: checkout (carrello -> PENDING),
 * conferma pagamento (PENDING -> CONFIRMED, con verifica proprietario/importo e conferma
 * degli hold su catalog-service) e annullamento (rilascio hold se non confermata,
 * rimborso se lo era), oltre a inviti e limite passeggeri per riga.
 *
 * Nota: dopo ogni addItem() si forza flush()+clear() prima del checkout, perché
 * @DataJpaTest tiene l'intero test in un'unica sessione Hibernate (a differenza della
 * produzione, dove ogni richiesta HTTP ne apre una nuova) e il carrello appena creato
 * resterebbe altrimenti "in cache" con la collection items non aggiornata.
 */
@DataJpaTest
@Import({BookingService.class, ShoppingCartService.class, BookingAuditService.class})
@ActiveProfiles("test")
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private ShoppingCartService cartService;
    @Autowired
    private BookingRepository bookingRepository;
    @MockitoBean
    private CatalogClient catalogClient;
    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private BookingEventPublisher eventPublisher;
    @MockitoBean
    private RabbitTemplate rabbitTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    private static final String LEADER_ID = "leader-1";
    private static final String OTHER_USER_ID = "other-1";

    @BeforeEach
    void setUp() {
        when(catalogClient.getItem(anyLong())).thenReturn(
                new CatalogItemSummaryDTO(1L, "Activity", BigDecimal.valueOf(50.0), null, null));
    }

    private void addItem(String userId, AddToCartRequestDTO request) {
        cartService.addItem(userId, request);
        entityManager.flush();
        entityManager.clear();
    }

    private BookingResponseDTO checkoutSimpleCartFor(String userId) {
        addItem(userId, new AddToCartRequestDTO(1L, 2, null, null, null, null)); // 2 * 50.0 = 100.0
        return bookingService.checkout(userId);
    }

    @Test
    void ilCheckoutTrasformaIlCarrelloInUnaPrenotazionePending() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.totalAmount()).isEqualByComparingTo("100.0");
        assertThat(booking.isLeader()).isTrue();
        assertThat(booking.lines()).hasSize(1);
        assertThat(booking.lines().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void ilCheckoutSelettivoPrenotaSoloGliArticoliScelti() {
        addItem(LEADER_ID, new AddToCartRequestDTO(1L, 1, null, null, null, null)); // 50.0
        when(catalogClient.getItem(2L)).thenReturn(
                new CatalogItemSummaryDTO(2L, "Activity", BigDecimal.valueOf(30.0), null, null));
        addItem(LEADER_ID, new AddToCartRequestDTO(2L, 1, null, null, null, null)); // 30.0, riga separata

        Long secondItemId = cartService.getCartDTOForUser(LEADER_ID).items().stream()
                .filter(i -> i.catalogItemId().equals(2L)).findFirst().get().id();

        BookingResponseDTO booking = bookingService.checkout(LEADER_ID, java.util.List.of(secondItemId));

        assertThat(booking.totalAmount()).isEqualByComparingTo("30.0");
        assertThat(booking.lines()).hasSize(1);
        assertThat(booking.lines().get(0).catalogItemId()).isEqualTo(2L);

        var remainingCart = cartService.getCartDTOForUser(LEADER_ID);
        assertThat(remainingCart.items()).hasSize(1);
        assertThat(remainingCart.items().get(0).catalogItemId()).isEqualTo(1L);
    }

    @Test
    void ilCheckoutConCarrelloVuotoLanciaEmptyCartException() {
        assertThatThrownBy(() -> bookingService.checkout("utente-senza-carrello"))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void ilCheckoutTrasferisceLHoldDallaCartAllaBookingLineSenzaRilasciarlo() {
        when(catalogClient.holdSeats(eq(7L), any())).thenReturn(new HoldResultDTO("seat-1", LocalDateTime.now().plusMinutes(15)));
        addItem(LEADER_ID, new AddToCartRequestDTO(1L, 1, null, 7L, null, null));

        BookingResponseDTO booking = bookingService.checkout(LEADER_ID);

        verify(catalogClient, never()).releaseHold(any());
        assertThat(booking.lines().get(0).fareClassId()).isEqualTo(7L);
    }

    @Test
    void confirmPaymentPortaLaPrenotazioneAConfirmedEConfermaGliHold() {
        when(catalogClient.holdSeats(eq(7L), any())).thenReturn(new HoldResultDTO("seat-1", LocalDateTime.now().plusMinutes(15)));
        addItem(LEADER_ID, new AddToCartRequestDTO(1L, 1, null, 7L, null, null)); // 50.0
        BookingResponseDTO booking = bookingService.checkout(LEADER_ID);

        var confirmed = bookingService.confirmPayment(booking.id(), LEADER_ID, new BigDecimal("50.0"));

        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(catalogClient).confirmHold("seat-1");
    }

    @Test
    void confirmPaymentRifiutaSeLimportoNonCorrispondeENonTocaGliHold() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);

        assertThatThrownBy(() -> bookingService.confirmPayment(booking.id(), LEADER_ID, new BigDecimal("1.0")))
                .isInstanceOf(PaymentValidationException.class);
        verify(catalogClient, never()).confirmHold(any());
    }

    @Test
    void confirmPaymentRifiutaSeNonSeIlProprietario() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);

        assertThatThrownBy(() -> bookingService.confirmPayment(booking.id(), OTHER_USER_ID, booking.totalAmount()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmPaymentRifiutaSeLaPrenotazioneNonEPiuPending() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);
        bookingService.confirmPayment(booking.id(), LEADER_ID, booking.totalAmount());

        assertThatThrownBy(() -> bookingService.confirmPayment(booking.id(), LEADER_ID, booking.totalAmount()))
                .isInstanceOf(InvalidBookingStateException.class);
    }

    // Simula il caso reale del retry di un pagamento su una Booking rimasta
    // PENDING a lungo: nel frattempo il blocco su catalog-service è scaduto
    // (409 sul confirm), quindi la conferma deve fallire con un messaggio
    // chiaro invece di un errore di integrazione generico, e la Booking deve
    // restare PENDING (nessuna conferma parziale).
    @Test
    void confirmPaymentSegnalaChiaramenteUnHoldNonPiuDisponibile() {
        when(catalogClient.holdSeats(eq(7L), any())).thenReturn(new HoldResultDTO("seat-1", LocalDateTime.now().plusMinutes(15)));
        addItem(LEADER_ID, new AddToCartRequestDTO(1L, 1, null, 7L, null, null));
        BookingResponseDTO booking = bookingService.checkout(LEADER_ID);

        FeignException expired = mock(FeignException.class);
        when(expired.status()).thenReturn(409);
        doThrow(expired).when(catalogClient).confirmHold("seat-1");

        assertThatThrownBy(() -> bookingService.confirmPayment(booking.id(), LEADER_ID, booking.totalAmount()))
                .isInstanceOf(InvalidBookingStateException.class)
                .hasMessageContaining("non sono più disponibili");

        Booking stillPending = bookingRepository.findById(booking.id()).orElseThrow();
        assertThat(stillPending.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void cancelBookingRilasciaGliHoldSeNonEraAncoraConfermata() {
        when(catalogClient.holdSeats(eq(7L), any())).thenReturn(new HoldResultDTO("seat-1", LocalDateTime.now().plusMinutes(15)));
        addItem(LEADER_ID, new AddToCartRequestDTO(1L, 1, null, 7L, null, null));
        BookingResponseDTO booking = bookingService.checkout(LEADER_ID);

        BookingResponseDTO cancelled = bookingService.cancelBooking(booking.id(), LEADER_ID);

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(catalogClient).releaseHold("seat-1");
        verify(paymentService, never()).refund(any(), any());
    }

    @Test
    void cancelBookingAvviaIlRimborsoSeEraGiaConfermata() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);
        bookingService.confirmPayment(booking.id(), LEADER_ID, booking.totalAmount());

        bookingService.cancelBooking(booking.id(), LEADER_ID);

        verify(paymentService).refund(booking.id(), booking.totalAmount());
    }

    @Test
    void cancelBookingRifiutaSeGiaAnnullata() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);
        bookingService.cancelBooking(booking.id(), LEADER_ID);

        assertThatThrownBy(() -> bookingService.cancelBooking(booking.id(), LEADER_ID))
                .isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void inviteFriendAggiungeUnPartecipanteEImpedisceIlDoppioInvito() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);

        BookingResponseDTO afterInvite = bookingService.inviteFriend(booking.id(), LEADER_ID, "amico-1");
        assertThat(afterInvite.participantIds()).containsExactly("amico-1");

        assertThatThrownBy(() -> bookingService.inviteFriend(booking.id(), LEADER_ID, "amico-1"))
                .isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void inviteFriendRifiutaLAutoInvito() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);

        assertThatThrownBy(() -> bookingService.inviteFriend(booking.id(), LEADER_ID, LEADER_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inviteFriendRifiutaSeLaPrenotazioneEAnnullata() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);
        bookingService.cancelBooking(booking.id(), LEADER_ID);

        assertThatThrownBy(() -> bookingService.inviteFriend(booking.id(), LEADER_ID, "amico-1"))
                .isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void addPassengerRispettaIlLimiteDiQuantityDellaRiga() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID); // quantity = 2
        Long lineId = booking.lines().get(0).id();
        var passeggero = passengerRequest("Mario", "Rossi");

        // flush()+clear() tra una chiamata e l'altra: in produzione ogni addPassenger()
        // è una richiesta/transazione separata, quindi bookingLineRepository.findById()
        // rilegge sempre lo stato reale. Qui invece siamo nella stessa sessione
        // @DataJpaTest per tutto il metodo, quindi senza forzare il refresh la
        // collection line.getPassengers() resterebbe quella (vuota) vista la prima volta.
        bookingService.addPassenger(lineId, LEADER_ID, passeggero);
        entityManager.flush();
        entityManager.clear();
        bookingService.addPassenger(lineId, LEADER_ID, passengerRequest("Anna", "Bianchi"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> bookingService.addPassenger(lineId, LEADER_ID, passengerRequest("Luca", "Verdi")))
                .isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void addPassengerRifiutaSeNonSeIlLeader() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);
        Long lineId = booking.lines().get(0).id();

        assertThatThrownBy(() -> bookingService.addPassenger(lineId, OTHER_USER_ID, passengerRequest("Mario", "Rossi")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addPassengerRifiutaSeLaPrenotazioneEAnnullata() {
        BookingResponseDTO booking = checkoutSimpleCartFor(LEADER_ID);
        Long lineId = booking.lines().get(0).id();
        bookingService.cancelBooking(booking.id(), LEADER_ID);

        assertThatThrownBy(() -> bookingService.addPassenger(lineId, LEADER_ID, passengerRequest("Mario", "Rossi")))
                .isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void getUserHistoryRestituisceIViaggiDoveSeiLeaderOPartecipante() {
        BookingResponseDTO ownBooking = checkoutSimpleCartFor(LEADER_ID);
        BookingResponseDTO friendsBooking = checkoutSimpleCartFor(OTHER_USER_ID);
        bookingService.inviteFriend(friendsBooking.id(), OTHER_USER_ID, LEADER_ID);

        var history = bookingService.getUserHistory(LEADER_ID, PageRequest.of(0, 10));

        assertThat(history.getContent()).hasSize(2);
        assertThat(history.getContent())
                .filteredOn(b -> b.id().equals(ownBooking.id()))
                .allMatch(BookingResponseDTO::isLeader);
        assertThat(history.getContent())
                .filteredOn(b -> b.id().equals(friendsBooking.id()))
                .allMatch(b -> !b.isLeader());
    }

    @Test
    void getReceivedBookingsRestituisceSoloLeRigheSugliAnnunciDiChiChiama() {
        BookingResponseDTO booking = checkoutSimpleCartFor(OTHER_USER_ID); // catalogItemId = 1

        when(catalogClient.getMyItems()).thenReturn(
                java.util.List.of(new CatalogItemSummaryDTO(1L, "Activity", BigDecimal.valueOf(50.0), null, null)));

        var received = bookingService.getReceivedBookings();

        assertThat(received).hasSize(1);
        assertThat(received.get(0).bookingId()).isEqualTo(booking.id());
        assertThat(received.get(0).buyerUserId()).isEqualTo(OTHER_USER_ID);
        assertThat(received.get(0).catalogItemId()).isEqualTo(1L);
    }

    @Test
    void getReceivedBookingsRestituisceListaVuotaSeNonHaAnnunci() {
        checkoutSimpleCartFor(OTHER_USER_ID);
        when(catalogClient.getMyItems()).thenReturn(java.util.List.of());

        assertThat(bookingService.getReceivedBookings()).isEmpty();
    }

    private com.tripify.booking_service.dto.PassengerRequestDTO passengerRequest(String firstName, String lastName) {
        return new com.tripify.booking_service.dto.PassengerRequestDTO(
                firstName, lastName, "3331234567", "RSSMRA80A01H501U", "PASSPORT", "AB1234567",
                java.time.LocalDate.now().plusYears(2), "ITA");
    }
}
