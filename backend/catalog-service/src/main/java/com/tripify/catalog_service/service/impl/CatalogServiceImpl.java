package com.tripify.catalog_service.service.impl;

import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.mapper.CatalogMapper; // <-- Importa il mapper
import com.tripify.catalog_service.repository.CatalogItemRepository;
import com.tripify.catalog_service.repository.spec.CatalogItemSpecification;
import com.tripify.catalog_service.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CatalogItemRepository catalogItemRepository;
    private final CatalogMapper catalogMapper; // <-- Inietta il componente

    @Override
    public List<CatalogItem> getAllItems() {
        return catalogItemRepository.findAll();
    }

    @Override
    public List<CatalogItemDTO> search(String category, String query, BigDecimal maxPrice, Integer minRating) {
        Specification<CatalogItem> spec = CatalogItemSpecification.withDynamicFilters(category, query, maxPrice, minRating);
        List<CatalogItem> items = catalogItemRepository.findAll(spec);

        // Usiamo il mapper separato!
        return items.stream()
                .map(catalogMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CatalogItem> getItemsByHost(UUID hostId) {
        return catalogItemRepository.findByHostId(hostId);
    }

    @Override
    public CatalogItem saveItem(CatalogItem item) {
        return catalogItemRepository.save(item);
    }
}