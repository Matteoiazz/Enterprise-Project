package com.tripify.user_auth_service.entity;

import lombok.*;
import jakarta.persistence.*;
import java.util.UUID;


@Entity
@Table(name = "payment_methods", indexes = @Index(name = "idx_payment_methods_user", columnList = "user_id"))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String cardProvider; // Es. "VISA", "MASTERCARD"

    @Column(nullable = false, length = 4)
    private String lastFourDigits; // Salviamo SOLO le ultime 4 cifre per sicurezza

    @Column(nullable = false)
    private String expirationMonthYear; // Es. "12/28"

    @Column(name = "is_default", nullable = false, columnDefinition = "boolean not null default false")
    private boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}