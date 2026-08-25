package com.tripify.catalog_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "catalog_items")
@Inheritance(strategy = InheritanceType.JOINED) // Magia del polimorfismo JPA
@Data
@NoArgsConstructor
@AllArgsConstructor

@SQLDelete(sql = "UPDATE catalog_items SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
public abstract class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @NotBlank(message = "il titolo è obbligatorio")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "il prezzo è obbligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "il prezzo deve essere maggiore di zero")
    @Column(nullable = false)
    private BigDecimal price;

    @NotBlank(message = "la valuta è obbligatoria")
    @Size(min = 3, max = 3, message = "la valuta deve essere un codice ISO a 3 lettere (es. EUR)")
    @Column(length = 3, nullable = false)
    private String currency; // es. "EUR"

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // false per gli item di data.sql (nessun organizzatore reale dietro), true per quelli
    // creati davvero da un Organizzatore autenticato: serve a decidere se mostrare "Chatta
    // con l'organizzatore" (altrimenti si aprirebbe una chat con un host inesistente).
    @Column(name = "is_user_generated", nullable = false, columnDefinition = "boolean default false")
    private boolean isUserGenerated = false;

    @Column(length = 50)
    private String category;

    @Min(value = 1, message = "il rating minimo è 1")
    @Max(value = 5, message = "il rating massimo è 5")
    @Column
    private Integer rating; // Da 1 a 5


    @OneToMany(mappedBy = "catalogItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CatalogImage> images = new ArrayList<>();

    public void addImage(CatalogImage image) {
        images.add(image);
        image.setCatalogItem(this);
    }
}
