package com.tripify.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "activity_details")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Activity extends CatalogItem {

    @NotBlank(message = "il tipo di attività è obbligatorio")
    @Column(name = "activity_type", nullable = false)
    private String activityType;

    @NotBlank(message = "la durata è obbligatoria")
    @Column(nullable = false)
    private String duration;

    @Column(name = "meeting_point")
    private String meetingPoint;

    @NotBlank(message = "la città è obbligatoria")
    @Column(name = "city", nullable = false)
    private String city;

    @Min(value = 1, message = "i partecipanti massimi devono essere almeno 1")
    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "guide_included", nullable = false)
    private boolean guideIncluded;
}