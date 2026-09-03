package com.tripify.itinerary_service.service;

import com.tripify.itinerary_service.client.BookingClient;
import com.tripify.itinerary_service.client.CatalogClient;
import com.tripify.itinerary_service.dto.AddListItemRequestDTO;
import com.tripify.itinerary_service.dto.BookAllResultDTO;
import com.tripify.itinerary_service.dto.CatalogItemSummaryDTO;
import com.tripify.itinerary_service.dto.CatalogSearchPageDTO;
import com.tripify.itinerary_service.dto.FavoriteListResponseDTO;
import com.tripify.itinerary_service.entity.FavoriteList;
import com.tripify.itinerary_service.entity.Visibility;
import com.tripify.itinerary_service.exception.ListNotFoundException;
import com.tripify.itinerary_service.exception.NotListOwnerException;
import com.tripify.itinerary_service.repository.FavoriteListRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Copre la validazione di coerenza geografica/temporale di un itinerario (mai
 * testata finora) e il nuovo link di condivisione indipendente dalla visibilità.
 * CatalogClient/BookingClient sono mock: qui interessa solo la logica di
 * itinerary-service, non le chiamate verso gli altri microservizi.
 */
@DataJpaTest
@Import(ItineraryService.class)
@ActiveProfiles("test")
class ItineraryServiceTest {

    @Autowired
    private ItineraryService itineraryService;
    @Autowired
    private FavoriteListRepository repository;

    @MockBean
    private CatalogClient catalogClient;
    @MockBean
    private BookingClient bookingClient;

    private static final String OWNER = "owner-1";
    private static final String OTHER_USER = "other-user";

    private CatalogItemSummaryDTO flight(Long id, String from, String to, LocalDateTime dep, LocalDateTime arr) {
        return new CatalogItemSummaryDTO(id, "Flight", "Volo " + from + "-" + to, BigDecimal.valueOf(100),
                null, from, to, dep, arr, null, null);
    }

    private CatalogItemSummaryDTO hotel(Long id, String city) {
        return new CatalogItemSummaryDTO(id, "Hotel", "Hotel a " + city, BigDecimal.valueOf(80),
                city, null, null, null, null, null, null);
    }

    private CatalogItemSummaryDTO activity(Long id, String city, BigDecimal price) {
        return new CatalogItemSummaryDTO(id, "Activity", "Attività a " + city, price,
                city, null, null, null, null, null, null);
    }

    private FavoriteList newList() {
        return repository.save(FavoriteList.builder().name("Viaggio di test").ownerId(OWNER).build());
    }

    private AddListItemRequestDTO itemRequest(Long catalogItemId) {
        return new AddListItemRequestDTO(catalogItemId, 1, null, null, null, null, null);
    }

    private AddListItemRequestDTO hotelRequest(Long catalogItemId, LocalDate checkIn, LocalDate checkOut) {
        return new AddListItemRequestDTO(catalogItemId, 1, null, null, checkIn, checkOut, null);
    }

    private AddListItemRequestDTO activityRequest(Long catalogItemId, LocalDate activityDate) {
        return new AddListItemRequestDTO(catalogItemId, 1, null, null, null, null, activityDate);
    }

    @Test
    void aggiungeUnHotelNellaCittaGiustaDopoIlVolo() {
        FavoriteList list = newList();
        LocalDateTime dep = LocalDateTime.now().plusDays(5);
        LocalDateTime arr = dep.plusHours(10);
        when(catalogClient.getItem(1L)).thenReturn(flight(1L, "Roma", "New York", dep, arr));
        when(catalogClient.getItem(2L)).thenReturn(hotel(2L, "New York"));

        itineraryService.addItemToList(list.getId(), itemRequest(1L), OWNER);
        itineraryService.addItemToList(list.getId(), hotelRequest(2L, arr.toLocalDate(), arr.toLocalDate().plusDays(3)), OWNER);

        assertThat(itineraryService.getById(list.getId()).getItems()).hasSize(2);
    }

    @Test
    void rifiutaUnHotelInUnaCittaDiversaDaQuellaDiArrivo() {
        FavoriteList list = newList();
        LocalDateTime dep = LocalDateTime.now().plusDays(5);
        LocalDateTime arr = dep.plusHours(10);
        when(catalogClient.getItem(1L)).thenReturn(flight(1L, "Roma", "New York", dep, arr));
        when(catalogClient.getItem(2L)).thenReturn(hotel(2L, "Venezia"));
        itineraryService.addItemToList(list.getId(), itemRequest(1L), OWNER);

        assertThatThrownBy(() ->
                itineraryService.addItemToList(list.getId(), hotelRequest(2L, arr.toLocalDate(), arr.toLocalDate().plusDays(1)), OWNER)
        ).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Venezia");
    }

    @Test
    void rifiutaUnSecondoVoloCheNonPartaDallaCittaDiArrivoDelPrimo() {
        FavoriteList list = newList();
        LocalDateTime dep1 = LocalDateTime.now().plusDays(5);
        LocalDateTime arr1 = dep1.plusHours(10);
        when(catalogClient.getItem(1L)).thenReturn(flight(1L, "Roma", "New York", dep1, arr1));
        when(catalogClient.getItem(2L)).thenReturn(flight(2L, "Toronto", "Roma", arr1.plusDays(3), arr1.plusDays(3).plusHours(9)));
        itineraryService.addItemToList(list.getId(), itemRequest(1L), OWNER);

        assertThatThrownBy(() -> itineraryService.addItemToList(list.getId(), itemRequest(2L), OWNER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rifiutaUnHotelPrimaDiQualsiasiVolo() {
        FavoriteList list = newList();
        when(catalogClient.getItem(1L)).thenReturn(hotel(1L, "Roma"));

        assertThatThrownBy(() ->
                itineraryService.addItemToList(list.getId(), hotelRequest(1L, LocalDate.now(), LocalDate.now().plusDays(1)), OWNER)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void soloIlProprietarioPuoRegistrareUnTentativoDiPrenotazioneSuUnaListaPrivata() {
        FavoriteList list = newList(); // PRIVATE di default

        assertThatThrownBy(() -> itineraryService.registerBookingAttempt(list.getId(), OTHER_USER))
                .isInstanceOf(ListNotFoundException.class);

        itineraryService.registerBookingAttempt(list.getId(), OWNER);
        assertThat(itineraryService.getById(list.getId()).getBookingsCount()).isEqualTo(1);
    }

    @Test
    void prenotareTuttoAggiungeIComponentiAlCarrelloEIncrementaIlContatore() {
        FavoriteList list = newList();
        LocalDateTime dep = LocalDateTime.now().plusDays(5);
        LocalDateTime arr = dep.plusHours(2);
        when(catalogClient.getItem(1L)).thenReturn(flight(1L, "Milano", "Roma", dep, arr));
        when(catalogClient.getItem(2L)).thenReturn(hotel(2L, "Roma"));
        itineraryService.addItemToList(list.getId(), itemRequest(1L), OWNER);
        itineraryService.addItemToList(list.getId(), hotelRequest(2L, arr.toLocalDate(), arr.toLocalDate().plusDays(3)), OWNER);

        // Il contatore va incrementato con una transazione vera e propria, non tramite
        // un'autoinvocazione a registerBookingAttempt() (ignorerebbe il suo @Transactional,
        // vedi ItineraryService.bookAllItems): senza il fix, questa riga lancia
        // TransactionRequiredException anche se ogni componente è già stato aggiunto
        // al carrello sopra.
        BookAllResultDTO result = itineraryService.bookAllItems(list.getId(), OWNER, "fake-jwt");

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.errors()).isEmpty();
        verify(bookingClient, org.mockito.Mockito.times(2)).addToCart(eq("Bearer fake-jwt"), any());
        assertThat(itineraryService.getById(list.getId()).getBookingsCount()).isEqualTo(1);
    }

    @Test
    void ilLinkDiCondivisioneFunzionaAncheSuUnaListaPrivataSenzaRequisitiMinimi() {
        FavoriteList list = newList();

        FavoriteList withLink = itineraryService.enableLinkSharing(list.getId(), OWNER);

        assertThat(withLink.getPublicToken()).isNotBlank();
        assertThat(withLink.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(itineraryService.getByPublicToken(withLink.getPublicToken()).getId()).isEqualTo(list.getId());
    }

    @Test
    void attivareIlLinkDueVolteNonCambiaIlToken() {
        FavoriteList list = newList();
        String firstToken = itineraryService.enableLinkSharing(list.getId(), OWNER).getPublicToken();

        String secondToken = itineraryService.enableLinkSharing(list.getId(), OWNER).getPublicToken();

        assertThat(secondToken).isEqualTo(firstToken);
    }

    @Test
    void disattivareIlLinkLoRendeInutilizzabile() {
        FavoriteList list = newList();
        String token = itineraryService.enableLinkSharing(list.getId(), OWNER).getPublicToken();

        itineraryService.disableLinkSharing(list.getId(), OWNER);

        assertThatThrownBy(() -> itineraryService.getByPublicToken(token)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void chiUsaIlLinkDiInvitoDiventaCollaboratoreConDirittoDiModifica() {
        FavoriteList list = newList(); // PRIVATE di default
        String token = itineraryService.enableCollabInvite(list.getId(), OWNER).getCollabToken();

        FavoriteList joined = itineraryService.joinAsCollaborator(token, OTHER_USER);

        assertThat(joined.getSharedUserIds()).contains(OTHER_USER);
        assertThat(joined.getVisibility()).isEqualTo(Visibility.SHARED);

        // Ora può davvero modificare la lista, non solo vederla.
        when(catalogClient.getItem(1L)).thenReturn(hotel(1L, "Roma"));
        // Orario fisso (non LocalDateTime.now()): un volo che parte a ridosso di
        // mezzanotte farebbe atterrare dep.plusHours(1) il giorno dopo, mentre il
        // check-in sotto resta sul giorno di dep, facendo fallire il test solo se
        // eseguito nell'ultima ora del giorno.
        LocalDateTime dep = LocalDate.now().plusDays(2).atTime(10, 0);
        when(catalogClient.getItem(2L)).thenReturn(flight(2L, "Milano", "Roma", dep, dep.plusHours(1)));
        itineraryService.addItemToList(list.getId(), itemRequest(2L), OWNER);
        itineraryService.addItemToList(list.getId(), hotelRequest(1L, dep.toLocalDate(), dep.toLocalDate().plusDays(2)), OTHER_USER);

        assertThat(itineraryService.getById(list.getId()).getItems()).hasSize(2);
    }

    @Test
    void ilCollaboratoreNonVedeIlTokenDiInvitoNellaListaDelProprietario() {
        FavoriteList list = newList();
        String token = itineraryService.enableCollabInvite(list.getId(), OWNER).getCollabToken();
        itineraryService.joinAsCollaborator(token, OTHER_USER);

        FavoriteList reloaded = itineraryService.getById(list.getId());
        FavoriteListResponseDTO ownerView = FavoriteListResponseDTO.forRequester(reloaded, OWNER);
        FavoriteListResponseDTO collaboratorView = FavoriteListResponseDTO.forRequester(reloaded, OTHER_USER);

        assertThat(ownerView.collabToken()).isEqualTo(token);
        assertThat(collaboratorView.collabToken()).isNull();
    }

    @Test
    void nonSiPuoCondividereUnaListaConSeStessi() {
        FavoriteList list = newList();

        assertThatThrownBy(() -> itineraryService.shareList(list.getId(), OWNER, OWNER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ilLikeAUnaListaPrivataDaLoStessoErroreDiUnaListaInesistente() {
        FavoriteList list = newList(); // PRIVATE di default

        assertThatThrownBy(() -> itineraryService.toggleLike(list.getId(), OTHER_USER))
                .isInstanceOf(ListNotFoundException.class);
        assertThatThrownBy(() -> itineraryService.toggleLike(999999L, OTHER_USER))
                .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void unTokenDiInvitoInventatoNonFunziona() {
        assertThatThrownBy(() -> itineraryService.joinAsCollaborator("token-inesistente", OTHER_USER))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void chiNonEProprietarioNeCollaboratoreNonPuoAggiungereComponenti() {
        FavoriteList list = newList();
        when(catalogClient.getItem(1L)).thenReturn(hotel(1L, "Roma"));

        assertThatThrownBy(() ->
                itineraryService.addItemToList(list.getId(), hotelRequest(1L, LocalDate.now(), LocalDate.now().plusDays(1)), OTHER_USER)
        ).isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void soloIlProprietarioPuoRinominareLaLista() {
        FavoriteList list = newList();

        FavoriteList renamed = itineraryService.renameList(list.getId(), "Nuovo nome", OWNER);
        assertThat(renamed.getName()).isEqualTo("Nuovo nome");

        assertThatThrownBy(() -> itineraryService.renameList(list.getId(), "Altro nome", OTHER_USER))
                .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void modificareUnaListaPubblicaAltruiDaUn403NonUn404() {
        FavoriteList list = newList();
        LocalDateTime dep1 = LocalDateTime.now().plusDays(5);
        LocalDateTime arr1 = dep1.plusHours(10);
        LocalDate checkIn = arr1.toLocalDate();
        LocalDate checkOut = checkIn.plusDays(3);
        LocalDateTime dep2 = checkOut.atTime(20, 0);
        LocalDateTime arr2 = dep2.plusHours(9);
        when(catalogClient.getItem(1L)).thenReturn(flight(1L, "Roma", "New York", dep1, arr1));
        when(catalogClient.getItem(2L)).thenReturn(hotel(2L, "New York"));
        when(catalogClient.getItem(3L)).thenReturn(activity(3L, "New York", BigDecimal.valueOf(40)));
        when(catalogClient.getItem(4L)).thenReturn(flight(4L, "New York", "Roma", dep2, arr2));
        itineraryService.addItemToList(list.getId(), itemRequest(1L), OWNER);
        itineraryService.addItemToList(list.getId(), hotelRequest(2L, checkIn, checkOut), OWNER);
        itineraryService.addItemToList(list.getId(), activityRequest(3L, checkIn.plusDays(1)), OWNER);
        itineraryService.addItemToList(list.getId(), itemRequest(4L), OWNER);
        itineraryService.setVisibility(list.getId(), Visibility.PUBLIC, "New York", OWNER);

        // L'esistenza della lista è già nota (è pubblica): un 404 qui sarebbe fuorviante,
        // non protettivo, a differenza del caso PRIVATE/SHARED (vedi getOwnedList).
        assertThatThrownBy(() -> itineraryService.renameList(list.getId(), "Altro nome", OTHER_USER))
                .isInstanceOf(NotListOwnerException.class);
    }

    @Test
    void esportaUnVoloEUnHotelInFormatoIcsValido() {
        FavoriteList list = newList();
        LocalDateTime dep = LocalDateTime.of(2026, 9, 10, 8, 0);
        LocalDateTime arr = LocalDateTime.of(2026, 9, 10, 18, 0);
        when(catalogClient.getItem(1L)).thenReturn(flight(1L, "Roma", "New York", dep, arr));
        when(catalogClient.getItem(2L)).thenReturn(hotel(2L, "New York"));
        itineraryService.addItemToList(list.getId(), itemRequest(1L), OWNER);
        itineraryService.addItemToList(list.getId(), hotelRequest(2L, arr.toLocalDate(), arr.toLocalDate().plusDays(3)), OWNER);

        String ics = itineraryService.exportToIcs(list.getId(), OWNER).content();

        assertThat(ics).startsWith("BEGIN:VCALENDAR");
        assertThat(ics).endsWith("END:VCALENDAR\r\n");
        assertThat(countOccurrences(ics, "BEGIN:VEVENT")).isEqualTo(2);
        assertThat(ics).contains("SUMMARY:Volo Roma → New York");
        assertThat(ics).contains("DTSTART:20260910T080000");
        assertThat(ics).contains("DTEND:20260910T180000");
        assertThat(ics).contains("SUMMARY:Soggiorno: Hotel a New York");
        assertThat(ics).contains("DTSTART;VALUE=DATE:20260910");
        assertThat(ics).contains("DTEND;VALUE=DATE:20260913");
    }

    @Test
    void esportareLoStessoItinerarioDueVolteProduceGliStessiUid() {
        FavoriteList list = newList();
        LocalDateTime dep = LocalDateTime.of(2026, 9, 10, 8, 0);
        LocalDateTime arr = LocalDateTime.of(2026, 9, 10, 18, 0);
        when(catalogClient.getItem(1L)).thenReturn(flight(1L, "Roma", "New York", dep, arr));
        when(catalogClient.getItem(2L)).thenReturn(hotel(2L, "New York"));
        itineraryService.addItemToList(list.getId(), itemRequest(1L), OWNER);
        itineraryService.addItemToList(list.getId(), hotelRequest(2L, arr.toLocalDate(), arr.toLocalDate().plusDays(3)), OWNER);

        String firstExport = itineraryService.exportToIcs(list.getId(), OWNER).content();
        String secondExport = itineraryService.exportToIcs(list.getId(), OWNER).content();

        // Prima del fix: UID:UUID.randomUUID() ad ogni chiamata -> reimportando lo
        // stesso itinerario invariato, il calendario creava eventi duplicati invece
        // di aggiornare quelli già importati.
        assertThat(firstExport).isEqualTo(secondExport);
        assertThat(firstExport).contains("UID:itinerario-" + list.getId() + "-0@tripify.app");
        assertThat(firstExport).contains("UID:itinerario-" + list.getId() + "-1@tripify.app");
    }

    @Test
    void nonPuoEsportareIlCalendarioDiUnaListaAltrui() {
        FavoriteList list = newList(); // PRIVATE di default

        assertThatThrownBy(() -> itineraryService.exportToIcs(list.getId(), OTHER_USER))
                .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void clonaUnItinerarioPubblicoCopiaIComponentiInUnaNuovaListaPrivata() {
        FavoriteList source = newList();
        LocalDateTime dep1 = LocalDateTime.now().plusDays(5);
        LocalDateTime arr1 = dep1.plusHours(10);
        LocalDate checkIn = arr1.toLocalDate();
        LocalDate checkOut = checkIn.plusDays(3);
        LocalDateTime dep2 = checkOut.atTime(20, 0);
        LocalDateTime arr2 = dep2.plusHours(9);

        // Servono almeno 2 voli, 1 hotel e 1 attività per poter pubblicare (vedi
        // validatePublishRequirements): un itinerario completo andata/soggiorno/ritorno.
        when(catalogClient.getItem(1L)).thenReturn(flight(1L, "Roma", "New York", dep1, arr1));
        when(catalogClient.getItem(2L)).thenReturn(hotel(2L, "New York"));
        when(catalogClient.getItem(3L)).thenReturn(activity(3L, "New York", BigDecimal.valueOf(40)));
        when(catalogClient.getItem(4L)).thenReturn(flight(4L, "New York", "Roma", dep2, arr2));
        itineraryService.addItemToList(source.getId(), itemRequest(1L), OWNER);
        itineraryService.addItemToList(source.getId(), hotelRequest(2L, checkIn, checkOut), OWNER);
        itineraryService.addItemToList(source.getId(), activityRequest(3L, checkIn.plusDays(1)), OWNER);
        itineraryService.addItemToList(source.getId(), itemRequest(4L), OWNER);
        itineraryService.setVisibility(source.getId(), Visibility.PUBLIC, "New York", OWNER);

        FavoriteList clone = itineraryService.cloneList(source.getId(), OTHER_USER);

        assertThat(clone.getOwnerId()).isEqualTo(OTHER_USER);
        assertThat(clone.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(clone.getName()).isEqualTo("Copia di " + source.getName());
        assertThat(clone.getItems()).hasSize(4);
        assertThat(clone.getItems().get(0).getCatalogItemId()).isEqualTo(1L);
        assertThat(clone.getItems().get(1).getCheckIn()).isEqualTo(checkIn);
        assertThat(clone.getItems().get(2).getActivityDate()).isEqualTo(checkIn.plusDays(1));

        // L'originale non è toccato dalla clonazione.
        FavoriteList reloadedSource = itineraryService.getById(source.getId());
        assertThat(reloadedSource.getOwnerId()).isEqualTo(OWNER);
        assertThat(reloadedSource.getItems()).hasSize(4);
    }

    @Test
    void nonSiPuoClonareUnaListaPrivataAltrui() {
        FavoriteList list = newList(); // PRIVATE di default

        assertThatThrownBy(() -> itineraryService.cloneList(list.getId(), OTHER_USER))
                .isInstanceOf(ListNotFoundException.class);
    }

    @Test
    void generaUnItinerarioConVoloHotelEAttivitaCoerenti() {
        LocalDateTime arr = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0);
        LocalDateTime dep = arr.minusHours(10);
        CatalogSearchPageDTO page = new CatalogSearchPageDTO(List.of(
                flight(1L, "Milano", "Roma", dep, arr),
                hotel(2L, "Roma"),
                activity(3L, "Roma", BigDecimal.valueOf(20)),
                activity(4L, "Roma", BigDecimal.valueOf(30))
        ));
        when(catalogClient.searchByDestination(eq("Roma"), anyInt())).thenReturn(page);

        FavoriteList generated = itineraryService.generateItinerary("Milano", "Roma", 2, 1, false, null, OWNER);

        assertThat(generated.getOwnerId()).isEqualTo(OWNER);
        assertThat(generated.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(generated.getItems()).hasSize(4); // volo + hotel + 2 attività (min(days=2, 2 disponibili))
        assertThat(generated.getItems().get(0).getCatalogItemId()).isEqualTo(1L);
        assertThat(generated.getItems().get(1).getCatalogItemId()).isEqualTo(2L);
        assertThat(generated.getItems().get(1).getCheckIn()).isEqualTo(arr.toLocalDate());
        assertThat(generated.getItems().get(1).getCheckOut()).isEqualTo(arr.toLocalDate().plusDays(2));
        assertThat(generated.getItems().get(2).getActivityDate()).isEqualTo(arr.toLocalDate());
        assertThat(generated.getItems().get(3).getActivityDate()).isEqualTo(arr.toLocalDate().plusDays(1));

        // Il risultato deve gia' essere una lista coerente e riletta senza errori.
        assertThat(itineraryService.getById(generated.getId()).getItems()).hasSize(4);
    }

    @Test
    void generaUnItinerarioFallisceSenzaVoliPerLaDestinazione() {
        when(catalogClient.searchByDestination(eq("Atlantide"), anyInt()))
                .thenReturn(new CatalogSearchPageDTO(List.of(hotel(1L, "Atlantide"))));

        assertThatThrownBy(() -> itineraryService.generateItinerary("Milano", "Atlantide", 3, 1, false, null, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volo");
    }

    @Test
    void generaUnItinerarioIgnoraIVoliChePartonoDaUnAltraCitta() {
        LocalDateTime arr = LocalDateTime.now().plusDays(3);
        CatalogSearchPageDTO page = new CatalogSearchPageDTO(List.of(
                flight(1L, "Napoli", "Roma", arr.minusHours(2), arr), // parte dalla città sbagliata
                hotel(2L, "Roma")
        ));
        when(catalogClient.searchByDestination(eq("Roma"), anyInt())).thenReturn(page);

        assertThatThrownBy(() -> itineraryService.generateItinerary("Milano", "Roma", 3, 1, false, null, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Milano")
                .hasMessageContaining("Roma");
    }

    @Test
    void generaUnItinerarioFallisceSenzaHotelNellaDestinazione() {
        LocalDateTime arr = LocalDateTime.now().plusDays(3);
        when(catalogClient.searchByDestination(eq("Roma"), anyInt()))
                .thenReturn(new CatalogSearchPageDTO(List.of(flight(1L, "Milano", "Roma", arr.minusHours(2), arr))));

        assertThatThrownBy(() -> itineraryService.generateItinerary("Milano", "Roma", 3, 1, false, null, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hotel");
    }

    @Test
    void generaUnItinerarioRispettaIlBudgetSaltandoLeAttivitaPiuCostose() {
        LocalDateTime arr = LocalDateTime.now().plusDays(3);
        CatalogSearchPageDTO page = new CatalogSearchPageDTO(List.of(
                flight(1L, "Milano", "Roma", arr.minusHours(2), arr), // prezzo base 100
                hotel(2L, "Roma"), // prezzo base 80/notte
                activity(3L, "Roma", BigDecimal.valueOf(500))
        ));
        when(catalogClient.searchByDestination(eq("Roma"), anyInt())).thenReturn(page);

        // Volo (100) + hotel per 1 notte (80) = 180: il budget di 200 li copre ma non
        // lascia spazio per l'attività da 500.
        FavoriteList generated = itineraryService.generateItinerary("Milano", "Roma", 1, 1, false, BigDecimal.valueOf(200), OWNER);

        assertThat(generated.getItems()).hasSize(2); // solo volo + hotel, l'attività è stata scartata
    }

    @Test
    void generaUnItinerarioFallisceSeIlBudgetNonCopreVoloEHotel() {
        LocalDateTime arr = LocalDateTime.now().plusDays(3);
        CatalogSearchPageDTO page = new CatalogSearchPageDTO(List.of(
                flight(1L, "Milano", "Roma", arr.minusHours(2), arr), // 100
                hotel(2L, "Roma") // 80/notte
        ));
        when(catalogClient.searchByDestination(eq("Roma"), anyInt())).thenReturn(page);

        assertThatThrownBy(() -> itineraryService.generateItinerary("Milano", "Roma", 1, 1, false, BigDecimal.valueOf(50), OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("budget");
    }

    @Test
    void generaUnItinerarioSceglieLaTariffaELaCameraDavveroPiuEconomica() {
        LocalDateTime arr = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0);
        LocalDateTime dep = arr.minusHours(10);
        // Business (id 10) e' il PRIMO elemento della lista ma NON e' il piu' economico:
        // prima del fix veniva scelto solo perche' primo in lista (senza @OrderBy lato
        // catalog-service, l'ordine di ritorno di Hibernate non e' garantito per prezzo).
        CatalogItemSummaryDTO flightWithFares = new CatalogItemSummaryDTO(
                1L, "Flight", "Volo Milano-Roma", BigDecimal.valueOf(100), null, "Milano", "Roma", dep, arr,
                null,
                List.of(
                        new CatalogItemSummaryDTO.FareClassSummaryDTO(10L, BigDecimal.valueOf(500), 50),
                        new CatalogItemSummaryDTO.FareClassSummaryDTO(11L, BigDecimal.valueOf(100), 50)
                )
        );
        CatalogItemSummaryDTO hotelWithRooms = new CatalogItemSummaryDTO(
                2L, "Hotel", "Hotel a Roma", BigDecimal.valueOf(200), "Roma", null, null, null, null,
                List.of(
                        new CatalogItemSummaryDTO.RoomTypeSummaryDTO(20L, BigDecimal.valueOf(200), 4),
                        new CatalogItemSummaryDTO.RoomTypeSummaryDTO(21L, BigDecimal.valueOf(80), 4)
                ),
                null
        );
        when(catalogClient.searchByDestination(eq("Roma"), anyInt()))
                .thenReturn(new CatalogSearchPageDTO(List.of(flightWithFares, hotelWithRooms)));

        FavoriteList generated = itineraryService.generateItinerary("Milano", "Roma", 1, 1, false, null, OWNER);

        assertThat(generated.getItems().get(0).getFareClassId()).isEqualTo(11L);
        assertThat(generated.getItems().get(1).getRoomTypeId()).isEqualTo(21L);
    }

    @Test
    void generaUnItinerarioPerPiuViaggiatoriScartaUnaTariffaConPostiInsufficienti() {
        LocalDateTime arr = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0);
        LocalDateTime dep = arr.minusHours(10);
        CatalogItemSummaryDTO flightWithFares = new CatalogItemSummaryDTO(
                1L, "Flight", "Volo Milano-Roma", BigDecimal.valueOf(80), null, "Milano", "Roma", dep, arr,
                null,
                List.of(
                        new CatalogItemSummaryDTO.FareClassSummaryDTO(10L, BigDecimal.valueOf(80), 2), // troppo pochi posti per 4 persone
                        new CatalogItemSummaryDTO.FareClassSummaryDTO(11L, BigDecimal.valueOf(150), 10)
                )
        );
        when(catalogClient.searchByDestination(eq("Roma"), anyInt()))
                .thenReturn(new CatalogSearchPageDTO(List.of(flightWithFares, hotel(2L, "Roma"))));

        FavoriteList generated = itineraryService.generateItinerary("Milano", "Roma", 1, 4, false, null, OWNER);

        assertThat(generated.getItems().get(0).getFareClassId()).isEqualTo(11L);
        assertThat(generated.getItems().get(0).getQuantity()).isEqualTo(4);
    }

    @Test
    void generaUnItinerarioPerPiuViaggiatoriSceglieLaCameraCheMinimizzaIlCostoTotale() {
        LocalDateTime arr = LocalDateTime.now().plusDays(3);
        CatalogItemSummaryDTO hotelWithRooms = new CatalogItemSummaryDTO(
                2L, "Hotel", "Hotel a Roma", BigDecimal.valueOf(80), "Roma", null, null, null, null,
                List.of(
                        new CatalogItemSummaryDTO.RoomTypeSummaryDTO(20L, BigDecimal.valueOf(80), 2), // 2 camere per 4 persone = 160/notte
                        new CatalogItemSummaryDTO.RoomTypeSummaryDTO(21L, BigDecimal.valueOf(100), 4) // 1 camera per 4 persone = 100/notte
                ),
                null
        );
        when(catalogClient.searchByDestination(eq("Roma"), anyInt()))
                .thenReturn(new CatalogSearchPageDTO(List.of(flight(1L, "Milano", "Roma", arr.minusHours(2), arr), hotelWithRooms)));

        FavoriteList generated = itineraryService.generateItinerary("Milano", "Roma", 1, 4, false, null, OWNER);

        assertThat(generated.getItems().get(1).getRoomTypeId()).isEqualTo(21L);
        assertThat(generated.getItems().get(1).getQuantity()).isEqualTo(1);
    }

    @Test
    void generaUnItinerarioConVoloDiRitornoRichiestoLoAggiungeInCoda() {
        LocalDateTime arr = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0);
        LocalDateTime dep = arr.minusHours(10);
        when(catalogClient.searchByDestination(eq("Roma"), anyInt()))
                .thenReturn(new CatalogSearchPageDTO(List.of(flight(1L, "Milano", "Roma", dep, arr), hotel(2L, "Roma"))));

        LocalDateTime returnDep = arr.toLocalDate().plusDays(1).atTime(18, 0);
        LocalDateTime returnArr = returnDep.plusHours(1);
        when(catalogClient.searchByDestination(eq("Milano"), anyInt()))
                .thenReturn(new CatalogSearchPageDTO(List.of(flight(3L, "Roma", "Milano", returnDep, returnArr))));

        FavoriteList generated = itineraryService.generateItinerary("Milano", "Roma", 1, 1, true, null, OWNER);

        assertThat(generated.getItems()).hasSize(3); // volo andata + hotel + volo ritorno
        assertThat(generated.getItems().get(2).getCatalogItemId()).isEqualTo(3L);
    }

    @Test
    void generaUnItinerarioSenzaVoloDiRitornoDisponibileNonFallisceLaGenerazione() {
        LocalDateTime arr = LocalDateTime.now().plusDays(3);
        when(catalogClient.searchByDestination(eq("Roma"), anyInt()))
                .thenReturn(new CatalogSearchPageDTO(List.of(flight(1L, "Milano", "Roma", arr.minusHours(2), arr), hotel(2L, "Roma"))));
        when(catalogClient.searchByDestination(eq("Milano"), anyInt()))
                .thenReturn(new CatalogSearchPageDTO(List.of()));

        FavoriteList generated = itineraryService.generateItinerary("Milano", "Roma", 1, 1, true, null, OWNER);

        assertThat(generated.getItems()).hasSize(2); // solo andata + hotel, nessun errore
    }

    @Test
    void clonareUnaListaEValorizzarneIlPrezzoDaLoStessoTotaleDellOriginale() {
        FavoriteList source = newList();
        LocalDateTime dep = LocalDateTime.now().plusDays(5);
        LocalDateTime arr = dep.plusHours(10);
        when(catalogClient.getItem(1L)).thenReturn(flight(1L, "Roma", "New York", dep, arr));
        when(catalogClient.getItem(2L)).thenReturn(hotel(2L, "New York"));
        itineraryService.addItemToList(source.getId(), itemRequest(1L), OWNER);
        itineraryService.addItemToList(source.getId(), hotelRequest(2L, arr.toLocalDate(), arr.toLocalDate().plusDays(2)), OWNER);

        FavoriteList clone = itineraryService.cloneList(source.getId(), OWNER);
        itineraryService.applyTotalPrice(clone); // stesso passo che ora fa il controller

        BigDecimal expectedTotal = itineraryService.computeTotalPrice(itineraryService.getById(source.getId()));
        assertThat(clone.getTotalPrice()).isEqualByComparingTo(expectedTotal);
        assertThat(clone.getTotalPrice()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void nonSiPuoMettereLikeAllaPropriaListaPubblica() {
        FavoriteList list = newList();
        LocalDateTime dep1 = LocalDateTime.now().plusDays(5);
        LocalDateTime arr1 = dep1.plusHours(10);
        LocalDate checkIn = arr1.toLocalDate();
        LocalDate checkOut = checkIn.plusDays(3);
        LocalDateTime dep2 = checkOut.atTime(20, 0);
        LocalDateTime arr2 = dep2.plusHours(9);
        when(catalogClient.getItem(1L)).thenReturn(flight(1L, "Roma", "New York", dep1, arr1));
        when(catalogClient.getItem(2L)).thenReturn(hotel(2L, "New York"));
        when(catalogClient.getItem(3L)).thenReturn(activity(3L, "New York", BigDecimal.valueOf(40)));
        when(catalogClient.getItem(4L)).thenReturn(flight(4L, "New York", "Roma", dep2, arr2));
        itineraryService.addItemToList(list.getId(), itemRequest(1L), OWNER);
        itineraryService.addItemToList(list.getId(), hotelRequest(2L, checkIn, checkOut), OWNER);
        itineraryService.addItemToList(list.getId(), activityRequest(3L, checkIn.plusDays(1)), OWNER);
        itineraryService.addItemToList(list.getId(), itemRequest(4L), OWNER);
        itineraryService.setVisibility(list.getId(), Visibility.PUBLIC, "New York", OWNER);

        assertThatThrownBy(() -> itineraryService.toggleLike(list.getId(), OWNER))
                .isInstanceOf(IllegalArgumentException.class);

        boolean liked = itineraryService.toggleLike(list.getId(), OTHER_USER);
        assertThat(liked).isTrue();
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0, index = 0;
        while ((index = text.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
