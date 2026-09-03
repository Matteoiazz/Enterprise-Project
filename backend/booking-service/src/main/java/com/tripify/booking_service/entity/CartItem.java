package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private ShoppingCart cart;

    @Column(nullable = false)
    private Long catalogItemId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtAdded;

    // Valuta di priceAtAdded, congelata al momento dell'aggiunta (ISO a 3
    // lettere, da CatalogItem.currency). Nullable per lo stesso motivo di
    // addedAt sotto; serve al frontend per non sommare articoli in valute diverse.
    private String currency;

    // Valorizzato solo per articoli hotel: id del RoomType (catalog-service)
    // scelto, diverso dall'id del CatalogItem generico sopra.
    private Long roomTypeId;

    // Valorizzato solo per articoli volo: id del FareClass (catalog-service) scelto.
    private Long fareClassId;

    // Date del soggiorno, obbligatorie solo quando roomTypeId è valorizzato.
    private LocalDate checkIn;
    private LocalDate checkOut;

    // Id del blocco disponibilità (RoomHold/SeatHold) aperto su catalog-service
    // per questo item, nel formato "room-{id}"/"seat-{id}" (vedi CatalogClient).
    // Null se l'item non richiede un hold (es. attività, che non ne ha).
    private String holdId;

    // Quando l'articolo è entrato nel carrello: usato per farlo scadere
    // automaticamente 15 minuti dopo (vedi ShoppingCartService.purgeExpiredCartItems),
    // indipendentemente dagli altri articoli già presenti o aggiunti dopo.
    // Volutamente nullable a livello di colonna nonostante il codice la valorizzi
    // sempre: con ddl-auto=update una NOT NULL su una tabella già popolata
    // farebbe fallire l'ALTER TABLE (stesso motivo di BookingLine.quantity).
    private LocalDateTime addedAt;
}