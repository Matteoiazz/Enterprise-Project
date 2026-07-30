package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId; // Questo è il LEADER (chi paga/crea il viaggio)

    // LA NOVITÀ: Lista degli User ID degli amici invitati
    @ElementCollection
    @CollectionTable(name = "booking_participants", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "participant_id")
    private Set<String> participantIds = new HashSet<>();

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private LocalDateTime bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingLine> lines;

    private LocalDateTime updatedAt;
}