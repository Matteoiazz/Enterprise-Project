package it.unical.webapp.enterpriseback.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "hotel_details")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Hotel extends CatalogItem {

    // Coordinate per la mappa Android!
    @Column(name = "location_lat", nullable = false)
    private Double locationLat;

    @Column(name = "location_lng", nullable = false)
    private Double locationLng;

    @Column(name = "room_type", nullable = false)
    private String roomType; // Es: "Single", "Double", "Suite"

    @Column(name = "available_rooms", nullable = false)
    private Integer availableRooms;
}