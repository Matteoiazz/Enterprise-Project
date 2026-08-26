package com.tripify.catalog_service.service;

import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.repository.FlightRepository;
import com.tripify.catalog_service.repository.SeatHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rimuove dal catalogo i voli il cui orario di partenza è già passato: un volo
 * scaduto non è più prenotabile e non ha senso lasciarlo visibile in ricerca.
 * Necessario perché catalog-service resta in esecuzione per giorni tra un riavvio
 * e l'altro (i voli ricorrenti generati da data.sql coprono una finestra di 60
 * giorni calcolata UNA volta all'avvio, quindi senza questa pulizia periodica i
 * primi giorni della finestra diventerebbero "voli fantasma" col passare del tempo).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlightCleanupService {

    private final FlightRepository flightRepository;
    private final SeatHoldRepository seatHoldRepository;

    @Scheduled(fixedRate = 60 * 60 * 1000, initialDelay = 60 * 1000)
    @Transactional
    public void removeExpiredFlights() {
        LocalDateTime now = LocalDateTime.now();

        // Gli hold (anche scaduti/confermati) sui voli in questione vanno rimossi
        // prima delle fare_classes, altrimenti la FK seat_holds -> fare_classes blocca la delete.
        seatHoldRepository.deleteByFareClass_Flight_DepartureTimeBefore(now);

        List<Flight> expired = flightRepository.findByDepartureTimeBefore(now);
        if (!expired.isEmpty()) {
            flightRepository.deleteAll(expired);
            log.info("Rimossi {} voli scaduti dal catalogo", expired.size());
        }
    }
}
