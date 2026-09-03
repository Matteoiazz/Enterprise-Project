package com.tripify.itinerary_service.dto;

import com.tripify.itinerary_service.entity.FavoriteList;
import com.tripify.itinerary_service.entity.FavoriteListItem;
import com.tripify.itinerary_service.entity.Visibility;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista esterna di una FavoriteList. collabToken (accesso in modifica) viene incluso
 * solo per il proprietario: chi lo riceve può auto-aggiungersi come collaboratore
 * chiamando /collab-link/{token}/join, quindi non deve mai comparire nel feed
 * pubblico, nel link di sola visualizzazione, o nella vista di un altro utente.
 */
public record FavoriteListResponseDTO(
        Long id,
        String name,
        String ownerId,
        List<String> sharedUserIds,
        List<FavoriteListItem> items,
        Visibility visibility,
        String publicToken,
        String collabToken,
        String city,
        int likesCount,
        int bookingsCount,
        LocalDateTime createdAt,
        boolean likedByMe,
        BigDecimal totalPrice
) {

    /** Per il proprietario: vede tutto, compreso il token per gestire gli inviti a collaborare. */
    public static FavoriteListResponseDTO forOwner(FavoriteList list) {
        return new FavoriteListResponseDTO(
                list.getId(), list.getName(), list.getOwnerId(), list.getSharedUserIds(), list.getItems(),
                list.getVisibility(), list.getPublicToken(), list.getCollabToken(), list.getCity(),
                list.getLikesCount(), list.getBookingsCount(), list.getCreatedAt(), list.isLikedByMe(), list.getTotalPrice()
        );
    }

    /** Per un collaboratore (in sharedUserIds, non proprietario): gli servono gli altri
     * sharedUserIds per il proprio controllo di canEdit lato app, ma niente collabToken
     * (non è lui ad aver creato l'invito). */
    public static FavoriteListResponseDTO forCollaborator(FavoriteList list) {
        return new FavoriteListResponseDTO(
                list.getId(), list.getName(), list.getOwnerId(), list.getSharedUserIds(), list.getItems(),
                list.getVisibility(), list.getPublicToken(), null, list.getCity(),
                list.getLikesCount(), list.getBookingsCount(), list.getCreatedAt(), list.isLikedByMe(), list.getTotalPrice()
        );
    }

    /** Per chi vede la lista senza esserne proprietario né collaboratore (es. autenticato
     * che sfoglia il feed pubblico): niente collabToken né sharedUserIds, non gli servono
     * e rivelerebbero gli id degli altri collaboratori a chi non c'entra nulla. */
    public static FavoriteListResponseDTO forViewer(FavoriteList list) {
        return new FavoriteListResponseDTO(
                list.getId(), list.getName(), list.getOwnerId(), List.of(), list.getItems(),
                list.getVisibility(), list.getPublicToken(), null, list.getCity(),
                list.getLikesCount(), list.getBookingsCount(), list.getCreatedAt(), list.isLikedByMe(), list.getTotalPrice()
        );
    }

    /** Per il feed pubblico e il link di sola visualizzazione (nessuna autenticazione): niente collabToken né sharedUserIds. */
    public static FavoriteListResponseDTO forPublic(FavoriteList list) {
        return forViewer(list);
    }

    /** Sceglie owner/collaboratore/viewer in base a chi sta chiedendo (null = non autenticato). */
    public static FavoriteListResponseDTO forRequester(FavoriteList list, String requesterId) {
        if (requesterId != null && requesterId.equals(list.getOwnerId())) {
            return forOwner(list);
        }
        if (requesterId != null && list.getSharedUserIds().contains(requesterId)) {
            return forCollaborator(list);
        }
        return forViewer(list);
    }
}
