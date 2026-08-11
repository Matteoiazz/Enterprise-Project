package com.tripify.catalog_service.repository;

import com.tripify.catalog_service.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    // Trova un volo specificando l'aeroporto di partenza e arrivo
    List<Flight> findByDepartureAirportAndArrivalAirport(String departure, String arrival);

    // Trova voli che partono in un determinato range di date
    List<Flight> findByDepartureTimeBetween(LocalDateTime start, LocalDateTime end);
}

/* ESISTE FLIGHT REPOSITORY IN QUANTO HA DUE QUERY SPECIFICHE*/