package com.tripify.booking_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Sostituisci l'URL con quello reale del servizio catalogo dei tuoi colleghi
@FeignClient(name = "catalog-service", url = "http://localhost:8081/api/v1/catalog")
public interface CatalogClient {

    // Questo chiama l'endpoint del catalogo!
    @GetMapping("/{itemId}/price")
    Double getItemPrice(@PathVariable("itemId") Long itemId);
}