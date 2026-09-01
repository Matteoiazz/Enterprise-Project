package com.tripify.catalog_service.service.impl;

import com.tripify.catalog_service.dto.CatalogItemDTO;
import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.entity.Hotel;
import com.tripify.catalog_service.exception.CatalogItemNotFoundException;
import com.tripify.catalog_service.mapper.CatalogMapper;
import com.tripify.catalog_service.repository.CatalogItemRepository;
import com.tripify.catalog_service.repository.spec.CatalogItemSpecification;
import com.tripify.catalog_service.service.AvailabilityService;
import com.tripify.catalog_service.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CatalogItemRepository catalogItemRepository;
    private final CatalogMapper catalogMapper;
    private final AvailabilityService availabilityService;

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
            LocalDate checkIn,
            LocalDate checkOut,
            Integer rooms,
            Pageable pageable
    ) {
        Specification<CatalogItem> spec = CatalogItemSpecification.withDynamicFilters(
                category, query, maxPrice, minRating, destination, departure, guideIncluded, amenities, directOnly, departureDate, minSeats
        );

        if (checkIn != null && checkOut != null) {

            int requestedRooms = rooms == null ? 1 : rooms;
            List<CatalogItemDTO> available = catalogItemRepository.findAll(spec, pageable.getSort()).stream()
                    .filter(item -> !(item instanceof Hotel hotel) || hasAvailableRoomType(hotel, checkIn, checkOut, requestedRooms))
                    .map(catalogMapper::toDto)
                    .toList();
            return paginate(available, pageable);
        }

        Page<CatalogItem> items = catalogItemRepository.findAll(spec, pageable);
        return items.map(catalogMapper::toDto);
    }

    private boolean hasAvailableRoomType(Hotel hotel, LocalDate checkIn, LocalDate checkOut, int rooms) {
        return hotel.getRoomTypes().stream()
                .anyMatch(rt -> availabilityService.computeRoomAvailability(rt.getId(), checkIn, checkOut) >= rooms);
    }

    private Page<CatalogItemDTO> paginate(List<CatalogItemDTO> all, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= all.size()) {
            return new PageImpl<>(List.of(), pageable, all.size());
        }
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    @Override
    public List<CatalogItem> getItemsByHost(UUID hostId) {
        return catalogItemRepository.findByHostIdAndIsActiveTrue(hostId);
    }

    @Override
    public CatalogItem saveItem(CatalogItem item) {
        return catalogItemRepository.save(item);
    }

    @Override
    public CatalogItem getRawItemById(Long id) {
        return catalogItemRepository.findById(id)
                .orElseThrow(() -> new CatalogItemNotFoundException(id));
    }

    @Override
    public void deactivateItem(Long id) {
        catalogItemRepository.delete(getRawItemById(id));
    }

    @Override
    public List<String> getCitySuggestions(String query) {
        return catalogItemRepository.findCitySuggestions(query);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CatalogItem addImages(Long itemId, List<String> imageUrls) {
        CatalogItem item = getRawItemById(itemId);
        for (String url : imageUrls) {
            item.addImage(com.tripify.catalog_service.entity.CatalogImage.builder().imageUrl(url).build());
        }
        return catalogItemRepository.save(item);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CatalogItem removeImage(Long itemId, String imageUrl) {
        CatalogItem item = getRawItemById(itemId);
        item.getImages().removeIf(image -> image.getImageUrl().equals(imageUrl));
        return catalogItemRepository.save(item);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CatalogItem updateRating(Long itemId, Double average, Integer count) {
        CatalogItem item = getRawItemById(itemId);
        if (average == null || count == null || count <= 0) {
            item.setRatingAvg(null);
            item.setReviewCount(0);
            item.setRating(null);
        } else {
            double clamped = Math.max(1.0, Math.min(5.0, average));
            item.setRatingAvg(Math.round(clamped * 10.0) / 10.0);
            item.setReviewCount(count);
            item.setRating((int) Math.round(clamped));
        }
        return catalogItemRepository.save(item);
    }
}
