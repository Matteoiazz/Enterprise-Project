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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    @PersistenceContext
    private EntityManager entityManager;

    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        when(catalogClient.getItem(anyLong())).thenReturn(
                new CatalogItemSummaryDTO(1L, "Activity", java.math.BigDecimal.valueOf(99.0), null, null));
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
    void clearCartAfterCheckoutNonRilasciaGliHold() {
        when(catalogClient.holdSeats(eq(7L), any())).thenReturn(new HoldResultDTO("seat-1", LocalDateTime.now().plusMinutes(15)));
        addItem(new AddToCartRequestDTO(1L, 1, null, 7L, null, null));

        cartService.clearCartAfterCheckout(USER_ID);
        entityManager.flush();
        entityManager.clear();

        verify(catalogClient, never()).releaseHold(any());
        assertThat(cartService.getCartForUser(USER_ID).getItems()).isEmpty();
    }

    @Test
    void getCartDTOForUserCalcolaIlTotaleCorrettamente() {
        addItem(new AddToCartRequestDTO(1L, 2, null, null, null, null)); // 2 * 99.0

        CartDTO dto = cartService.getCartDTOForUser(USER_ID);

        assertThat(dto.totalAmount()).isEqualByComparingTo("198.0");
        assertThat(dto.items()).hasSize(1);
    }
}
