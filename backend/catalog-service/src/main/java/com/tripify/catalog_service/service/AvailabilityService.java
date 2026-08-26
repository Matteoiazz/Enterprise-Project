package com.tripify.catalog_service.service;

import com.tripify.catalog_service.dto.HoldResultDTO;

import java.time.LocalDate;

public interface AvailabilityService {

    HoldResultDTO holdRoom(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms, String userId);

    HoldResultDTO holdSeats(Long fareClassId, int seats, String userId);

    void confirm(String holdId, String userId);

    void release(String holdId, String userId);

    int computeRoomAvailability(Long roomTypeId, LocalDate checkIn, LocalDate checkOut);

    int computeSeatAvailability(Long fareClassId);
}
