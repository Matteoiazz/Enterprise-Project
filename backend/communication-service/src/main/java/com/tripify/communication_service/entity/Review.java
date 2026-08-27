package com.tripify.communication_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 1000)
    private String comment;

    @Column(name = "traveler_id", nullable = false)
    private String travelerId; // <-- Cambiato in String

    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;
}