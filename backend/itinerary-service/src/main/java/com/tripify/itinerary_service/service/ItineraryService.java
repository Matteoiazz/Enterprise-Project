package com.tripify.itinerary_service.service;

import com.tripify.itinerary_service.client.BookingClient;
import com.tripify.itinerary_service.client.CatalogClient;
import com.tripify.itinerary_service.dto.AddListItemRequestDTO;
import com.tripify.itinerary_service.dto.AddToCartRequestDTO;
import com.tripify.itinerary_service.dto.BookAllResultDTO;
import com.tripify.itinerary_service.dto.CalendarEventDTO;
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
import com.tripify.itinerary_service.util.IcsBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final PlatformTransactionManager transactionManager;

    public FavoriteList createList(String name, String ownerId) {
        FavoriteList list = FavoriteList.builder()
                .name(name)
                .ownerId(ownerId)
                .build();
        return repository.save(list);
    }

    @Transactional
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
     * Copia i componenti di una lista accessibile (pubblica, propria o condivisa) in
     * una nuova lista privata di proprietà di chi clona. Stesso identico contenuto
     * (componenti, quantità, date), ma like/visibilità/link ripartono da zero, come
     * una lista appena creata a mano — non è collegata all'originale in alcun modo.
     */
    public FavoriteList cloneList(Long sourceId, String requesterId) {
        FavoriteList source = getAccessibleById(sourceId, requesterId);
        List<FavoriteListItem> copiedItems = new ArrayList<>();
        for (FavoriteListItem item : source.getItems()) {
            copiedItems.add(FavoriteListItem.builder()
                    .catalogItemId(item.getCatalogItemId())
                    .quantity(item.getQuantity())
                    .roomTypeId(item.getRoomTypeId())
                    .fareClassId(item.getFareClassId())
                    .checkIn(item.getCheckIn())
                    .checkOut(item.getCheckOut())
                    .activityDate(item.getActivityDate())
                    .build());
        }
        FavoriteList clone = FavoriteList.builder()
                .name("Copia di " + source.getName())
                .ownerId(requesterId)
                .items(copiedItems)
                .build();
        return repository.save(clone);
    }

    private static final int GENERATE_CANDIDATE_POOL = 100;

    /** Vero se almeno una tariffa ha abbastanza posti per tutta la comitiva (o se il volo non espone la capienza). */
    private boolean flightHasCapacityFor(CatalogItemSummaryDTO flight, int travelers) {
        if (flight.fareClasses() == null || flight.fareClasses().isEmpty()) return true;
        return flight.fareClasses().stream().anyMatch(f -> f.totalSeats() == null || f.totalSeats() >= travelers);
    }

    /** Tariffa più economica TRA QUELLE con posti sufficienti per tutta la comitiva. */
    private CatalogItemSummaryDTO.FareClassSummaryDTO cheapestFareClassFor(CatalogItemSummaryDTO flight, int travelers) {
        if (flight.fareClasses() == null || flight.fareClasses().isEmpty()) return null;
        return flight.fareClasses().stream()
                .filter(f -> f.totalSeats() == null || f.totalSeats() >= travelers)
                .min(Comparator.comparing(f -> f.price() != null ? f.price() : BigDecimal.ZERO))
                .orElse(null);
    }

    private record RoomChoice(CatalogItemSummaryDTO.RoomTypeSummaryDTO roomType, int rooms, BigDecimal totalPerNight) {}

    /**
     * La combinazione camera+numero di camere che minimizza il costo TOTALE per notte
     * per tutta la comitiva — non la camera con il prezzo unitario più basso: una
     * camera da 80€ che ospita 2 persone richiede 2 camere (160€) per una comitiva di
     * 4, mentre una da 100€ che ne ospita 4 basta da sola (100€) ed è più economica nel
     * complesso pur costando di più "a listino".
     */
    private RoomChoice cheapestRoomChoiceFor(CatalogItemSummaryDTO hotel, int travelers) {
        if (hotel.roomTypes() == null || hotel.roomTypes().isEmpty()) return null;
        RoomChoice best = null;
        for (CatalogItemSummaryDTO.RoomTypeSummaryDTO roomType : hotel.roomTypes()) {
            int occupancy = (roomType.maxOccupancy() != null && roomType.maxOccupancy() > 0) ? roomType.maxOccupancy() : travelers;
            int rooms = (int) Math.ceil(travelers / (double) occupancy);
            BigDecimal totalPerNight = (roomType.price() != null ? roomType.price() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(rooms));
            if (best == null || totalPerNight.compareTo(best.totalPerNight()) < 0) {
                best = new RoomChoice(roomType, rooms, totalPerNight);
            }
        }
        return best;
    }

    /**
     * Genera una bozza di itinerario per una città: il volo più economico da
     * departureCity che vi atterra (con posti per tutta la comitiva), l'hotel più
     * economico in città per tutta la durata (camere quante ne servono per la
     * comitiva), fino a un'attività al giorno (le più economiche disponibili), ed
     * eventualmente un volo di ritorno se richiesto — pescati da catalog-service con
     * ricerche per destinazione. Hotel e attività sono ancorati alla stessa stringa di
     * arrivalCity del volo scelto (non alla città passata dall'utente): la validazione
     * di coerenza confronta stringhe esatte, quindi un'incoerenza di maiuscole/spazi
     * tra il testo digitato e i dati di catalogo non deve far fallire una generazione
     * altrimenti corretta. Se catalog-service non ha un volo per quella tratta (o
     * abbastanza posti per la comitiva), fallisce con un messaggio chiaro invece di
     * produrre un itinerario incoerente; il volo di ritorno invece non è mai motivo di
     * fallimento — se non se ne trova uno adatto (o il budget non lo copre), l'utente
     * può sempre aggiungerlo a mano. Il risultato finale passa comunque dalla stessa
     * validazione usata per l'aggiunta manuale (validateItineraryCoherence).
     */
    public FavoriteList generateItinerary(String departureCity, String city, int days, int travelers,
                                           boolean wantReturnFlight, BigDecimal budget, String ownerId) {
        String from = departureCity.trim();
        String to = city.trim();
        List<CatalogItemSummaryDTO> candidates;
        try {
            candidates = catalogClient.searchByDestination(to, GENERATE_CANDIDATE_POOL).content();
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossibile contattare il catalogo, riprova più tardi");
        }

        Comparator<CatalogItemSummaryDTO> byPrice = Comparator.comparing(
                c -> c.price() != null ? c.price() : BigDecimal.ZERO);

        // Un volo entra in gioco solo se in quella stessa città (stringa esatta,
        // case-insensitive) esiste già almeno un hotel: senza questo, il volo più
        // economico potrebbe atterrare in una città "vicina" nel testo (es. una
        // ricerca su "Roma" che include anche un volo per "Roma Fiumicino") per cui
        // non esiste nessun hotel con la stessa identica stringa città, facendo
        // fallire la generazione con un "nessun hotel disponibile" fuorviante anche
        // quando la destinazione richiesta ne ha eccome.
        Set<String> hotelCities = candidates.stream()
                .filter(c -> "Hotel".equals(c.itemType()) && c.city() != null)
                .map(c -> c.city().toLowerCase())
                .collect(Collectors.toSet());

        List<CatalogItemSummaryDTO> flights = candidates.stream()
                .filter(c -> "Flight".equals(c.itemType()) && c.arrivalCity() != null && c.arrivalTime() != null
                        && hotelCities.contains(c.arrivalCity().toLowerCase())
                        && c.departureCity() != null && c.departureCity().trim().equalsIgnoreCase(from))
                .sorted(byPrice)
                .toList();

        // Tra i voli sulla tratta, il più economico che abbia davvero posto per tutta
        // la comitiva: uno con una sola tariffa da 6 posti non basta per 10 viaggiatori,
        // anche se è il più economico in assoluto.
        CatalogItemSummaryDTO flight = flights.stream()
                .filter(f -> flightHasCapacityFor(f, travelers))
                .findFirst()
                .orElse(null);
        if (flight == null) {
            if (flights.isEmpty()) {
                throw new IllegalArgumentException("Nessun volo da " + from + " a " + to + " con hotel disponibile: prova un'altra tratta");
            }
            String people = travelers == 1 ? "1 persona" : travelers + " persone";
            throw new IllegalArgumentException(
                    "Nessun volo da " + from + " a " + to + " con posti sufficienti per " + people + ": prova un'altra tratta o riduci i viaggiatori");
        }
        String arrivalCity = flight.arrivalCity();

        List<CatalogItemSummaryDTO> hotels = candidates.stream()
                .filter(c -> "Hotel".equals(c.itemType()) && arrivalCity.equalsIgnoreCase(c.city()))
                .sorted(byPrice)
                .toList();
        if (hotels.isEmpty()) {
            // Non dovrebbe accadere mai (il filtro sui voli sopra lo garantisce): rete
            // di sicurezza nel caso il presupposto smetta di valere.
            throw new IllegalArgumentException("Nessun hotel disponibile a " + to + ": prova un'altra destinazione");
        }
        List<CatalogItemSummaryDTO> activities = candidates.stream()
                .filter(c -> "Activity".equals(c.itemType()) && arrivalCity.equalsIgnoreCase(c.city()))
                .sorted(byPrice)
                .toList();

        // La tariffa scelta deve essere davvero la più economica CON POSTO PER TUTTI:
        // prendere semplicemente il primo elemento della lista restituita da Hibernate
        // (senza @OrderBy) non garantisce affatto che sia la più economica, anche se il
        // volo nel suo complesso è stato scelto per prezzo minimo tra i candidati.
        CatalogItemSummaryDTO.FareClassSummaryDTO cheapestFare = cheapestFareClassFor(flight, travelers);
        Long flightFareClassId = cheapestFare != null ? cheapestFare.id() : null;
        BigDecimal flightUnitPrice = cheapestFare != null
                ? (cheapestFare.price() != null ? cheapestFare.price() : BigDecimal.ZERO)
                : (flight.price() != null ? flight.price() : BigDecimal.ZERO);
        BigDecimal flightTotalPrice = flightUnitPrice.multiply(BigDecimal.valueOf(travelers));

        LocalDate arrivalDate = flight.arrivalTime().toLocalDate();
        LocalDate checkOut = arrivalDate.plusDays(days);

        CatalogItemSummaryDTO hotel = hotels.get(0);
        RoomChoice roomChoice = cheapestRoomChoiceFor(hotel, travelers);
        Long roomTypeId = roomChoice != null ? roomChoice.roomType().id() : null;
        int roomsNeeded = roomChoice != null ? roomChoice.rooms() : 1;
        BigDecimal roomUnitPrice = roomChoice != null
                ? (roomChoice.roomType().price() != null ? roomChoice.roomType().price() : BigDecimal.ZERO)
                : (hotel.price() != null ? hotel.price() : BigDecimal.ZERO);
        BigDecimal roomTotalPrice = roomUnitPrice.multiply(BigDecimal.valueOf(days)).multiply(BigDecimal.valueOf(roomsNeeded));
        BigDecimal spent = flightTotalPrice.add(roomTotalPrice);

        if (budget != null && spent.compareTo(budget) > 0) {
            throw new IllegalArgumentException(
                    "Il budget indicato non copre nemmeno il volo e l'hotel più economici disponibili per " + to);
        }

        // Costruiamo anche i ResolvedItem insieme ai FavoriteListItem: i DTO sono già
        // in mano dalla ricerca, quindi la validazione finale può riusarli invece di
        // rifare una chiamata a catalog-service per ciascun componente (vedi
        // validateResolvedCoherence, la stessa usata da resolveItems).
        List<FavoriteListItem> items = new ArrayList<>();
        List<ResolvedItem> resolved = new ArrayList<>();

        FavoriteListItem flightItem = FavoriteListItem.builder().catalogItemId(flight.id()).quantity(travelers).fareClassId(flightFareClassId).build();
        items.add(flightItem);
        resolved.add(new ResolvedItem(flightItem, flight));

        FavoriteListItem hotelItem = FavoriteListItem.builder().catalogItemId(hotel.id()).quantity(roomsNeeded).roomTypeId(roomTypeId)
                .checkIn(arrivalDate).checkOut(checkOut).build();
        items.add(hotelItem);
        resolved.add(new ResolvedItem(hotelItem, hotel));

        int activityCount = Math.min(days, activities.size());
        for (int i = 0; i < activityCount; i++) {
            CatalogItemSummaryDTO activity = activities.get(i);
            BigDecimal activityPrice = (activity.price() != null ? activity.price() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(travelers));
            if (budget != null && spent.add(activityPrice).compareTo(budget) > 0) {
                break;
            }
            FavoriteListItem activityItem = FavoriteListItem.builder().catalogItemId(activity.id()).quantity(travelers)
                    .activityDate(arrivalDate.plusDays(i)).build();
            items.add(activityItem);
            resolved.add(new ResolvedItem(activityItem, activity));
            spent = spent.add(activityPrice);
        }

        if (wantReturnFlight) {
            addReturnFlightIfAffordable(items, resolved, from, arrivalCity, checkOut, travelers, budget, spent);
        }

        validateResolvedCoherence(resolved);

        FavoriteList list = FavoriteList.builder()
                .name("Viaggio a " + to)
                .ownerId(ownerId)
                .items(items)
                .build();
        return repository.save(list);
    }

    /**
     * Cerca un volo di ritorno (da arrivalCity a departureCity, dopo il checkout) e lo
     * aggiunge in coda alla lista se lo trova e il budget lo permette. Una seconda
     * ricerca dedicata invece di riusare i candidati già in mano: quelli includono solo
     * voli in ARRIVO alla destinazione, mai quelli in partenza da lì. Non lancia mai
     * un'eccezione: un ritorno non trovato (o troppo caro) non deve mai far fallire
     * un'intera generazione altrimenti riuscita.
     */
    private void addReturnFlightIfAffordable(List<FavoriteListItem> items, List<ResolvedItem> resolved,
                                              String originalDepartureCity, String arrivalCity,
                                              LocalDate checkOut, int travelers, BigDecimal budget, BigDecimal spent) {
        List<CatalogItemSummaryDTO> returnCandidates;
        try {
            returnCandidates = catalogClient.searchByDestination(originalDepartureCity, GENERATE_CANDIDATE_POOL).content();
        } catch (Exception e) {
            return;
        }
        Comparator<CatalogItemSummaryDTO> byPrice = Comparator.comparing(
                c -> c.price() != null ? c.price() : BigDecimal.ZERO);
        CatalogItemSummaryDTO returnFlight = returnCandidates.stream()
                .filter(c -> "Flight".equals(c.itemType()) && c.departureCity() != null && c.arrivalCity() != null && c.departureTime() != null
                        && arrivalCity.equalsIgnoreCase(c.departureCity())
                        && c.arrivalCity().trim().equalsIgnoreCase(originalDepartureCity)
                        && !c.departureTime().toLocalDate().isBefore(checkOut)
                        && flightHasCapacityFor(c, travelers))
                .sorted(byPrice)
                .findFirst()
                .orElse(null);
        if (returnFlight == null) return;

        CatalogItemSummaryDTO.FareClassSummaryDTO returnFare = cheapestFareClassFor(returnFlight, travelers);
        BigDecimal returnUnitPrice = returnFare != null
                ? (returnFare.price() != null ? returnFare.price() : BigDecimal.ZERO)
                : (returnFlight.price() != null ? returnFlight.price() : BigDecimal.ZERO);
        BigDecimal returnTotalPrice = returnUnitPrice.multiply(BigDecimal.valueOf(travelers));
        if (budget != null && spent.add(returnTotalPrice).compareTo(budget) > 0) return;

        FavoriteListItem returnItem = FavoriteListItem.builder().catalogItemId(returnFlight.id()).quantity(travelers)
                .fareClassId(returnFare != null ? returnFare.id() : null).build();
        items.add(returnItem);
        resolved.add(new ResolvedItem(returnItem, returnFlight));
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
    @Transactional
    public RemoveItemResultDTO removeItemFromList(Long listId, int index, String requesterId) {
        FavoriteList list = getEditableList(listId, requesterId);
        if (index < 0 || index >= list.getItems().size()) {
            throw new IllegalArgumentException("Componente non trovato in questa lista");
        }
        list.getItems().remove(index);

        List<FavoriteListItem> items = list.getItems();
        // Risolti una sola volta, poi riusati sia per trovare il prefisso coerente
        // (longestCoherentPrefix) sia per i titoli di alsoRemoved: senza questo, ogni
        // lunghezza di prefisso provata rifarebbe le stesse chiamate a catalog-service.
        List<ResolvedItem> resolved = resolveItems(items);
        int keep = longestCoherentPrefix(resolved);
        List<String> alsoRemoved = resolved.subList(keep, resolved.size()).stream()
                .map(r -> r.catalog().title())
                .toList();
        while (items.size() > keep) {
            items.remove(items.size() - 1);
        }

        repository.save(list);
        return new RemoveItemResultDTO(alsoRemoved);
    }

    /**
     * Come longestCoherentPrefix, ma su una lista già risolta (vedi resolveItems):
     * evita di richiamare catalog-service una volta per ogni lunghezza di prefisso
     * provata, che per una lista di M componenti farebbe fino a M+(M-1)+...+1 chiamate.
     */
    private int longestCoherentPrefix(List<ResolvedItem> resolved) {
        for (int k = resolved.size(); k > 0; k--) {
            try {
                validateResolvedCoherence(resolved.subList(0, k));
                return k;
            } catch (IllegalArgumentException e) {
                // prefisso ancora troppo lungo, proviamo quello più corto
            }
        }
        return 0;
    }

    /**
     * Esporta la lista in un file .ics: un VEVENT per volo (orario reale di partenza/
     * arrivo), uno per il soggiorno hotel e uno per attività (entrambi all-day, solo
     * la data). Riusa la stessa risoluzione dei componenti usata per la coerenza e il
     * prezzo, quindi un item senza le date necessarie viene semplicemente saltato
     * invece di far fallire l'intera esportazione.
     */
    public record IcsExport(String content, String listName) {}

    public IcsExport exportToIcs(Long listId, String requesterId) {
        FavoriteList list = getAccessibleById(listId, requesterId);
        List<ResolvedItem> resolved = resolveItems(list.getItems());

        List<CalendarEventDTO> events = new ArrayList<>();
        for (int i = 0; i < resolved.size(); i++) {
            ResolvedItem r = resolved.get(i);
            CatalogItemSummaryDTO catalog = r.catalog();
            FavoriteListItem source = r.source();
            // Basato sulla posizione della tappa nella lista (stabile: FavoriteList.items
            // ha un @OrderColumn dedicato), non su un id proprio: FavoriteListItem è
            // @Embeddable, non ha una riga/id suo. Riesportando lo stesso itinerario
            // invariato l'uid resta identico, quindi il calendario aggiorna l'evento
            // già importato invece di duplicarlo.
            String uid = "itinerario-" + listId + "-" + i;

            if ("Flight".equals(catalog.itemType())) {
                if (catalog.departureTime() == null || catalog.arrivalTime() == null) continue;
                events.add(new CalendarEventDTO(
                        uid,
                        "Volo " + catalog.departureCity() + " → " + catalog.arrivalCity(),
                        catalog.departureCity(),
                        catalog.departureTime(),
                        catalog.arrivalTime(),
                        false
                ));
            } else if ("Hotel".equals(catalog.itemType())) {
                if (source.getCheckIn() == null || source.getCheckOut() == null) continue;
                events.add(new CalendarEventDTO(
                        uid,
                        "Soggiorno: " + catalog.title(),
                        catalog.city(),
                        source.getCheckIn().atStartOfDay(),
                        source.getCheckOut().atStartOfDay(),
                        true
                ));
            } else if ("Activity".equals(catalog.itemType())) {
                if (source.getActivityDate() == null) continue;
                LocalDate date = source.getActivityDate();
                events.add(new CalendarEventDTO(
                        uid,
                        catalog.title(),
                        catalog.city(),
                        date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay(),
                        true
                ));
            }
        }

        return new IcsExport(IcsBuilder.build(list.getName(), events), list.getName());
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
        validateResolvedCoherence(resolveItems(items));
    }

    /** Stessa validazione di validateItineraryCoherence, su componenti già risolti (vedi resolveItems). */
    private void validateResolvedCoherence(List<ResolvedItem> resolved) {
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
        if (userIdToShareWith.equals(requesterId)) {
            throw new IllegalArgumentException("Non puoi condividere una lista con te stesso");
        }
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

        // Una lista non propria/condivisa entra qui solo perché ci è stato messo like: se
        // nel frattempo il proprietario l'ha resa di nuovo privata, non deve più comparire
        // (il like resta registrato, ma smette di dare visibilità sui "Salvati").
        List<Long> likedListIds = likeRepository.findByUserId(userId).stream().map(FavoriteListLike::getListId).toList();
        if (!likedListIds.isEmpty()) {
            for (FavoriteList list : repository.findAllById(likedListIds)) {
                if (list.getVisibility() == Visibility.PUBLIC) {
                    merged.putIfAbsent(list.getId(), list);
                }
            }
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

    /**
     * Dettaglio di una lista: visibile se pubblica, o se il richiedente è
     * proprietario/condivisa con lui. Stesso "non trovato" (404) sia per un id
     * inesistente sia per uno esistente ma non accessibile (vedi toggleLike): un 403
     * qui rivelerebbe a chiunque abbia un JWT quali id di liste private/condivise
     * altrui esistono davvero, semplicemente provando id in sequenza.
     */
    public FavoriteList getAccessibleById(Long listId, String requesterId) {
        FavoriteList list = getById(listId);
        boolean allowed = list.getVisibility() == Visibility.PUBLIC
                || list.getOwnerId().equals(requesterId)
                || list.getSharedUserIds().contains(requesterId);
        if (!allowed) {
            throw new ListNotFoundException(listId);
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
     * Il link di condivisione è un controllo indipendente dalla visibilità (vedi
     * enableLinkSharing/disableLinkSharing): diventare pubblici ne genera uno se manca,
     * ma tornare privati non lo disattiva.
     */
    @Transactional
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

    /**
     * Limite del feed pubblico: un endpoint anonimo non deve poter scaricare l'intera
     * tabella in una chiamata (vedi il Pageable passato alle query sotto).
     */
    private static final int PUBLIC_FEED_MAX_SIZE = 50;

    public List<FavoriteList> getPublicFeed(String city, String sort) {
        boolean byLikes = !"recent".equalsIgnoreCase(sort);
        Pageable page = PageRequest.of(0, PUBLIC_FEED_MAX_SIZE);
        if (city != null && !city.isBlank()) {
            return byLikes
                    ? repository.findByVisibilityAndCityIgnoreCaseOrderByLikesCountDesc(Visibility.PUBLIC, city.trim(), page)
                    : repository.findByVisibilityAndCityIgnoreCaseOrderByCreatedAtDesc(Visibility.PUBLIC, city.trim(), page);
        }
        return byLikes
                ? repository.findByVisibilityOrderByLikesCountDesc(Visibility.PUBLIC, page)
                : repository.findByVisibilityOrderByCreatedAtDesc(Visibility.PUBLIC, page);
    }

    @Transactional
    public boolean toggleLike(Long listId, String userId) {
        FavoriteList list = getById(listId);
        if (list.getVisibility() != Visibility.PUBLIC) {
            // Stessa eccezione (404) del caso "non esiste": altrimenti un 400 qui
            // rivelerebbe a chiunque abbia un JWT che una lista privata/condivisa
            // altrui esiste davvero, solo provando a metterci like.
            throw new ListNotFoundException(listId);
        }
        if (list.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Non puoi mettere mi piace al tuo stesso itinerario");
        }
        var existing = likeRepository.findByListIdAndUserId(listId, userId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            repository.decrementLikesCount(listId);
            return false;
        }
        likeRepository.save(FavoriteListLike.builder().listId(listId).userId(userId).build());
        repository.incrementLikesCount(listId);
        return true;
    }

    /**
     * Incrementa il contatore "prenotazioni tentate" quando l'utente preme "prenota
     * tutto": è un contatore best-effort, non collegato all'esito reale del pagamento
     * in booking-service (che itinerary-service non tocca). Richiede lo stesso accesso
     * di lettura della lista (pubblica, propria o condivisa), altrimenti chiunque con
     * un JWT valido potrebbe gonfiare il contatore di un itinerario altrui a piacere.
     */
    @Transactional
    public void registerBookingAttempt(Long listId, String requesterId) {
        getAccessibleById(listId, requesterId);
        repository.incrementBookingsCount(listId);
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

        // Non un'autoinvocazione a registerBookingAttempt(): chiamare un metodo
        // @Transactional dalla stessa classe scavalca il proxy AOP di Spring, quindi
        // l'annotazione verrebbe semplicemente ignorata — incrementBookingsCount è una
        // query di UPDATE che senza una transazione davvero aperta lancia
        // TransactionRequiredException, facendo fallire l'intera richiesta (con status
        // 500) anche se ogni componente era già stato aggiunto al carrello sopra.
        // TransactionTemplate apre una transazione vera qui, breve e indipendente dal
        // ciclo di chiamate HTTP appena fatto (che quindi non la tiene aperta inutilmente).
        if (successCount > 0) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> repository.incrementBookingsCount(listId));
        }
        return new BookAllResultDTO(successCount, list.getItems().size(), errors);
    }

    /**
     * 404 per PRIVATE/SHARED (non 403): stessa ragione di getAccessibleById, un id
     * esistente ma non tuo non deve essere distinguibile da uno inesistente. Per una
     * lista PUBLIC invece l'esistenza è già nota (visibile via GET/feed pubblico), quindi
     * qui è corretto un 403 esplicito: un 404 sarebbe solo fuorviante, non protettivo.
     */
    private FavoriteList getOwnedList(Long listId, String requesterId) {
        FavoriteList list = getById(listId);
        if (!list.getOwnerId().equals(requesterId)) {
            if (list.getVisibility() == Visibility.PUBLIC) {
                throw new NotListOwnerException(listId);
            }
            throw new ListNotFoundException(listId);
        }
        return list;
    }

    /** Come getOwnedList, ma ammette anche i collaboratori (sharedUserIds) per le modifiche di contenuto. */
    private FavoriteList getEditableList(Long listId, String requesterId) {
        FavoriteList list = getById(listId);
        boolean canEdit = list.getOwnerId().equals(requesterId) || list.getSharedUserIds().contains(requesterId);
        if (!canEdit) {
            if (list.getVisibility() == Visibility.PUBLIC) {
                throw new NotListOwnerException(listId);
            }
            throw new ListNotFoundException(listId);
        }
        return list;
    }
}
