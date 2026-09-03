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
    private String cardProvider;

    @Column(nullable = false, length = 4)
    private String lastFourDigits;

    @Column(nullable = false)
    private String expirationMonthYear;

    @Column(name = "is_default", nullable = false, columnDefinition = "boolean not null default false")
    private boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
