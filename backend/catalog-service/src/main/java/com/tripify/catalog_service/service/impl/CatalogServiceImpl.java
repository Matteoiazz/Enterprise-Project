package com.tripify.catalog_service.service.impl;

import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.repository.CatalogItemRepository;
import com.tripify.catalog_service.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CatalogItemRepository catalogItemRepository;

    @Override
    public List<CatalogItem> getAllItems() {
        return catalogItemRepository.findAll();
    }

    @Override
    public List<CatalogItem> searchItems(String keyword) {
        return catalogItemRepository.findByTitleContainingIgnoreCase(keyword);
    }

    @Override
    public List<CatalogItem> getItemsByHost(Long hostId) {
        return catalogItemRepository.findByHostId(hostId);
    }

    // NUOVO: Implementazione del salvataggio
    @Override
    public CatalogItem saveItem(CatalogItem item) {
        return catalogItemRepository.save(item);
    }
}