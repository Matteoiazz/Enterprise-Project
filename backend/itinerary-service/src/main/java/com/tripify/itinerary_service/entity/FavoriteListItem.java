package com.tripify.itinerary_service.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.*;

import java.math.BigDecimal;
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

    /** Valorizzato solo per le Attività: il giorno del viaggio in cui si svolge. */
    private LocalDate activityDate;

    /**
     * Non persistito: prezzo reale di QUESTO componente (tariffa/camera×notti scelta),
     * valorizzato da ItineraryService.computeTotalPrice insieme al totale della lista.
     * Serve al frontend per mostrare un prezzo per tappa senza duplicare la logica di
     * calcolo (fareClass/roomType) lato client.
     */
    @Transient
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;
}
