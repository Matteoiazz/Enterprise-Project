package com.tripify.itinerary_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "favorite_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Esempio: "Weekend a Londra"

    /**
     * ID dell'utente proprietario.
     * Non usiamo l'oggetto User perché risiede nel db_users. [cite: 11, 53]
     */
    @Column(nullable = false)
    private Long ownerId;

    /**
     * Lista di ID utenti per la condivisione. [cite: 32]
     */
    @ElementCollection
    @CollectionTable(name = "list_shares", joinColumns = @JoinColumn(name = "list_id"))
    private List<Long> sharedUserIds;

    /**
     * ID degli elementi del catalogo (Voli, Hotel, Attività). [cite: 27]
     */
    @ElementCollection
    @CollectionTable(name = "list_items", joinColumns = @JoinColumn(name = "list_id"))
    private List<Long> catalogItemIds;
}
