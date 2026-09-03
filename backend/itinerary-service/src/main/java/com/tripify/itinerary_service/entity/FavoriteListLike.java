package com.tripify.itinerary_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "favorite_list_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"list_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteListLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_id", nullable = false)
    private Long listId;

    @Column(name = "user_id", nullable = false)
    private String userId;
}
