package com.tripify.itinerary_service.controller;

import com.tripify.itinerary_service.dto.AddListItemRequestDTO;
import com.tripify.itinerary_service.dto.BookAllResultDTO;
import com.tripify.itinerary_service.dto.CreateListRequestDTO;
import com.tripify.itinerary_service.dto.FavoriteListResponseDTO;
import com.tripify.itinerary_service.dto.RemoveItemResultDTO;
import com.tripify.itinerary_service.dto.UpdateVisibilityRequestDTO;
import com.tripify.itinerary_service.entity.FavoriteList;
import com.tripify.itinerary_service.service.ItineraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService service;

    @PostMapping
    public ResponseEntity<FavoriteListResponseDTO> create(@Valid @RequestBody CreateListRequestDTO request,
                                                @AuthenticationPrincipal Jwt jwt) {
        FavoriteList created = service.createList(request.name(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(FavoriteListResponseDTO.forOwner(created));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<Void> addItem(@PathVariable Long id, @Valid @RequestBody AddListItemRequestDTO request,
                                         @AuthenticationPrincipal Jwt jwt) {
        service.addItemToList(id, request, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    /** Rimuove il componente in posizione {index} (0-based, stesso ordine mostrato nel dettaglio). */
    @DeleteMapping("/{id}/items/{index}")
    public ResponseEntity<RemoveItemResultDTO> removeItem(@PathVariable Long id, @PathVariable int index,
                                            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.removeItemFromList(id, index, jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteList(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.deleteList(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/share")
    public ResponseEntity<Void> share(@PathVariable Long id, @RequestParam String userId,
                                       @AuthenticationPrincipal Jwt jwt) {
        service.shareList(id, userId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    /**
     * Attiva il link di condivisione (capabilities), indipendentemente dalla visibilità:
     * funziona anche su una lista privata o condivisa, senza i requisiti di pubblicazione.
     */
    @PostMapping("/{id}/link")
    public ResponseEntity<FavoriteListResponseDTO> enableLinkSharing(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(FavoriteListResponseDTO.forOwner(service.enableLinkSharing(id, jwt.getSubject())));
    }

    @DeleteMapping("/{id}/link")
    public ResponseEntity<FavoriteListResponseDTO> disableLinkSharing(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(FavoriteListResponseDTO.forOwner(service.disableLinkSharing(id, jwt.getSubject())));
    }

    /** Link di invito: chi lo apre da loggato entra come collaboratore (vedi join sotto). */
    @PostMapping("/{id}/collab-link")
    public ResponseEntity<FavoriteListResponseDTO> enableCollabInvite(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(FavoriteListResponseDTO.forOwner(service.enableCollabInvite(id, jwt.getSubject())));
    }

    @DeleteMapping("/{id}/collab-link")
    public ResponseEntity<FavoriteListResponseDTO> disableCollabInvite(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(FavoriteListResponseDTO.forOwner(service.disableCollabInvite(id, jwt.getSubject())));
    }

    /** Chi chiama (autenticato, per via di .anyRequest().authenticated()) entra come collaboratore. */
    @PostMapping("/collab-link/{token}/join")
    public ResponseEntity<FavoriteListResponseDTO> joinAsCollaborator(@PathVariable String token, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(FavoriteListResponseDTO.forCollaborator(service.joinAsCollaborator(token, jwt.getSubject())));
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<FavoriteListResponseDTO> renameList(@PathVariable Long id,
                                                    @Valid @RequestBody CreateListRequestDTO request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(FavoriteListResponseDTO.forOwner(service.renameList(id, request.name(), jwt.getSubject())));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<FavoriteListResponseDTO>> getMyLists(@AuthenticationPrincipal Jwt jwt) {
        List<FavoriteList> lists = service.getUserLists(jwt.getSubject());
        service.applyTotalPrice(lists);
        // getUserLists include anche le liste dove si e' solo collaboratori: forOwner
        // esporrebbe il collabToken del proprietario anche a loro, va dispacciata per ruolo.
        return ResponseEntity.ok(lists.stream().map(l -> FavoriteListResponseDTO.forRequester(l, jwt.getSubject())).toList());
    }

    /** "Salvati": liste proprie + condivise + itinerari altrui a cui si è messo like. */
    @GetMapping("/saved")
    public ResponseEntity<List<FavoriteListResponseDTO>> getSavedLists(@AuthenticationPrincipal Jwt jwt) {
        List<FavoriteList> lists = service.getSavedLists(jwt.getSubject());
        service.applyTotalPrice(lists);
        return ResponseEntity.ok(lists.stream().map(l -> FavoriteListResponseDTO.forRequester(l, jwt.getSubject())).toList());
    }

    @PostMapping("/catalog-likes/{catalogItemId}")
    public ResponseEntity<Map<String, Boolean>> toggleCatalogItemLike(@PathVariable Long catalogItemId,
                                                                        @AuthenticationPrincipal Jwt jwt) {
        boolean liked = service.toggleCatalogItemLike(catalogItemId, jwt.getSubject());
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @GetMapping("/catalog-likes/mine")
    public ResponseEntity<List<Long>> getMyLikedCatalogItems(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.getLikedCatalogItemIds(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FavoriteListResponseDTO> getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        FavoriteList list = service.getAccessibleById(id, jwt.getSubject());
        service.applyLikedByMe(list, jwt.getSubject());
        service.applyTotalPrice(list);
        return ResponseEntity.ok(FavoriteListResponseDTO.forRequester(list, jwt.getSubject()));
    }

    /** Esporta la lista come file .ics per l'app Calendario del telefono. */
    @GetMapping("/{id}/calendar.ics")
    public ResponseEntity<String> exportCalendar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        ItineraryService.IcsExport export = service.exportToIcs(id, jwt.getSubject());
        String safeName = export.listName().replaceAll("[^a-zA-Z0-9 -]", "").trim();
        String filename = (safeName.isBlank() ? "itinerario" : safeName) + ".ics";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(export.content());
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<FavoriteListResponseDTO> updateVisibility(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateVisibilityRequestDTO request,
                                                           @AuthenticationPrincipal Jwt jwt) {
        FavoriteList updated = service.setVisibility(id, request.visibility(), request.city(), jwt.getSubject());
        return ResponseEntity.ok(FavoriteListResponseDTO.forOwner(updated));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        boolean liked = service.toggleLike(id, jwt.getSubject());
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @PostMapping("/{id}/booked")
    public ResponseEntity<Void> registerBookingAttempt(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.registerBookingAttempt(id, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    /**
     * "Prenota tutto": aggiunge ogni componente della lista al carrello reale su
     * booking-service, propagando il JWT dell'utente corrente (booking-service
     * ricava l'identità solo dal token, non da un header).
     */
    @PostMapping("/{id}/book-all")
    public ResponseEntity<BookAllResultDTO> bookAll(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        BookAllResultDTO result = service.bookAllItems(id, jwt.getSubject(), jwt.getTokenValue());
        return ResponseEntity.ok(result);
    }

    // --- Feed pubblico e link con capabilities: nessuna autenticazione richiesta ---

    /**
     * Il feed non calcola totalPrice: farlo per ogni lista significherebbe una chiamata
     * HTTP a catalog-service per ogni componente di ogni lista, su un endpoint anonimo
     * (facile amplificazione DoS). Il prezzo reale resta disponibile nel dettaglio
     * (getById/getByPublicToken), dove riguarda una sola lista. Le card del feed non lo
     * mostrano comunque (vedi frontend ItineraryListScreen).
     */
    @GetMapping("/public")
    public ResponseEntity<List<FavoriteListResponseDTO>> getPublicFeed(@RequestParam(required = false) String city,
                                                              @RequestParam(required = false) String sort,
                                                              @AuthenticationPrincipal Jwt jwt) {
        List<FavoriteList> feed = service.getPublicFeed(city, sort);
        service.applyLikedByMe(feed, jwt != null ? jwt.getSubject() : null);
        List<FavoriteListResponseDTO> response = feed.stream()
                .map(list -> toPublicOrRequesterView(list, jwt))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/{publicToken}")
    public ResponseEntity<FavoriteListResponseDTO> getByPublicToken(@PathVariable String publicToken,
                                                          @AuthenticationPrincipal Jwt jwt) {
        FavoriteList list = service.getByPublicToken(publicToken);
        service.applyLikedByMe(list, jwt != null ? jwt.getSubject() : null);
        service.applyTotalPrice(list);
        return ResponseEntity.ok(toPublicOrRequesterView(list, jwt));
    }

    /** Anonimo -> vista pubblica (niente collabToken né sharedUserIds); autenticato -> vista in base a chi è. */
    private FavoriteListResponseDTO toPublicOrRequesterView(FavoriteList list, Jwt jwt) {
        return jwt != null
                ? FavoriteListResponseDTO.forRequester(list, jwt.getSubject())
                : FavoriteListResponseDTO.forPublic(list);
    }
}
