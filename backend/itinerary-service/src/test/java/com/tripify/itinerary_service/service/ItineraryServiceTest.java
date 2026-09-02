package com.tripify.itinerary_service.service;

import com.tripify.itinerary_service.client.BookingClient;
import com.tripify.itinerary_service.client.CatalogClient;
import com.tripify.itinerary_service.dto.AddListItemRequestDTO;
import com.tripify.itinerary_service.dto.CatalogItemSummaryDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private FavoriteList newList() {
        return repository.save(FavoriteList.builder().name("Viaggio di test").ownerId(OWNER).build());
    }

    private AddListItemRequestDTO itemRequest(Long catalogItemId) {
        return new AddListItemRequestDTO(catalogItemId, 1, null, null, null, null, null);
    }

    private AddListItemRequestDTO hotelRequest(Long catalogItemId, LocalDate checkIn, LocalDate checkOut) {
        return new AddListItemRequestDTO(catalogItemId, 1, null, null, checkIn, checkOut, null);
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
                .isInstanceOf(NotListOwnerException.class);

        itineraryService.registerBookingAttempt(list.getId(), OWNER);
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
        ).isInstanceOf(NotListOwnerException.class);
    }

    @Test
    void soloIlProprietarioPuoRinominareLaLista() {
        FavoriteList list = newList();

        FavoriteList renamed = itineraryService.renameList(list.getId(), "Nuovo nome", OWNER);
        assertThat(renamed.getName()).isEqualTo("Nuovo nome");

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
                .isInstanceOf(NotListOwnerException.class);
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
