package com.tripify.itinerary_service.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDate;

/**
 * Un componente di una FavoriteList. roomTypeId/fareClassId/checkIn/checkOut sono
 * valorizzati solo quando il componente è rispettivamente una camera d'hotel o un
 * posto su un volo: servono a booking-service per aprire l'hold corretto quando si
 * preme "prenota tutto" (vedi ItineraryService.bookAllItems).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteListItem {

    private Long catalogItemId;

    @Builder.Default
    private Integer quantity = 1;

    private Long roomTypeId;
    private Long fareClassId;
    private LocalDate checkIn;
    private LocalDate checkOut;
}
