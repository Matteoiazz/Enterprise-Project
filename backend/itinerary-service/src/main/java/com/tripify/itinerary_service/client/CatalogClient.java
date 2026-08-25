package com.tripify.itinerary_service.client;

import com.tripify.itinerary_service.dto.CatalogItemSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service", url = "${catalog-service.url}")
public interface CatalogClient {

    @GetMapping("/items/{itemId}")
    CatalogItemSummaryDTO getItem(@PathVariable("itemId") Long itemId);
}
