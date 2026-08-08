package com.tripify.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "activity_details")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Activity extends CatalogItem {

    @Column(name = "activity_type", nullable = false)
    private String activityType; // Es: "Museo", "Tour Gastronomico", "Sport Estremi"

    @Column(nullable = false)
    private String duration; // Es: "2 ore", "Giornata intera"

    @Column(name = "meeting_point")
    private String meetingPoint; // Indirizzo di ritrovo per l'attività

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "guide_included", nullable = false)
    private boolean guideIncluded;
}