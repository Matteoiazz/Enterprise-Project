package com.tripify.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "booking_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class BookingLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private Long catalogItemId;

    @Column(nullable = false)
    private Double price;

    @OneToMany(mappedBy = "bookingLine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Passenger> passengers;
}
