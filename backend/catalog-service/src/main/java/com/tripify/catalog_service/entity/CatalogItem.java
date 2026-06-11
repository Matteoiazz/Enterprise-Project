package com.tripify.catalog_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "catalog_items")
@Inheritance(strategy = InheritanceType.JOINED) // Magia del polimorfismo JPA
@Data
@NoArgsConstructor
@AllArgsConstructor
// --- IMPLEMENTAZIONE SOFT DELETE ---
// Sovrascrive il comando "DELETE" con un "UPDATE"
@SQLDelete(sql = "UPDATE catalog_items SET is_active = false WHERE id = ?")
// Hibernate 6 (Spring Boot 3): Applica in automatico questo filtro a TUTTE le query SELECT!
@SQLRestriction("is_active = true")
public abstract class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // VINCOLO MICROSERVIZI: Nessuna Foreign Key verso la tabella Users di Dario!
    // È solo un ID che l'API Gateway o il Frontend si occuperanno di risolvere.
    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(length = 3, nullable = false)
    private String currency; // es. "EUR"

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(length = 50)
    private String category;

    @Column
    private Integer rating; // Da 1 a 5
}
