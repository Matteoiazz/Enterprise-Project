package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart-items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class cartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private shoppingCart cart;

    @Column(nullable = false)
    private Long catalogItemId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double price;
}
