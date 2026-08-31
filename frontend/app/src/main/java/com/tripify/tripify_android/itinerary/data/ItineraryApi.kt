package com.tripify.tripify_android.itinerary.data

import retrofit2.Response
import retrofit2.http.*

interface ItineraryApi {

    @GET("api/v1/itinerary/public")
    suspend fun getPublicFeed(
        @Query("city") city: String? = null,
        @Query("sort") sort: String? = null
    ): Response<List<FavoriteListDto>>

    @GET("api/v1/itinerary/public/{publicToken}")
    suspend fun getByPublicToken(@Path("publicToken") publicToken: String): Response<FavoriteListDto>

    @GET("api/v1/itinerary/{id}")
    suspend fun getById(@Path("id") id: Long): Response<FavoriteListDto>

    @DELETE("api/v1/itinerary/{id}")
    suspend fun deleteList(@Path("id") id: Long): Response<Unit>

    @GET("api/v1/itinerary/mine")
    suspend fun getMyLists(): Response<List<FavoriteListDto>>

    /** "Salvati": liste proprie + condivise + itinerari altrui a cui si è messo like. */
    @GET("api/v1/itinerary/saved")
    suspend fun getSavedLists(): Response<List<FavoriteListDto>>

    @POST("api/v1/itinerary/catalog-likes/{catalogItemId}")
    suspend fun toggleCatalogItemLike(@Path("catalogItemId") catalogItemId: Long): Response<LikeResponse>

    @GET("api/v1/itinerary/catalog-likes/mine")
    suspend fun getLikedCatalogItemIds(): Response<List<Long>>

    @POST("api/v1/itinerary")
    suspend fun createList(@Body request: CreateListRequest): Response<FavoriteListDto>

    @POST("api/v1/itinerary/{id}/items")
    suspend fun addItem(@Path("id") id: Long, @Body request: AddListItemRequest): Response<Unit>

    /** Rimuove il componente in posizione {index} (0-based, stesso ordine mostrato nel dettaglio). */
    @DELETE("api/v1/itinerary/{id}/items/{index}")
    suspend fun removeItem(@Path("id") id: Long, @Path("index") index: Int): Response<RemoveItemResultDto>

    @PUT("api/v1/itinerary/{id}/share")
    suspend fun share(@Path("id") id: Long, @Query("userId") userId: String): Response<Unit>

    @PATCH("api/v1/itinerary/{id}/visibility")
    suspend fun updateVisibility(@Path("id") id: Long, @Body request: UpdateVisibilityRequest): Response<FavoriteListDto>

    /** Link di condivisione indipendente dalla visibilità: funziona anche su liste private/condivise. */
    @POST("api/v1/itinerary/{id}/link")
    suspend fun enableLinkSharing(@Path("id") id: Long): Response<FavoriteListDto>

    @DELETE("api/v1/itinerary/{id}/link")
    suspend fun disableLinkSharing(@Path("id") id: Long): Response<FavoriteListDto>

    /** Link di invito: chi lo apre da loggato entra come collaboratore (può modificare la lista). */
    @POST("api/v1/itinerary/{id}/collab-link")
    suspend fun enableCollabInvite(@Path("id") id: Long): Response<FavoriteListDto>

    @DELETE("api/v1/itinerary/{id}/collab-link")
    suspend fun disableCollabInvite(@Path("id") id: Long): Response<FavoriteListDto>

    @POST("api/v1/itinerary/collab-link/{token}/join")
    suspend fun joinAsCollaborator(@Path("token") token: String): Response<FavoriteListDto>

    @PATCH("api/v1/itinerary/{id}/name")
    suspend fun renameList(@Path("id") id: Long, @Body request: CreateListRequest): Response<FavoriteListDto>

    @POST("api/v1/itinerary/{id}/like")
    suspend fun toggleLike(@Path("id") id: Long): Response<LikeResponse>

    @POST("api/v1/itinerary/{id}/booked")
    suspend fun registerBookingAttempt(@Path("id") id: Long): Response<Unit>

    /** "Prenota tutto": booking-service riceve ogni componente della lista con hold su room/fare quando presenti. */
    @POST("api/v1/itinerary/{id}/book-all")
    suspend fun bookAll(@Path("id") id: Long): Response<BookAllResultDto>
}
