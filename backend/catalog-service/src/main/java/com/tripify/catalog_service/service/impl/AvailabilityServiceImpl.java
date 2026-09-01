package com.tripify.catalog_service.service.impl;

import com.tripify.catalog_service.dto.HoldResultDTO;
import com.tripify.catalog_service.entity.*;
import com.tripify.catalog_service.exception.CatalogItemNotFoundException;
import com.tripify.catalog_service.exception.HoldNotFoundException;
import com.tripify.catalog_service.exception.InsufficientAvailabilityException;
import com.tripify.catalog_service.exception.InvalidHoldStateException;
import com.tripify.catalog_service.repository.FareClassRepository;
import com.tripify.catalog_service.repository.RoomHoldRepository;
import com.tripify.catalog_service.repository.RoomTypeRepository;
import com.tripify.catalog_service.repository.SeatHoldRepository;
import com.tripify.catalog_service.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(15);
    private static final String ROOM_PREFIX = "room-";
    private static final String SEAT_PREFIX = "seat-";

    private final RoomTypeRepository roomTypeRepository;
    private final FareClassRepository fareClassRepository;
    private final RoomHoldRepository roomHoldRepository;
    private final SeatHoldRepository seatHoldRepository;

    @Override
    @Transactional
    public HoldResultDTO holdRoom(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms, String userId) {
        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("checkIn deve essere una data precedente a checkOut");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("checkIn non può essere nel passato");
        }
        if (rooms < 1) {
            throw new IllegalArgumentException("il numero di camere richieste deve essere almeno 1");
        }

        RoomType roomType = roomTypeRepository.findByIdForUpdate(roomTypeId)
                .orElseThrow(() -> new CatalogItemNotFoundException(roomTypeId));

        LocalDateTime now = LocalDateTime.now();
        int available = computeRoomAvailability(roomType, checkIn, checkOut, now);
        if (available < rooms) {
            throw new InsufficientAvailabilityException(
                    "Disponibilità insufficiente per \"" + roomType.getName() + "\" dal " + checkIn + " al " + checkOut);
        }

        RoomHold hold = RoomHold.builder()
                .roomType(roomType)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .rooms(rooms)
                .userId(userId)
                .status(HoldStatus.HELD)
                .createdAt(now)
                .expiresAt(now.plus(HOLD_DURATION))
                .build();
        hold = roomHoldRepository.save(hold);

        return new HoldResultDTO(ROOM_PREFIX + hold.getId(), hold.getExpiresAt());
    }

    @Override
    @Transactional
    public HoldResultDTO holdSeats(Long fareClassId, int seats, String userId) {
        if (seats < 1) {
            throw new IllegalArgumentException("il numero di posti richiesti deve essere almeno 1");
        }

        FareClass fareClass = fareClassRepository.findByIdForUpdate(fareClassId)
                .orElseThrow(() -> new CatalogItemNotFoundException(fareClassId));

        LocalDateTime now = LocalDateTime.now();
        int available = computeSeatAvailability(fareClass, now);
        if (available < seats) {
            throw new InsufficientAvailabilityException("Disponibilità insufficiente per la classe \"" + fareClass.getName() + "\"");
        }

        SeatHold hold = SeatHold.builder()
                .fareClass(fareClass)
                .seats(seats)
                .userId(userId)
                .status(HoldStatus.HELD)
                .createdAt(now)
                .expiresAt(now.plus(HOLD_DURATION))
                .build();
        hold = seatHoldRepository.save(hold);

        return new HoldResultDTO(SEAT_PREFIX + hold.getId(), hold.getExpiresAt());
    }

    @Override
    @Transactional
    public void confirm(String holdId, String userId) {
        if (holdId.startsWith(ROOM_PREFIX)) {
            RoomHold hold = roomHoldRepository.findByIdForUpdate(parseId(holdId, ROOM_PREFIX))
                    .orElseThrow(() -> new HoldNotFoundException(holdId));
            requireOwner(hold.getUserId(), userId, holdId);
            hold.setStatus(nextStatusOnConfirm(hold.getStatus(), hold.getExpiresAt(), holdId));
            roomHoldRepository.save(hold);
        } else if (holdId.startsWith(SEAT_PREFIX)) {
            SeatHold hold = seatHoldRepository.findByIdForUpdate(parseId(holdId, SEAT_PREFIX))
                    .orElseThrow(() -> new HoldNotFoundException(holdId));
            requireOwner(hold.getUserId(), userId, holdId);
            hold.setStatus(nextStatusOnConfirm(hold.getStatus(), hold.getExpiresAt(), holdId));
            seatHoldRepository.save(hold);
        } else {
            throw new HoldNotFoundException(holdId);
        }
    }

    @Override
    @Transactional
    public void release(String holdId, String userId) {
        if (holdId.startsWith(ROOM_PREFIX)) {
            RoomHold hold = roomHoldRepository.findByIdForUpdate(parseId(holdId, ROOM_PREFIX))
                    .orElseThrow(() -> new HoldNotFoundException(holdId));
            requireOwner(hold.getUserId(), userId, holdId);
            hold.setStatus(nextStatusOnRelease(hold.getStatus(), holdId));
            roomHoldRepository.save(hold);
        } else if (holdId.startsWith(SEAT_PREFIX)) {
            SeatHold hold = seatHoldRepository.findByIdForUpdate(parseId(holdId, SEAT_PREFIX))
                    .orElseThrow(() -> new HoldNotFoundException(holdId));
            requireOwner(hold.getUserId(), userId, holdId);
            hold.setStatus(nextStatusOnRelease(hold.getStatus(), holdId));
            seatHoldRepository.save(hold);
        } else {
            throw new HoldNotFoundException(holdId);
        }
    }

    @Override
    @Transactional
    public void compensate(String holdId) {
        if (holdId.startsWith(ROOM_PREFIX)) {
            RoomHold hold = roomHoldRepository.findByIdForUpdate(parseId(holdId, ROOM_PREFIX))
                    .orElseThrow(() -> new HoldNotFoundException(holdId));
            if (hold.getStatus() != HoldStatus.RELEASED) {
                hold.setStatus(HoldStatus.RELEASED);
                roomHoldRepository.save(hold);
            }
        } else if (holdId.startsWith(SEAT_PREFIX)) {
            SeatHold hold = seatHoldRepository.findByIdForUpdate(parseId(holdId, SEAT_PREFIX))
                    .orElseThrow(() -> new HoldNotFoundException(holdId));
            if (hold.getStatus() != HoldStatus.RELEASED) {
                hold.setStatus(HoldStatus.RELEASED);
                seatHoldRepository.save(hold);
            }
        } else {
            throw new HoldNotFoundException(holdId);
        }
    }

    // Non riveliamo se l'hold esiste a chi non ne è il proprietario: stesso
    // errore "non trovato" usato per un id inesistente.
    private void requireOwner(String ownerId, String requesterId, String holdId) {
        if (!ownerId.equals(requesterId)) {
            throw new HoldNotFoundException(holdId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int computeRoomAvailability(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new CatalogItemNotFoundException(roomTypeId));
        return computeRoomAvailability(roomType, checkIn, checkOut, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public int computeSeatAvailability(Long fareClassId) {
        FareClass fareClass = fareClassRepository.findById(fareClassId)
                .orElseThrow(() -> new CatalogItemNotFoundException(fareClassId));
        return computeSeatAvailability(fareClass, LocalDateTime.now());
    }

    // --- interno ---

    private int computeRoomAvailability(RoomType roomType, LocalDate checkIn, LocalDate checkOut, LocalDateTime now) {
        List<RoomHold> overlapping = roomHoldRepository.findActiveOverlapping(roomType.getId(), checkIn, checkOut, now);
        int minAvailable = roomType.getTotalRooms();
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            final LocalDate current = night;
            int used = overlapping.stream()
                    .filter(h -> h.coversNight(current))
                    .mapToInt(RoomHold::getRooms)
                    .sum();
            minAvailable = Math.min(minAvailable, roomType.getTotalRooms() - used);
        }
        return minAvailable;
    }

    private int computeSeatAvailability(FareClass fareClass, LocalDateTime now) {
        Integer used = seatHoldRepository.sumActiveSeats(fareClass.getId(), now);
        return fareClass.getTotalSeats() - (used == null ? 0 : used);
    }

    private Long parseId(String holdId, String prefix) {
        try {
            return Long.parseLong(holdId.substring(prefix.length()));
        } catch (NumberFormatException e) {
            throw new HoldNotFoundException(holdId);
        }
    }

    private HoldStatus nextStatusOnConfirm(HoldStatus current, LocalDateTime expiresAt, String holdId) {
        if (current == HoldStatus.HELD && expiresAt.isBefore(LocalDateTime.now())) {
            throw new InvalidHoldStateException("Il blocco " + holdId + " è scaduto, non può più essere confermato");
        }
        if (current != HoldStatus.HELD) {
            throw new InvalidHoldStateException("Il blocco " + holdId + " non è in attesa di conferma (stato attuale: " + current + ")");
        }
        return HoldStatus.CONFIRMED;
    }

    private HoldStatus nextStatusOnRelease(HoldStatus current, String holdId) {
        if (current == HoldStatus.CONFIRMED) {
            throw new InvalidHoldStateException("Il blocco " + holdId + " è già confermato, non può essere rilasciato");
        }
        return HoldStatus.RELEASED;
    }
}
