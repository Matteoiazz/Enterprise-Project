package com.tripify.communication_service.client;

import com.tripify.communication_service.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "catalog-service", url = "${catalog.service.url:http://localhost:8082}", configuration = FeignClientConfig.class)
public interface CatalogClient {

    @PutMapping("/api/v1/catalog/items/{id}/rating")
    void updateRating(@PathVariable("id") Long id, @RequestBody RatingUpdate body);

    record RatingUpdate(Double average, Integer count) {}
}
