package com.tripify.communication_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Voto da 1 a 5 stelle
    @Column(nullable = false)
    private Integer rating;

    // Testo obbligatorio come da specifiche
    @Column(nullable = false, length = 1000)
    private String comment;

    // SOFT LINK: ID del Viaggiatore che scrive la recensione (User Service)
    @Column(name = "traveler_id", nullable = false)
    private Long travelerId;

    // SOFT LINK: ID dell'elemento recensito (Catalog Service)
    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;
}
