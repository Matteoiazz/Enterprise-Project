package com.tripify.catalog_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalog_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500) // length 500 utile per URL molto lunghi
    private String imageUrl;

    // Relazione Inversa verso il viaggio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private CatalogItem catalogItem;
}