package com.tripify.catalog_service.service;

import com.tripify.catalog_service.entity.HoldStatus;
import com.tripify.catalog_service.repository.RoomHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Rimuove periodicamente gli hold di stanza non confermati (HELD/RELEASED) il cui
 * check-out è ormai passato. A differenza dei voli, un hotel resta prenotabile per
 * sempre (nuove date future): qui non si cancella mai l'hotel o la RoomType, solo
 * gli hold ormai storici che altrimenti si accumulerebbero senza limite (vedi anche
 * FlightCleanupService per l'analogo sui voli).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomHoldCleanupService {

    private final RoomHoldRepository roomHoldRepository;

    @Scheduled(fixedRate = 60 * 60 * 1000, initialDelay = 90 * 1000)
    @Transactional
    public void removeStaleRoomHolds() {
        int removed = roomHoldRepository.deleteByCheckOutBeforeAndStatusNot(LocalDate.now(), HoldStatus.CONFIRMED);
        if (removed > 0) {
            log.info("Rimossi {} hold di stanza scaduti", removed);
        }
    }
}
