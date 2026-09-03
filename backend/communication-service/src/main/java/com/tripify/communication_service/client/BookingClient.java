package com.tripify.communication_service.client;

import com.tripify.communication_service.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booking-service", url = "${booking.service.url:http://localhost:8083}", configuration = FeignClientConfig.class)
public interface BookingClient {

    @GetMapping("/api/v1/bookings/catalog/{catalogItemId}/has-booked")
    boolean hasUserBookedItem(@PathVariable("catalogItemId") Long catalogItemId);
}