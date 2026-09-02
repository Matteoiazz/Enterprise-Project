package com.tripify.communication_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "review_votes",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_votes_review_voter", columnNames = {"review_id", "voter_id"}),
        indexes = @Index(name = "idx_review_votes_review", columnList = "review_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "voter_id", nullable = false)
    private String voterId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
