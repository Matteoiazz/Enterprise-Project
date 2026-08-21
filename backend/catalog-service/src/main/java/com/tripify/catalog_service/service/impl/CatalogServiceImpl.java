package com.tripify.catalog_service.service.impl;

import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.exception.CatalogItemNotFoundException;
import com.tripify.catalog_service.mapper.CatalogMapper;
import com.tripify.catalog_service.repository.CatalogItemRepository;
import com.tripify.catalog_service.repository.spec.CatalogItemSpecification;
import com.tripify.catalog_service.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CatalogItemRepository catalogItemRepository;
    private final CatalogMapper catalogMapper;

    @Override
    public List<CatalogItemDTO> getAllItems() {
        return catalogItemRepository.findAll().stream()
                .map(catalogMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CatalogItemDTO getItemById(Long id) {
        CatalogItem item = catalogItemRepository.findById(id)
                .orElseThrow(() -> new CatalogItemNotFoundException(id));
        return catalogMapper.toDto(item);
    }

    @Override
    public Page<CatalogItemDTO> search(
            String category,
            String query,
            BigDecimal maxPrice,
            Integer minRating,
            String destination,
            String departure,
            Boolean guideIncluded,
            List<String> amenities,
            Boolean directOnly,
            LocalDate departureDate,
            Integer minSeats,
            Pageable pageable
    ) {
        Specification<CatalogItem> spec = CatalogItemSpecification.withDynamicFilters(
                category, query, maxPrice, minRating, destination, departure, guideIncluded, amenities, directOnly, departureDate, minSeats
        );
        Page<CatalogItem> items = catalogItemRepository.findAll(spec, pageable);

        return items.map(catalogMapper::toDto);
    }

    @Override
    public List<CatalogItem> getItemsByHost(UUID hostId) {
        return catalogItemRepository.findByHostId(hostId);
    }

    @Override
    public CatalogItem saveItem(CatalogItem item) {
        return catalogItemRepository.save(item);
    }
    @Override
    public List<String> getCitySuggestions(String query) {
        return catalogItemRepository.findCitySuggestions(query);
    }
}
