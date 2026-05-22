package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "shopping-carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId; // Il soft link logico che punta all'utente di Dario [cite: 11, 12, 54]

    /**
     * Relazione OneToMany: Un carrello contiene molti elementi.
     * cascade = CascadeType.ALL significa che se eliminiamo il carrello,
     * in automatico si cancellano tutti i suoi elementi.
     * orphanRemoval = true serve a cancellare dal DB un CartItem se lo rimuovi dalla lista Java.
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items;
}
