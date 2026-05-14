package com.tripify.itinerary_service.service;

import com.tripify.itinerary_service.entity.FavoriteList;
import com.tripify.itinerary_service.repository.FavoriteListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final FavoriteListRepository repository;

    public FavoriteList createList(String name, Long ownerId) {
        FavoriteList list = FavoriteList.builder()
                .name(name)
                .ownerId(ownerId)
                .build();
        return repository.save(list);
    }

    public void addItemToList(Long listId, Long itemId) {
        FavoriteList list = repository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Lista non trovata"));
        list.getCatalogItemIds().add(itemId);
        repository.save(list);
    }

    public void shareList(Long listId, Long userIdToShareWith) {
        FavoriteList list = repository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Lista non trovata"));
        if (!list.getSharedUserIds().contains(userIdToShareWith)) {
            list.getSharedUserIds().add(userIdToShareWith);
            repository.save(list);
        }
    }

    public List<FavoriteList> getUserLists(Long userId) {
        // Ritorna sia le proprie che quelle condivise
        List<FavoriteList> owned = repository.findByOwnerId(userId);
        owned.addAll(repository.findBySharedUserIdsContaining(userId));
        return owned;
    }
}