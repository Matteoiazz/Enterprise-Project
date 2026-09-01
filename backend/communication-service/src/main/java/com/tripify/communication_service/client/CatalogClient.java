package com.tripify.communication_service.client;

import com.tripify.communication_service.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "catalog-service", url = "${catalog.service.url:http://localhost:8082}", configuration = FeignClientConfig.class)
public interface CatalogClient {

    // Chiamata di servizio, non di un utente: la chiave sostituisce il JWT (che catalog-service
    // qui non richiede, vedi CatalogController.updateRating/SecurityConfig).
    @PutMapping("/api/v1/catalog/items/{id}/rating")
    void updateRating(@PathVariable("id") Long id,
                       @RequestHeader("X-Internal-Key") String internalKey,
                       @RequestBody RatingUpdate body);

    record RatingUpdate(Double average, Integer count) {}
}
