package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

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
}