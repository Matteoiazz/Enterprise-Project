package com.tripify.catalog_service.service;

import com.tripify.catalog_service.entity.CatalogItem;
import java.util.List;

public interface CatalogService {

    // Trova tutti gli elementi nel catalogo (voli, hotel, attività)
    List<CatalogItem> getAllItems();

    // Cerca elementi per parola chiave (es. "Parigi")
    List<CatalogItem> searchItems(String keyword);

    // Trova tutti gli elementi creati da un organizzatore specifico
    List<CatalogItem> getItemsByHost(Long hostId);

    // ... potrai aggiungere altri metodi come getFlightDetails, ecc.
}