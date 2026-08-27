package com.tripify.communication_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booking-service", url = "${booking.service.url:http://localhost:8082}")
public interface BookingClient {

    @GetMapping("/api/v1/bookings/catalog/{catalogItemId}/has-booked")
    boolean hasUserBookedItem(@PathVariable("catalogItemId") Long catalogItemId);
}