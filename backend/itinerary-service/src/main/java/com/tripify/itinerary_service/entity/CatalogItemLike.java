package com.tripify.itinerary_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Mi piace su un singolo elemento del catalogo (volo, hotel, attività), a differenza
 * di FavoriteListLike che è il mi piace su un intero itinerario pubblico. Serve per
 * far apparire un item "salvato" singolarmente in Salvati, senza doverlo per forza
 * raggruppare in una lista.
 */
@Entity
@Table(name = "catalog_item_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"catalog_item_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogItemLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
