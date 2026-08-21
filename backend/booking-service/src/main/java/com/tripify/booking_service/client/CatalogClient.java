package com.tripify.booking_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// L'URL non è più scritto qui a mano: viene letto da application.properties
// (chiave catalog-service.url), così basta cambiare quella riga quando si passa
// a Docker, senza toccare questo file.
@FeignClient(name = "catalog-service", url = "${catalog-service.url}")
public interface CatalogClient {

    // Questo chiama l'endpoint del catalogo!
    @GetMapping("/{itemId}/price")
    Double getItemPrice(@PathVariable("itemId") Long itemId);
}