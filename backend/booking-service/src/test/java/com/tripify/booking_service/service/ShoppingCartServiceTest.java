package com.tripify.booking_service.service;

import com.tripify.booking_service.client.CatalogClient;
import com.tripify.booking_service.dto.AddToCartRequestDTO;
import com.tripify.booking_service.dto.CartDTO;
import com.tripify.booking_service.dto.CatalogItemSummaryDTO;
import com.tripify.booking_service.dto.HoldResultDTO;
import com.tripify.booking_service.entity.CartItem;
import com.tripify.booking_service.entity.ShoppingCart;
import com.tripify.booking_service.exception.CatalogItemNotFoundException;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Copre il cuore del carrello: il prezzo viene sempre riletto dal catalogo (mai fidato
 * dal client), gli articoli "semplici" si sommano per quantità mentre quelli con un hold
 * aperto (camera/posto) restano righe separate, e il rilascio degli hold avviene solo
 * quando l'utente svuota davvero il carrello, non quando il checkout li trasferisce
 * alla Booking (vedi anche BookingServiceTest).
 *
 * Nota: @DataJpaTest esegue l'intero metodo di test in un'unica sessione Hibernate,
 * a differenza della produzione dove ogni richiesta HTTP ne apre una nuova. Per questo
 * dopo ogni chiamata che scrive si forza flush()+clear(): senza, le letture successive
 * nella stessa sessione vedrebbero ancora l'istanza in cache (con la collection items
 * non aggiornata) invece di rileggere lo stato reale dal database.
 */
@DataJpaTest
@Import(ShoppingCartService.class)
@ActiveProfiles("test")
class ShoppingCartServiceTest {

    @Autowired
    private ShoppingCartService cartService;
    @MockitoBean
    private CatalogClient catalogClient;
    @MockitoBean
    private RabbitTemplate rabbitTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        when(catalogClient.getItem(anyLong())).thenReturn(
                new CatalogItemSummaryDTO(1L, "Activity", java.math.BigDecimal.valueOf(99.0), "EUR", null, null));
    }

    private void addItem(AddToCartRequestDTO request) {
        cartService.addItem(USER_ID, request);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void aggiungeUnArticoloSempliceUsandoIlPrezzoDelCatalogo() {
        addItem(new AddToCartRequestDTO(1L, 2, null, null, null, null));

        ShoppingCart cart = cartService.getCartForUser(USER_ID);
        assertThat(cart.getItems()).hasSize(1);
        CartItem item = cart.getItems().get(0);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getPriceAtAdded()).isEqualByComparingTo("99.0");
        assertThat(item.getCurrency()).isEqualTo("EUR");
        assertThat(item.getHoldId()).isNull();
    }

    @Test
    void sommaLaQuantitaSeLoStessoArticoloVieneAggiuntoDueVolte() {
        addItem(new AddToCartRequestDTO(1L, 2, null, null, null, null));
        addItem(new AddToCartRequestDTO(1L, 3, null, null, null, null));

        ShoppingCart cart = cartService.getCartForUser(USER_ID);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void rifiutaArticoloNonPresenteNelCatalogo() {
        when(catalogClient.getItem(404L)).thenReturn(null);

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddToCartRequestDTO(404L, 1, null, null, null, null)))
                .isInstanceOf(CatalogItemNotFoundException.class);
    }

    @Test
    void apreUnHoldELoSalvaSullArticoloQuandoESceltaUnaCameraHotel() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        when(catalogClient.holdRoom(eq(5L), any())).thenReturn(new HoldResultDTO("room-1", LocalDateTime.now().plusMinutes(15)));

        addItem(new AddToCartRequestDTO(1L, 2, 5L, null, checkIn, checkOut));

        ShoppingCart cart = cartService.getCartForUser(USER_ID);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getHoldId()).isEqualTo("room-1");
        verify(catalogClient).holdRoom(eq(5L), any());
    }

    @Test
    void nonUnisceDueArticoliConHoldAncheSeStessoCatalogItemId() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        when(catalogClient.holdRoom(eq(5L), any()))
                .thenReturn(new HoldResultDTO("room-1", LocalDateTime.now().plusMinutes(15)))
                .thenReturn(new HoldResultDTO("room-2", LocalDateTime.now().plusMinutes(15)));

        addItem(new AddToCartRequestDTO(1L, 1, 5L, null, checkIn, checkOut));
        addItem(new AddToCartRequestDTO(1L, 1, 5L, null, checkIn, checkOut));

        ShoppingCart cart = cartService.getCartForUser(USER_ID);
        assertThat(cart.getItems()).hasSize(2);
        assertThat(cart.getItems()).extracting(CartItem::getHoldId).containsExactlyInAnyOrder("room-1", "room-2");
    }

    @Test
    void richiedeCheckInECheckOutPerLeCamere() {
        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddToCartRequestDTO(1L, 1, 5L, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rifiutaCheckOutNonSuccessivoAlCheckIn() {
        LocalDate checkIn = LocalDate.now().plusDays(10);

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddToCartRequestDTO(1L, 1, 5L, null, checkIn, checkIn)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddToCartRequestDTO(1L, 1, 5L, null, checkIn, checkIn.minusDays(1))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(catalogClient, never()).holdRoom(any(), any());
    }

    @Test
    void rifiutaSeSonoSpecificatiSiaRoomTypeCheFareClass() {
        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddToCartRequestDTO(1L, 1, 5L, 7L, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void svuotareIlCarrelloRilasciaGliHoldAperti() {
        when(catalogClient.holdSeats(eq(7L), any())).thenReturn(new HoldResultDTO("seat-1", LocalDateTime.now().plusMinutes(15)));
        addItem(new AddToCartRequestDTO(1L, 1, null, 7L, null, null));

        cartService.clearCart(USER_ID);
        entityManager.flush();
        entityManager.clear();

        verify(catalogClient).releaseHold("seat-1");
        assertThat(cartService.getCartForUser(USER_ID).getItems()).isEmpty();
    }

    @Test
    void removeCheckedOutItemsNonRilasciaGliHold() {
        when(catalogClient.holdSeats(eq(7L), any())).thenReturn(new HoldResultDTO("seat-1", LocalDateTime.now().plusMinutes(15)));
        addItem(new AddToCartRequestDTO(1L, 1, null, 7L, null, null));
        Long itemId = cartService.getCartForUser(USER_ID).getItems().get(0).getId();

        cartService.removeCheckedOutItems(USER_ID, List.of(itemId));
        entityManager.flush();
        entityManager.clear();

        verify(catalogClient, never()).releaseHold(any());
        assertThat(cartService.getCartForUser(USER_ID).getItems()).isEmpty();
    }

    @Test
    void removeCheckedOutItemsLasciaGliAltriArticoliNelCarrello() {
        addItem(new AddToCartRequestDTO(1L, 1, null, null, null, null));
        when(catalogClient.getItem(2L)).thenReturn(
                new com.tripify.booking_service.dto.CatalogItemSummaryDTO(2L, "Activity", java.math.BigDecimal.valueOf(50.0), "EUR", null, null));
        addItem(new AddToCartRequestDTO(2L, 1, null, null, null, null));
        Long firstItemId = cartService.getCartForUser(USER_ID).getItems().stream()
                .filter(i -> i.getCatalogItemId().equals(1L)).findFirst().get().getId();

        cartService.removeCheckedOutItems(USER_ID, List.of(firstItemId));
        entityManager.flush();
        entityManager.clear();

        ShoppingCart remaining = cartService.getCartForUser(USER_ID);
        assertThat(remaining.getItems()).hasSize(1);
        assertThat(remaining.getItems().get(0).getCatalogItemId()).isEqualTo(2L);
    }

    @Test
    void removeItemRimuoveSoloQuellArticoloERilasciaIlSuoHold() {
        when(catalogClient.holdSeats(eq(7L), any())).thenReturn(new HoldResultDTO("seat-1", LocalDateTime.now().plusMinutes(15)));
        addItem(new AddToCartRequestDTO(1L, 1, null, 7L, null, null));
        addItem(new AddToCartRequestDTO(1L, 1, null, null, null, null));
        Long holdItemId = cartService.getCartForUser(USER_ID).getItems().stream()
                .filter(i -> i.getHoldId() != null).findFirst().get().getId();

        cartService.removeItem(USER_ID, holdItemId);
        entityManager.flush();
        entityManager.clear();

        verify(catalogClient).releaseHold("seat-1");
        assertThat(cartService.getCartForUser(USER_ID).getItems()).hasSize(1);
    }

    @Test
    void removeItemRifiutaUnArticoloNonPresenteNelCarrello() {
        assertThatThrownBy(() -> cartService.removeItem(USER_ID, 999L))
                .isInstanceOf(com.tripify.booking_service.exception.ResourceNotFoundException.class);
    }

    @Test
    void purgeExpiredCartItemsRimuoveSoloGliArticoliScadutiRilasciandoGliHold() {
        when(catalogClient.holdSeats(eq(7L), any())).thenReturn(new HoldResultDTO("seat-1", LocalDateTime.now().plusMinutes(15)));
        addItem(new AddToCartRequestDTO(1L, 1, null, 7L, null, null)); // scaduto
        addItem(new AddToCartRequestDTO(1L, 1, null, null, null, null)); // fresco

        CartItem oldItem = cartService.getCartForUser(USER_ID).getItems().stream()
                .filter(i -> i.getHoldId() != null).findFirst().get();
        oldItem.setAddedAt(LocalDateTime.now().minusMinutes(16));
        entityManager.flush();
        entityManager.clear();

        cartService.purgeExpiredCartItems();
        entityManager.flush();
        entityManager.clear();

        verify(catalogClient).releaseHold("seat-1");
        ShoppingCart remaining = cartService.getCartForUser(USER_ID);
        assertThat(remaining.getItems()).hasSize(1);
        assertThat(remaining.getItems().get(0).getHoldId()).isNull();
    }

    @Test
    void getCartDTOForUserRiportaLaValutaDiCiascunArticolo() {
        addItem(new AddToCartRequestDTO(1L, 1, null, null, null, null)); // EUR (setUp)
        when(catalogClient.getItem(2L)).thenReturn(
                new CatalogItemSummaryDTO(2L, "Activity", java.math.BigDecimal.valueOf(40.0), "USD", null, null));
        addItem(new AddToCartRequestDTO(2L, 1, null, null, null, null));

        CartDTO dto = cartService.getCartDTOForUser(USER_ID);

        assertThat(dto.items()).extracting(com.tripify.booking_service.dto.CartItemDTO::currency)
                .containsExactlyInAnyOrder("EUR", "USD");
    }

    @Test
    void getCartDTOForUserCalcolaIlTotaleCorrettamente() {
        addItem(new AddToCartRequestDTO(1L, 2, null, null, null, null)); // 2 * 99.0

        CartDTO dto = cartService.getCartDTOForUser(USER_ID);

        assertThat(dto.totalAmount()).isEqualByComparingTo("198.0");
        assertThat(dto.items()).hasSize(1);
    }

    // Su H2 una scrittura in transazione readOnly non viene rifiutata come su
    // Postgres, quindi questo test non riproduce il 500 originale - verifica
    // solo che il comportamento atteso (carrello creato al volo) resti corretto.
    @Test
    void getCartDTOForUserCreaIlCarrelloAlPrimoAccessoDiUnUtenteNuovo() {
        CartDTO dto = cartService.getCartDTOForUser("utente-mai-visto-prima");

        assertThat(dto.items()).isEmpty();
        assertThat(dto.totalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void rifiutaLaSommaDelMergeSeSuperaIlLimiteMassimo() {
        addItem(new AddToCartRequestDTO(1L, 15, null, null, null, null));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddToCartRequestDTO(1L, 10, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(cartService.getCartForUser(USER_ID).getItems().get(0).getQuantity()).isEqualTo(15);
    }
}
