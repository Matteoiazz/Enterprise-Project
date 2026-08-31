package com.tripify.itinerary_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
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
     * UUID Keycloak del proprietario (stesso formato usato ovunque nel resto del
     * sistema: hostId di Catalog, chat, ecc.). Non un oggetto User perché risiede
     * in un altro microservizio.
     */
    @Column(nullable = false)
    private String ownerId;

    /**
     * UUID Keycloak degli utenti con cui la lista è condivisa.
     */
    @ElementCollection
    @CollectionTable(name = "list_shares", joinColumns = @JoinColumn(name = "list_id"))
    @Column(name = "user_id")
    @Builder.Default
    private List<String> sharedUserIds = new java.util.ArrayList<>();

    /**
     * Componenti del catalogo (Voli, Hotel, Attività), con i dettagli necessari a
     * booking-service per aprire l'hold quando si preme "prenota tutto" (vedi
     * FavoriteListItem).
     */
    @ElementCollection
    @CollectionTable(name = "list_items", joinColumns = @JoinColumn(name = "list_id"))
    @OrderColumn(name = "item_order")
    @Builder.Default
    private List<FavoriteListItem> items = new java.util.ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    /**
     * Token opaco per il link di sola visualizzazione: dà accesso al dettaglio senza
     * login, indipendentemente dalla visibilità (vedi enableLinkSharing).
     */
    @Column(name = "public_token", unique = true)
    private String publicToken;

    /**
     * Token opaco per il link di invito: chi lo apre da loggato viene aggiunto a
     * sharedUserIds con diritto di modifica (vedi joinAsCollaborator). Distinto dal
     * link di sola visualizzazione perché concede un accesso molto più ampio.
     */
    @Column(name = "collab_token", unique = true)
    private String collabToken;

    /**
     * Città di riferimento, richiesta esplicitamente quando si pubblica la lista
     * (serve per la ricerca nel feed pubblico, non derivata dai componenti).
     */
    private String city;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private int likesCount = 0;

    /**
     * Contatore "best effort": incrementato quando qualcuno preme "prenota tutto",
     * non quando il pagamento va davvero a buon fine (itinerary-service non è
     * collegato al checkout di booking-service).
     */
    @Column(name = "bookings_count", nullable = false)
    @Builder.Default
    private int bookingsCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Non persistito: valorizzato a runtime in base a chi sta chiedendo la lista,
     * cosi' il frontend sa se mostrare il cuore pieno o vuoto senza dover tenere
     * uno stato locale separato.
     */
    @Transient
    @Builder.Default
    private boolean likedByMe = false;

    /**
     * Non persistito: prezzo reale calcolato al volo (fareClass/roomType scelti,
     * notti hotel incluse) ogni volta che la lista viene letta — vedi
     * ItineraryService.computeTotalPrice.
     */
    @Transient
    @Builder.Default
    private java.math.BigDecimal totalPrice = java.math.BigDecimal.ZERO;
}
