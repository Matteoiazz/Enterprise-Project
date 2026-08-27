package com.tripify.booking_service.service;

import com.tripify.booking_service.client.CatalogClient;
import com.tripify.booking_service.dto.AddToCartRequestDTO;
import com.tripify.booking_service.dto.CartDTO;
import com.tripify.booking_service.dto.CartItemDTO;
import com.tripify.booking_service.dto.CatalogItemSummaryDTO;
import com.tripify.booking_service.dto.HoldResultDTO;
import com.tripify.booking_service.dto.RoomHoldRequestDTO;
import com.tripify.booking_service.dto.SeatHoldRequestDTO;
import com.tripify.booking_service.entity.CartItem;
import com.tripify.booking_service.entity.ShoppingCart;
import com.tripify.booking_service.exception.CatalogItemNotFoundException;
import com.tripify.booking_service.exception.ResourceNotFoundException;
import com.tripify.booking_service.repository.CartItemRepository;
import com.tripify.booking_service.repository.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingCartService {

    // Ogni articolo scade 15 minuti dopo essere ENTRATO nel carrello (addedAt),
    // indipendentemente dagli altri: se A è nel carrello da 10 minuti e si
    // aggiunge B, tra 5 minuti scade solo A mentre a B restano ancora 10 minuti.
    private static final long CART_ITEM_TTL_MINUTES = 15;

    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository itemRepository;

    // INIETTIAMO IL CLIENT FEIGN PER PARLARE COL CATALOGO
    private final CatalogClient catalogClient;

    // 1. Recupera il carrello (entità) di un utente. Uso interno di altri
    // service (es. BookingService.checkout) che leggono cart.getItems()
    // già dentro una propria transazione: per esporlo via API vedi
    // getCartDTOForUser, che costruisce il DTO senza far uscire collection
    // lazy fuori dal confine transazionale (open-in-view è disattivato).
    public ShoppingCart getCartForUser(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ShoppingCart newCart = ShoppingCart.builder()
                            .userId(userId)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    // 1bis. Versione esposta via API: costruisce il DTO dentro la stessa
    // transazione di lettura, così cart.getItems() (LAZY) viene inizializzata
    // qui e non fallisce in fase di serializzazione JSON nel controller.
    @Transactional(readOnly = true)
    public CartDTO getCartDTOForUser(String userId) {
        ShoppingCart cart = getCartForUser(userId);

        List<CartItemDTO> items = cart.getItems().stream()
                .map(item -> new CartItemDTO(
                        item.getId(),
                        item.getCatalogItemId(),
                        item.getQuantity(),
                        item.getPriceAtAdded(),
                        item.getRoomTypeId(),
                        item.getFareClassId(),
                        item.getCheckIn(),
                        item.getCheckOut()))
                .collect(Collectors.toList());

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getPriceAtAdded().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDTO(cart.getId(), items, total);
    }

    // 2. Aggiunge un elemento (NON CHIEDIAMO PIÙ IL PREZZO AD ANDROID!)
    // Se request.roomTypeId()/fareClassId() è valorizzato, blocca subito la
    // disponibilità su catalog-service (hold temporaneo) prima di salvare
    // la riga: evita che due utenti prenotino più camere/posti di quelli
    // realmente disponibili nella finestra tra "aggiungi al carrello" e
    // "checkout" (vedi anche confirmPayment/cancelBooking in BookingService).
    @Transactional
    public void addItem(String userId, AddToCartRequestDTO request) {
        if (request.roomTypeId() != null && request.fareClassId() != null) {
            throw new IllegalArgumentException("Non è possibile specificare sia roomTypeId che fareClassId per lo stesso articolo.");
        }

        ShoppingCart cart = getCartForUser(userId);

        // LA CHIAMATA DI SICUREZZA: chiediamo l'articolo completo al microservizio
        // Catalogo (non solo il prezzo base) per poter usare il prezzo della
        // tariffa/camera realmente scelta, e per gli hotel moltiplicarlo per le notti.
        CatalogItemSummaryDTO catalogItem = catalogClient.getItem(request.catalogItemId());

        if (catalogItem == null) {
            throw new CatalogItemNotFoundException("Articolo non trovato nel catalogo: " + request.catalogItemId());
        }

        if (request.roomTypeId() != null && (request.checkIn() == null || request.checkOut() == null)) {
            throw new IllegalArgumentException("checkIn e checkOut sono obbligatori per prenotare una camera d'hotel.");
        }

        BigDecimal price = resolveRealPrice(catalogItem, request);
        String holdId = null;

        if (request.roomTypeId() != null) {
            HoldResultDTO hold = catalogClient.holdRoom(request.roomTypeId(),
                    new RoomHoldRequestDTO(request.checkIn(), request.checkOut(), request.quantity(), userId));
            holdId = hold.holdId();
        } else if (request.fareClassId() != null) {
            HoldResultDTO hold = catalogClient.holdSeats(request.fareClassId(),
                    new SeatHoldRequestDTO(request.quantity(), userId));
            holdId = hold.holdId();
        }

        // Gli item con un hold aperto (camera/posto) non vengono mai uniti a uno
        // esistente: ogni hold su catalog-service ha una propria quantity fissa
        // e non esiste un endpoint per "aumentarla", quindi ogni aggiunta con
        // roomTypeId/fareClassId diventa una nuova riga con un nuovo hold.
        if (holdId == null) {
            Optional<CartItem> existingItem = cart.getItems().stream()
                    .filter(item -> item.getCatalogItemId().equals(request.catalogItemId())
                            && item.getRoomTypeId() == null && item.getFareClassId() == null)
                    .findFirst();

            if (existingItem.isPresent()) {
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + request.quantity());
                itemRepository.save(item);
                return;
            }
        }

        CartItem newItem = CartItem.builder()
                .cart(cart)
                .catalogItemId(request.catalogItemId())
                .quantity(request.quantity())
                .priceAtAdded(price)
                .roomTypeId(request.roomTypeId())
                .fareClassId(request.fareClassId())
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .holdId(holdId)
                .addedAt(LocalDateTime.now())
                .build();
        itemRepository.save(newItem);
    }

    /**
     * Prezzo reale dell'articolo da salvare in priceAtAdded: tariffa scelta per i
     * voli, prezzo camera × numero di notti per gli hotel, prezzo base altrimenti.
     * Prima di questo fix veniva sempre usato il prezzo base dell'item, ignorando
     * la tariffa/camera scelta e senza mai moltiplicare per le notti.
     */
    private BigDecimal resolveRealPrice(CatalogItemSummaryDTO catalogItem, AddToCartRequestDTO request) {
        BigDecimal basePrice = catalogItem.price() != null ? catalogItem.price() : BigDecimal.ZERO;

        if (request.fareClassId() != null) {
            return catalogItem.fareClasses() == null ? basePrice : catalogItem.fareClasses().stream()
                    .filter(f -> f.id().equals(request.fareClassId()))
                    .map(CatalogItemSummaryDTO.FareClassSummaryDTO::price)
                    .findFirst().orElse(basePrice);
        }

        if (request.roomTypeId() != null) {
            BigDecimal roomPrice = catalogItem.roomTypes() == null ? basePrice : catalogItem.roomTypes().stream()
                    .filter(r -> r.id().equals(request.roomTypeId()))
                    .map(CatalogItemSummaryDTO.RoomTypeSummaryDTO::price)
                    .findFirst().orElse(basePrice);
            long nights = Math.max(1, ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()));
            return roomPrice.multiply(BigDecimal.valueOf(nights));
        }

        return basePrice;
    }

    // 3. Svuota completamente il carrello su richiesta esplicita dell'utente:
    // rilascia prima ogni hold ancora aperto su catalog-service, altrimenti
    // le camere/i posti bloccati resterebbero indisponibili fino alla scadenza
    // naturale del hold (15 minuti) anche se l'utente ha cambiato idea subito.
    @Transactional
    public void clearCart(String userId) {
        ShoppingCart cart = getCartForUser(userId);
        releaseHolds(cart);
        // Svuotiamo la collection invece di usare itemRepository.deleteByCartId:
        // releaseHolds() sopra ha già forzato il caricamento di cart.getItems(),
        // e con orphanRemoval=true è quella la via che Hibernate si aspetta per
        // cancellare i figli di una collection già inizializzata in questa sessione
        // - una delete diretta via repository su entità già presenti nella
        // collection del padre rischia di essere ignorata al flush.
        cart.getItems().clear();
    }

    // 3bis. Usata dal checkout: rimuove solo gli articoli effettivamente
    // acquistati (non necessariamente tutto il carrello, vedi BookingService.checkout
    // con selezione parziale) - i loro hold non vanno rilasciati qui, perché
    // sono stati appena trasferiti alle nuove BookingLine (che li confermeranno
    // o rilasceranno a seconda dell'esito del pagamento).
    @Transactional
    public void removeCheckedOutItems(String userId, List<Long> cartItemIds) {
        ShoppingCart cart = getCartForUser(userId);
        cart.getItems().removeIf(item -> cartItemIds.contains(item.getId()));
    }

    // 4. Rimuove un singolo articolo dal carrello su richiesta esplicita
    // dell'utente (a differenza di clearCart, qui rilasciamo l'hold solo di
    // QUELL'articolo, non di tutto il carrello).
    @Transactional
    public void removeItem(String userId, Long cartItemId) {
        ShoppingCart cart = getCartForUser(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Articolo non trovato nel carrello."));

        releaseHoldIfPresent(item);
        cart.getItems().remove(item);
    }

    // 5. Job schedulato: rimuove gli articoli il cui hold di 15 minuti è
    // scaduto. Va diretto su itemRepository (non passa dall'aggregato
    // ShoppingCart) perché in un colpo solo può toccare i carrelli di utenti
    // diversi, non ha senso caricarli tutti solo per questo.
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void purgeExpiredCartItems() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CART_ITEM_TTL_MINUTES);
        List<CartItem> expired = itemRepository.findByAddedAtBefore(threshold);

        for (CartItem item : expired) {
            releaseHoldIfPresent(item);
            itemRepository.delete(item);
        }

        if (!expired.isEmpty()) {
            log.info("Rimossi {} articoli dal carrello per scadenza dei {} minuti", expired.size(), CART_ITEM_TTL_MINUTES);
        }
    }

    private void releaseHoldIfPresent(CartItem item) {
        if (item.getHoldId() != null) {
            try {
                catalogClient.releaseHold(item.getHoldId());
            } catch (RuntimeException ex) {
                // Non blocchiamo la rimozione per un hold che magari è già
                // scaduto/rilasciato da solo lato catalog-service: logghiamo
                // e proseguiamo, il peggio che succede è che scadrà da solo.
                log.warn("Impossibile rilasciare il blocco {} su catalog-service: {}", item.getHoldId(), ex.getMessage());
            }
        }
    }

    private void releaseHolds(ShoppingCart cart) {
        for (CartItem item : cart.getItems()) {
            releaseHoldIfPresent(item);
        }
    }
}
