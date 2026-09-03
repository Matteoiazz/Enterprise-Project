package com.tripify.itinerary_service.client;

import com.tripify.itinerary_service.dto.CatalogItemSummaryDTO;
import com.tripify.itinerary_service.dto.CatalogSearchPageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "catalog-service", url = "${catalog-service.url}")
public interface CatalogClient {

    @GetMapping("/items/{itemId}")
    CatalogItemSummaryDTO getItem(@PathVariable("itemId") Long itemId);

    /**
     * Usata dalla generazione automatica di un itinerario: "destination" su
     * category=Tutti restituisce in un'unica chiamata sia i voli con arrivo in
     * quella città sia gli hotel/attività che vi si trovano (vedi
     * CatalogItemSpecification.withDynamicFilters), evitando una ricerca per tipo.
     */
    @GetMapping("/items/search")
    CatalogSearchPageDTO searchByDestination(@RequestParam("destination") String destination,
                                              @RequestParam("size") int size);
}
