package com.tripify.booking_service.dto;

import com.tripify.booking_service.entity.BookingStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponseDTO {
    private Long id;
    private Double totalAmount;
    private LocalDateTime bookingDate;
    private BookingStatus status;

    // IL CAMPO MAGICO PER IL FRONTEND
    private boolean isLeader;
}