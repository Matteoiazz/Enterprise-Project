package com.tripify.itinerary_service.service;

import com.tripify.itinerary_service.client.CatalogClient;
import com.tripify.itinerary_service.dto.CatalogItemSummaryDTO;
import com.tripify.itinerary_service.entity.CatalogItemLike;
import com.tripify.itinerary_service.entity.FavoriteList;
import com.tripify.itinerary_service.entity.FavoriteListLike;
import com.tripify.itinerary_service.entity.Visibility;
import com.tripify.itinerary_service.exception.ListNotFoundException;
import com.tripify.itinerary_service.exception.NotListOwnerException;
import com.tripify.itinerary_service.exception.PublishRequirementsNotMetException;
import com.tripify.itinerary_service.repository.CatalogItemLikeRepository;
import com.tripify.itinerary_service.repository.FavoriteListLikeRepository;
import com.tripify.itinerary_service.repository.FavoriteListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItineraryService {

    private static final int MIN_FLIGHTS = 2;
    private static final int MIN_HOTELS = 1;
    private static final int MIN_ACTIVITIES = 1;

    private final FavoriteListRepository repository;
    private final FavoriteListLikeRepository likeRepository;
    private final CatalogItemLikeRepository catalogItemLikeRepository;
    private final CatalogClient catalogClient;

    public FavoriteList createList(String name, String ownerId) {
        FavoriteList list = FavoriteList.builder()
                .name(name)
                .ownerId(ownerId)
                .build();
        return repository.save(list);
    }

    public void addItemToList(Long listId, Long itemId, String requesterId) {
        FavoriteList list = getOwnedList(listId, requesterId);
        list.getCatalogItemIds().add(itemId);
        repository.save(list);
    }

    public void shareList(Long listId, String userIdToShareWith, String requesterId) {
        FavoriteList list = getOwnedList(listId, requesterId);
        if (!list.getSharedUserIds().contains(userIdToShareWith)) {
            list.getSharedUserIds().add(userIdToShareWith);
        }
        if (list.getVisibility() == Visibility.PRIVATE) {
            list.setVisibility(Visibility.SHARED);
        }
        repository.save(list);
    }

    public List<FavoriteList> getUserLists(String userId) {
        // Ritorna sia le proprie che quelle condivise. Usata da "Le mie liste" (dove si
        // può anche aggiungere un item), quindi include solo liste su cui si può agire,
        // non gli itinerari altrui a cui si è messo semplicemente like.
        List<FavoriteList> owned = repository.findByOwnerId(userId);
        owned.addAll(repository.findBySharedUserIdsContaining(userId));
        return owned;
    }

    /**
     * "Salvati": tutto ciò che l'utente considera suo in senso lato — le liste che
     * possiede, quelle condivise con lui, e gli itinerari pubblici altrui a cui ha
     * messo like. A differenza di getUserLists() questa è pensata solo per la
     * visualizzazione, non per poterci aggiungere componenti.
     */
    public List<FavoriteList> getSavedLists(String userId) {
        Map<Long, FavoriteList> merged = new LinkedHashMap<>();
        for (FavoriteList list : repository.findByOwnerId(userId)) merged.put(list.getId(), list);
        for (FavoriteList list : repository.findBySharedUserIdsContaining(userId)) merged.putIfAbsent(list.getId(), list);

        List<Long> likedListIds = likeRepository.findByUserId(userId).stream().map(FavoriteListLike::getListId).toList();
        if (!likedListIds.isEmpty()) {
            for (FavoriteList list : repository.findAllById(likedListIds)) merged.putIfAbsent(list.getId(), list);
        }

        List<FavoriteList> result = new ArrayList<>(merged.values());
        applyLikedByMe(result, userId);
        return result;
    }

    /** Mette/toglie il like a un singolo elemento del catalogo (non a un'intera lista). */
    @Transactional
    public boolean toggleCatalogItemLike(Long catalogItemId, String userId) {
        var existing = catalogItemLikeRepository.findByUserIdAndCatalogItemId(userId, catalogItemId);
        if (existing.isPresent()) {
            catalogItemLikeRepository.delete(existing.get());
            return false;
        }
        catalogItemLikeRepository.save(CatalogItemLike.builder().userId(userId).catalogItemId(catalogItemId).build());
        return true;
    }

    /** Id dei componenti di catalogo a cui l'utente ha messo like singolarmente, più recenti prima. */
    public List<Long> getLikedCatalogItemIds(String userId) {
        return catalogItemLikeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CatalogItemLike::getCatalogItemId)
                .toList();
    }

    public FavoriteList getById(Long listId) {
        return repository.findById(listId).orElseThrow(() -> new ListNotFoundException(listId));
    }

    /** Dettaglio di una lista: visibile se pubblica, o se il richiedente è proprietario/condivisa con lui. */
    public FavoriteList getAccessibleById(Long listId, String requesterId) {
        FavoriteList list = getById(listId);
        boolean allowed = list.getVisibility() == Visibility.PUBLIC
                || list.getOwnerId().equals(requesterId)
                || list.getSharedUserIds().contains(requesterId);
        if (!allowed) {
            throw new NotListOwnerException();
        }
        return list;
    }

    public FavoriteList getByPublicToken(String token) {
        return repository.findByPublicToken(token)
                .filter(list -> list.getVisibility() == Visibility.PUBLIC)
                .orElseThrow(() -> new ListNotFoundException("Nessuna lista pubblica trovata per questo link"));
    }

    /** Valorizza likedByMe su una lista in base a chi sta guardando (null se non autenticato). */
    public void applyLikedByMe(FavoriteList list, String requesterId) {
        list.setLikedByMe(requesterId != null && likeRepository.existsByListIdAndUserId(list.getId(), requesterId));
    }

    public void applyLikedByMe(List<FavoriteList> lists, String requesterId) {
        if (requesterId == null) return;
        lists.forEach(list -> applyLikedByMe(list, requesterId));
    }

    /**
     * Cambia la visibilità di una lista. Per passare a PUBLIC serve una città esplicita
     * (non derivata dai componenti) e il requisito minimo di componenti (2 voli, 1 hotel,
     * 1 attività) — verificato interrogando catalog-service per ciascun componente.
     */
    public FavoriteList setVisibility(Long listId, Visibility newVisibility, String city, String requesterId) {
        FavoriteList list = getOwnedList(listId, requesterId);

        if (newVisibility == Visibility.PUBLIC) {
            if (city == null || city.isBlank()) {
                throw new IllegalArgumentException("La città è obbligatoria per pubblicare una lista");
            }
            validatePublishRequirements(list);
            list.setCity(city.trim());
            if (list.getPublicToken() == null) {
                list.setPublicToken(UUID.randomUUID().toString());
            }
        }
        list.setVisibility(newVisibility);
        return repository.save(list);
    }

    private void validatePublishRequirements(FavoriteList list) {
        int flights = 0, hotels = 0, activities = 0;
        for (Long itemId : list.getCatalogItemIds()) {
            CatalogItemSummaryDTO item;
            try {
                item = catalogClient.getItem(itemId);
            } catch (Exception e) {
                throw new PublishRequirementsNotMetException("Impossibile verificare il componente " + itemId + " nel catalogo");
            }
            if (item == null || item.itemType() == null) continue;
            switch (item.itemType()) {
                case "Flight" -> flights++;
                case "Hotel" -> hotels++;
                case "Activity" -> activities++;
                default -> { }
            }
        }
        if (flights < MIN_FLIGHTS || hotels < MIN_HOTELS || activities < MIN_ACTIVITIES) {
            throw new PublishRequirementsNotMetException(
                    "Per pubblicare una lista servono almeno " + MIN_FLIGHTS + " voli, " + MIN_HOTELS
                            + " hotel e " + MIN_ACTIVITIES + " attività (trovati: " + flights + " voli, "
                            + hotels + " hotel, " + activities + " attività)");
        }
    }

    public List<FavoriteList> getPublicFeed(String city, String sort) {
        boolean byLikes = !"recent".equalsIgnoreCase(sort);
        if (city != null && !city.isBlank()) {
            return byLikes
                    ? repository.findByVisibilityAndCityIgnoreCaseOrderByLikesCountDesc(Visibility.PUBLIC, city.trim())
                    : repository.findByVisibilityAndCityIgnoreCaseOrderByCreatedAtDesc(Visibility.PUBLIC, city.trim());
        }
        return byLikes
                ? repository.findByVisibilityOrderByLikesCountDesc(Visibility.PUBLIC)
                : repository.findByVisibilityOrderByCreatedAtDesc(Visibility.PUBLIC);
    }

    @Transactional
    public boolean toggleLike(Long listId, String userId) {
        FavoriteList list = getById(listId);
        if (list.getVisibility() != Visibility.PUBLIC) {
            throw new IllegalArgumentException("Si può mettere like solo a liste pubbliche");
        }
        var existing = likeRepository.findByListIdAndUserId(listId, userId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            list.setLikesCount(Math.max(0, list.getLikesCount() - 1));
            repository.save(list);
            return false;
        }
        likeRepository.save(FavoriteListLike.builder().listId(listId).userId(userId).build());
        list.setLikesCount(list.getLikesCount() + 1);
        repository.save(list);
        return true;
    }

    /**
     * Incrementa il contatore "prenotazioni tentate" quando l'utente preme "prenota
     * tutto": è un contatore best-effort, non collegato all'esito reale del pagamento
     * in booking-service (che itinerary-service non tocca).
     */
    public void registerBookingAttempt(Long listId) {
        FavoriteList list = getById(listId);
        list.setBookingsCount(list.getBookingsCount() + 1);
        repository.save(list);
    }

    private FavoriteList getOwnedList(Long listId, String requesterId) {
        FavoriteList list = getById(listId);
        if (!list.getOwnerId().equals(requesterId)) {
            throw new NotListOwnerException();
        }
        return list;
    }
}
