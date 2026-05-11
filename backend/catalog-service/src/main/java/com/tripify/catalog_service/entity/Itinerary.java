package com.tripify.catalog_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "itineraries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // Soft Link: ID dell'utente che ha creato la lista o il pacchetto
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    // Distingue tra Pacchetto Host (vendibile) e Lista Viaggiatore (preferiti)
    @Column(name = "is_commercial_package")
    private boolean isCommercialPackage = false;

    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.PRIVATE;

    // Il prezzo del pacchetto intero (se è commerciale)
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    /* VINCOLO MICROSERVIZI: Non usiamo @ManyToMany reale con CatalogItem
       perché Itinerary potrebbe essere spostato in un altro microservizio.
       Salviamo solo la lista degli ID.
    */
    @ElementCollection
    @CollectionTable(name = "itinerary_items", joinColumns = @JoinColumn(name = "itinerary_id"))
    @Column(name = "catalog_item_id")
    private List<Long> catalogItemIds;
}