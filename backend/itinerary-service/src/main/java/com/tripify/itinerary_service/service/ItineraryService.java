package com.tripify.itinerary_service.service;

import com.tripify.itinerary_service.client.BookingClient;
import com.tripify.itinerary_service.client.CatalogClient;
import com.tripify.itinerary_service.dto.AddListItemRequestDTO;
import com.tripify.itinerary_service.dto.AddToCartRequestDTO;
import com.tripify.itinerary_service.dto.BookAllResultDTO;
import com.tripify.itinerary_service.dto.CatalogItemSummaryDTO;
import com.tripify.itinerary_service.dto.RemoveItemResultDTO;
import com.tripify.itinerary_service.entity.CatalogItemLike;
import com.tripify.itinerary_service.entity.FavoriteList;
import com.tripify.itinerary_service.entity.FavoriteListItem;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final BookingClient bookingClient;

    public FavoriteList createList(String name, String ownerId) {
        FavoriteList list = FavoriteList.builder()
                .name(name)
                .ownerId(ownerId)
                .build();
        return repository.save(list);
    }

    public void addItemToList(Long listId, AddListItemRequestDTO request, String requesterId) {
        FavoriteList list = getEditableList(listId, requesterId);
        FavoriteListItem newItem = FavoriteListItem.builder()
                .catalogItemId(request.catalogItemId())
                .quantity(request.quantity() != null ? request.quantity() : 1)
                .roomTypeId(request.roomTypeId())
                .fareClassId(request.fareClassId())
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .activityDate(request.activityDate())
                .build();

        // Coerenza validata su "lista attuale + nuovo item", PRIMA di salvare: se il
        // nuovo componente rompe la coerenza geografica/temporale del viaggio, viene
        // rifiutato subito (blocco rigido, non un semplice avviso).
        List<FavoriteListItem> candidateItems = new ArrayList<>(list.getItems());
        candidateItems.add(newItem);
        validateItineraryCoherence(candidateItems);

        list.getItems().add(newItem);
        repository.save(list);
    }

    /**
     * Elimina l'intero itinerario. I componenti (list_items) e le condivisioni
     * (list_shares) sono @ElementCollection di FavoriteList: Hibernate li cancella
     * automaticamente insieme al genitore. I like alla lista (favorite_list_likes)
     * sono un'entità separata senza vincolo FK, quindi vanno ripuliti a mano per non
     * lasciare righe orfane.
     */
    @Transactional
    public void deleteList(Long listId, String requesterId) {
        FavoriteList list = getOwnedList(listId, requesterId);
        likeRepository.deleteByListId(listId);
        repository.delete(list);
    }

    /**
     * Rimuove un componente per posizione (0-based, stesso ordine di visualizzazione).
     * L'ordine è la sequenza cronologica del viaggio, quindi togliere un volo può
     * rendere incoerenti i componenti che venivano dopo (es. un hotel rimasto senza
     * il volo di andata): in quel caso vengono troncati anche quelli, e i loro titoli
     * tornano nel risultato così il chiamante può avvisare l'utente.
     */
    public RemoveItemResultDTO removeItemFromList(Long listId, int index, String requesterId) {
        FavoriteList list = getEditableList(listId, requesterId);
        if (index < 0 || index >= list.getItems().size()) {
            throw new IllegalArgumentException("Componente non trovato in questa lista");
        }
        list.getItems().remove(index);

        List<FavoriteListItem> items = list.getItems();
        int keep = longestCoherentPrefix(items);
        List<String> alsoRemoved = new ArrayList<>();
        if (keep < items.size()) {
            for (int i = keep; i < items.size(); i++) {
                try {
                    alsoRemoved.add(catalogClient.getItem(items.get(i).getCatalogItemId()).title());
                } catch (Exception ignored) {
                    // non blocchiamo la rimozione per un titolo che non riusciamo a recuperare
                }
            }
            while (items.size() > keep) {
                items.remove(items.size() - 1);
            }
        }

        repository.save(list);
        return new RemoveItemResultDTO(alsoRemoved);
    }

    /** La porzione iniziale più lunga di items che forma ancora un itinerario coerente. */
    private int longestCoherentPrefix(List<FavoriteListItem> items) {
        for (int k = items.size(); k > 0; k--) {
            try {
                validateItineraryCoherence(items.subList(0, k));
                return k;
            } catch (IllegalArgumentException e) {
                // prefisso ancora troppo lungo, proviamo quello più corto
            }
        }
        return 0;
    }

    private record ResolvedItem(FavoriteListItem source, CatalogItemSummaryDTO catalog) {}

    private List<ResolvedItem> resolveItems(List<FavoriteListItem> items) {
        List<ResolvedItem> resolved = new ArrayList<>();
        for (FavoriteListItem item : items) {
            CatalogItemSummaryDTO catalog;
            try {
                catalog = catalogClient.getItem(item.getCatalogItemId());
            } catch (Exception e) {
                throw new IllegalArgumentException("Impossibile verificare il componente " + item.getCatalogItemId() + " nel catalogo");
            }
            if (catalog == null) {
                throw new IllegalArgumentException("Componente " + item.getCatalogItemId() + " non trovato nel catalogo");
            }
            resolved.add(new ResolvedItem(item, catalog));
        }
        return resolved;
    }

    private record CityWindow(String city, LocalDateTime start, LocalDateTime end) {}

    /**
     * Valida che l'itinerario abbia senso come viaggio reale:
     * 1) i voli, presi in ordine, devono formare una catena continua (si parte dalla
     *    città in cui è atterrato il volo precedente, dopo il suo orario di arrivo);
     * 2) ogni hotel/attività deve trovarsi nella città in cui ci si trova in quel
     *    punto dell'itinerario (l'ultimo volo prima di esso, in ordine di lista) e le
     *    sue date devono cadere nella finestra [arrivo di quel volo, partenza del
     *    prossimo volo da quella città (se già presente)].
     * Lancia IllegalArgumentException (400) con un messaggio chiaro al primo problema
     * trovato: nessun salvataggio parziale, nessun semplice avviso.
     */
    private void validateItineraryCoherence(List<FavoriteListItem> items) {
        List<ResolvedItem> resolved = resolveItems(items);

        List<CatalogItemSummaryDTO> flights = resolved.stream()
                .map(ResolvedItem::catalog)
                .filter(c -> "Flight".equals(c.itemType()))
                .toList();

        for (int i = 1; i < flights.size(); i++) {
            CatalogItemSummaryDTO prev = flights.get(i - 1);
            CatalogItemSummaryDTO curr = flights.get(i);
            if (prev.arrivalCity() == null || curr.departureCity() == null
                    || !prev.arrivalCity().equalsIgnoreCase(curr.departureCity())) {
                throw new IllegalArgumentException(
                        "Il volo \"" + curr.title() + "\" parte da " + curr.departureCity()
                                + ", ma il volo precedente atterra a " + prev.arrivalCity());
            }
            if (prev.arrivalTime() != null && curr.departureTime() != null
                    && !curr.departureTime().isAfter(prev.arrivalTime())) {
                throw new IllegalArgumentException(
                        "Il volo \"" + curr.title() + "\" parte prima che il volo precedente sia atterrato");
            }
        }

        List<CityWindow> windows = new ArrayList<>();
        for (int i = 0; i < flights.size(); i++) {
            LocalDateTime end = (i + 1 < flights.size()) ? flights.get(i + 1).departureTime() : null;
            windows.add(new CityWindow(flights.get(i).arrivalCity(), flights.get(i).arrivalTime(), end));
        }

        int flightIdx = -1;
        for (ResolvedItem resolvedItem : resolved) {
            CatalogItemSummaryDTO catalog = resolvedItem.catalog();
            if ("Flight".equals(catalog.itemType())) {
                flightIdx++;
                continue;
            }

            if (flightIdx == -1) {
                throw new IllegalArgumentException(
                        "\"" + catalog.title() + "\" non può essere aggiunto prima di un volo che porti in quella città");
            }
            CityWindow window = windows.get(flightIdx);
            if (catalog.city() == null || !catalog.city().equalsIgnoreCase(window.city())) {
                throw new IllegalArgumentException(
                        "\"" + catalog.title() + "\" si trova a " + catalog.city()
                                + ", ma in questo punto dell'itinerario ti trovi a " + window.city());
            }

            if ("Hotel".equals(catalog.itemType())) {
                LocalDate checkIn = resolvedItem.source().getCheckIn();
                LocalDate checkOut = resolvedItem.source().getCheckOut();
                if (checkIn == null || checkOut == null) {
                    throw new IllegalArgumentException("Specifica check-in e check-out per l'hotel \"" + catalog.title() + "\"");
                }
                if (!checkOut.isAfter(checkIn)) {
                    throw new IllegalArgumentException("Il check-out deve essere successivo al check-in per l'hotel \"" + catalog.title() + "\"");
                }
                if (checkIn.isBefore(window.start().toLocalDate())) {
                    throw new IllegalArgumentException(
                            "Il check-in dell'hotel \"" + catalog.title() + "\" è prima dell'arrivo del volo a " + window.city());
                }
                if (window.end() != null && checkOut.isAfter(window.end().toLocalDate())) {
                    throw new IllegalArgumentException(
                            "Il check-out dell'hotel \"" + catalog.title() + "\" è dopo la partenza del volo successivo da " + window.city());
                }
            } else if ("Activity".equals(catalog.itemType())) {
                LocalDate activityDate = resolvedItem.source().getActivityDate();
                if (activityDate == null) {
                    throw new IllegalArgumentException("Specifica la data per l'attività \"" + catalog.title() + "\"");
                }
                if (activityDate.isBefore(window.start().toLocalDate())
                        || (window.end() != null && activityDate.isAfter(window.end().toLocalDate()))) {
                    throw new IllegalArgumentException(
                            "La data dell'attività \"" + catalog.title() + "\" è fuori dal periodo in cui ti trovi a " + window.city());
                }
            }
        }
    }

    /**
     * Prezzo reale della lista: tariffa scelta per i voli, prezzo camera × notti per
     * gli hotel, prezzo base × quantità per le attività. Riusa lo stesso fetch di
     * CatalogItemSummaryDTO fatto per la validazione di coerenza.
     */
    public BigDecimal computeTotalPrice(FavoriteList list) {
        List<ResolvedItem> resolved = resolveItems(list.getItems());
        BigDecimal total = BigDecimal.ZERO;
        for (ResolvedItem r : resolved) {
            CatalogItemSummaryDTO catalog = r.catalog();
            FavoriteListItem source = r.source();
            int quantity = source.getQuantity() != null ? source.getQuantity() : 1;
            BigDecimal basePrice = catalog.price() != null ? catalog.price() : BigDecimal.ZERO;
            BigDecimal itemPrice;

            if ("Flight".equals(catalog.itemType())) {
                BigDecimal farePrice = catalog.fareClasses() == null ? null : catalog.fareClasses().stream()
                        .filter(f -> f.id().equals(source.getFareClassId()))
                        .map(CatalogItemSummaryDTO.FareClassSummaryDTO::price)
                        .findFirst().orElse(null);
                itemPrice = (farePrice != null ? farePrice : basePrice).multiply(BigDecimal.valueOf(quantity));
            } else if ("Hotel".equals(catalog.itemType())) {
                BigDecimal roomPrice = catalog.roomTypes() == null ? null : catalog.roomTypes().stream()
                        .filter(rt -> rt.id().equals(source.getRoomTypeId()))
                        .map(CatalogItemSummaryDTO.RoomTypeSummaryDTO::price)
                        .findFirst().orElse(null);
                long nights = (source.getCheckIn() != null && source.getCheckOut() != null)
                        ? Math.max(1, ChronoUnit.DAYS.between(source.getCheckIn(), source.getCheckOut()))
                        : 1;
                itemPrice = (roomPrice != null ? roomPrice : basePrice)
                        .multiply(BigDecimal.valueOf(nights))
                        .multiply(BigDecimal.valueOf(quantity));
            } else {
                itemPrice = basePrice.multiply(BigDecimal.valueOf(quantity));
            }
            source.setPrice(itemPrice);
            total = total.add(itemPrice);
        }
        return total;
    }

    /** Valorizza totalPrice su una lista; in caso di errore verso catalog-service lascia 0 invece di far fallire la lettura. */
    public void applyTotalPrice(FavoriteList list) {
        try {
            list.setTotalPrice(computeTotalPrice(list));
        } catch (Exception e) {
            list.setTotalPrice(BigDecimal.ZERO);
        }
    }

    public void applyTotalPrice(List<FavoriteList> lists) {
        lists.forEach(this::applyTotalPrice);
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

    /** L'accesso via link non dipende dalla visibilità: basta un token valido (vedi enableLinkSharing). */
    public FavoriteList getByPublicToken(String token) {
        return repository.findByPublicToken(token)
                .orElseThrow(() -> new ListNotFoundException("Link non valido o non più attivo"));
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
     * Non tocca il link di condivisione: è un controllo indipendente (vedi
     * enableLinkSharing/disableLinkSharing), diventare pubblici ne crea uno se manca
     * ancora, ma tornare privati non lo revoca più automaticamente.
     */
    public FavoriteList setVisibility(Long listId, Visibility newVisibility, String city, String requesterId) {
        FavoriteList list = getOwnedList(listId, requesterId);

        if (newVisibility == Visibility.PUBLIC) {
            if (city == null || city.isBlank()) {
                throw new IllegalArgumentException("La città è obbligatoria per pubblicare una lista");
            }
            validatePublishRequirements(list);
            list.setCity(city.trim());
            ensureLinkToken(list);
        }
        list.setVisibility(newVisibility);
        return repository.save(list);
    }

    /**
     * Attiva il link di condivisione (capabilities) su una lista privata o condivisa,
     * senza richiedere i requisiti minimi di pubblicazione né renderla ricercabile
     * nel feed pubblico: è un controllo indipendente dalla visibilità, per poter
     * mandare a qualcuno un itinerario ancora in bozza senza esporlo a tutti.
     */
    public FavoriteList enableLinkSharing(Long listId, String requesterId) {
        FavoriteList list = getOwnedList(listId, requesterId);
        ensureLinkToken(list);
        return repository.save(list);
    }

    /** Revoca il link: chi lo aveva salvato non potrà più usarlo. La visibilità non cambia. */
    public FavoriteList disableLinkSharing(Long listId, String requesterId) {
        FavoriteList list = getOwnedList(listId, requesterId);
        list.setPublicToken(null);
        return repository.save(list);
    }

    private void ensureLinkToken(FavoriteList list) {
        if (list.getPublicToken() == null) {
            list.setPublicToken(UUID.randomUUID().toString());
        }
    }

    /**
     * Attiva il link di invito: chi lo apre da loggato (vedi joinAsCollaborator) può
     * modificare la lista, non solo vederla — un accesso ben più ampio del link di
     * sola visualizzazione, quindi un token separato.
     */
    public FavoriteList enableCollabInvite(Long listId, String requesterId) {
        FavoriteList list = getOwnedList(listId, requesterId);
        if (list.getCollabToken() == null) {
            list.setCollabToken(UUID.randomUUID().toString());
        }
        return repository.save(list);
    }

    /** Revoca l'invito: chi lo aveva salvato non può più usarlo per unirsi. I collaboratori già aggiunti restano. */
    public FavoriteList disableCollabInvite(Long listId, String requesterId) {
        FavoriteList list = getOwnedList(listId, requesterId);
        list.setCollabToken(null);
        return repository.save(list);
    }

    /**
     * Chi apre il link di invito da loggato entra come collaboratore (può modificare
     * la lista): stesso meccanismo di shareList, ma è chi si unisce ad agire su se
     * stesso, non il proprietario ad aggiungere qualcuno che conosce già.
     */
    @Transactional
    public FavoriteList joinAsCollaborator(String collabToken, String joinerId) {
        FavoriteList list = repository.findByCollabToken(collabToken)
                .orElseThrow(() -> new ListNotFoundException("Link di invito non valido o non più attivo"));

        if (!list.getOwnerId().equals(joinerId) && !list.getSharedUserIds().contains(joinerId)) {
            list.getSharedUserIds().add(joinerId);
            if (list.getVisibility() == Visibility.PRIVATE) {
                list.setVisibility(Visibility.SHARED);
            }
            repository.save(list);
        }
        return list;
    }

    /** Rinomina la lista: solo il proprietario, come le altre modifiche "strutturali". */
    public FavoriteList renameList(Long listId, String newName, String requesterId) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Il nome dell'itinerario non può essere vuoto");
        }
        FavoriteList list = getOwnedList(listId, requesterId);
        list.setName(newName.trim());
        return repository.save(list);
    }

    private void validatePublishRequirements(FavoriteList list) {
        int flights = 0, hotels = 0, activities = 0;
        for (FavoriteListItem listItem : list.getItems()) {
            Long itemId = listItem.getCatalogItemId();
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
     * in booking-service (che itinerary-service non tocca). Richiede lo stesso accesso
     * di lettura della lista (pubblica, propria o condivisa), altrimenti chiunque con
     * un JWT valido potrebbe gonfiare il contatore di un itinerario altrui a piacere.
     */
    public void registerBookingAttempt(Long listId, String requesterId) {
        FavoriteList list = getAccessibleById(listId, requesterId);
        list.setBookingsCount(list.getBookingsCount() + 1);
        repository.save(list);
    }

    /**
     * "Prenota tutto": aggiunge ogni componente della lista al carrello reale
     * dell'utente su booking-service, propagando il suo JWT così l'identità è
     * verificata da booking-service esattamente come per una singola aggiunta
     * manuale al carrello. Continua anche se un componente fallisce, per non
     * bloccare gli altri; il contatore pubblico cresce solo se almeno un
     * componente è stato aggiunto davvero.
     */
    public BookAllResultDTO bookAllItems(Long listId, String requesterId, String rawJwt) {
        FavoriteList list = getAccessibleById(listId, requesterId);
        String authorizationHeader = "Bearer " + rawJwt;

        int successCount = 0;
        List<String> errors = new ArrayList<>();
        for (FavoriteListItem item : list.getItems()) {
            try {
                bookingClient.addToCart(authorizationHeader, new AddToCartRequestDTO(
                        item.getCatalogItemId(), item.getQuantity(),
                        item.getRoomTypeId(), item.getFareClassId(),
                        item.getCheckIn(), item.getCheckOut()));
                successCount++;
            } catch (Exception e) {
                errors.add("Componente " + item.getCatalogItemId() + ": " + e.getMessage());
            }
        }

        if (successCount > 0) {
            registerBookingAttempt(listId, requesterId);
        }
        return new BookAllResultDTO(successCount, list.getItems().size(), errors);
    }

    private FavoriteList getOwnedList(Long listId, String requesterId) {
        FavoriteList list = getById(listId);
        if (!list.getOwnerId().equals(requesterId)) {
            throw new NotListOwnerException();
        }
        return list;
    }

    /** Come getOwnedList, ma ammette anche i collaboratori (sharedUserIds) per le modifiche di contenuto. */
    private FavoriteList getEditableList(Long listId, String requesterId) {
        FavoriteList list = getById(listId);
        boolean canEdit = list.getOwnerId().equals(requesterId) || list.getSharedUserIds().contains(requesterId);
        if (!canEdit) {
            throw new NotListOwnerException("Non hai i permessi per modificare questa lista");
        }
        return list;
    }
}
