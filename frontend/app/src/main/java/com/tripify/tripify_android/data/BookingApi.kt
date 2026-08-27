package com.tripify.tripify_android.data

import com.tripify.tripify_android.data.model.AddToCartRequestDTO
import com.tripify.tripify_android.data.model.BookingResponseDTO
import com.tripify.tripify_android.data.model.CartDTO
import com.tripify.tripify_android.data.model.CheckoutRequestDTO
import com.tripify.tripify_android.data.model.PagedResponse
import com.tripify.tripify_android.data.model.PassengerRequestDTO
import com.tripify.tripify_android.data.model.PaymentMethodDto
import com.tripify.tripify_android.data.model.PaymentRequestDTO
import com.tripify.tripify_android.data.model.PaymentResultDTO
import com.tripify.tripify_android.data.model.ReceivedBookingLineDto
import com.tripify.tripify_android.data.model.TravelDocumentDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookingApi {

    // L'utente non si passa più esplicitamente (né come path, né come header
    // X-User-Id): il backend lo ricava dal JWT che AuthInterceptor allega già
    // in automatico a ogni richiesta (header Authorization).
    @GET("api/v1/cart")
    suspend fun getCart(): Response<CartDTO>

    // catalogItemId/quantity (e opzionalmente roomTypeId/fareClassId/checkIn/
    // checkOut per camere e voli) vanno ora nel body, non più come query param.
    @POST("api/v1/cart/add")
    suspend fun addToCart(
        @Body request: AddToCartRequestDTO
    ): Response<Unit>

    @DELETE("api/v1/cart/clear")
    suspend fun clearCart(): Response<Unit>

    // Rimuove un singolo articolo dal carrello (rilascia il suo eventuale hold).
    @DELETE("api/v1/cart/items/{itemId}")
    suspend fun removeCartItem(@Path("itemId") itemId: Long): Response<Unit>

    // Trasforma il carrello (o solo gli articoli selezionati) dell'utente
    // autenticato in una prenotazione PENDING.
    @POST("api/v1/bookings/checkout")
    suspend fun checkout(@Body request: CheckoutRequestDTO = CheckoutRequestDTO()): Response<BookingResponseDTO>

    // Storico dei viaggi, ora paginato lato server.
    @GET("api/v1/bookings/user")
    suspend fun getUserBookings(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<BookingResponseDTO>>

    // Permette al Leader di invitare un amico a un viaggio
    @POST("api/v1/bookings/{bookingId}/invite")
    suspend fun inviteFriend(
        @Path("bookingId") bookingId: Long,
        @Query("friendId") friendId: String
    ): Response<BookingResponseDTO>

    // Annulla una prenotazione (solo il Leader); se era già pagata avvia anche il rimborso.
    @POST("api/v1/bookings/{bookingId}/cancel")
    suspend fun cancelBooking(
        @Path("bookingId") bookingId: Long
    ): Response<BookingResponseDTO>

    // Simula l'addebito sulla carta e, se approvato, conferma la prenotazione
    // (PENDING -> CONFIRMED) lato booking-service.
    @POST("api/v1/payments/process")
    suspend fun processPayment(
        @Body request: PaymentRequestDTO
    ): Response<PaymentResultDTO>

    // Prenotazioni ricevute sugli annunci di cui il chiamante è organizzatore.
    @GET("api/v1/bookings/received")
    suspend fun getReceivedBookings(): Response<List<ReceivedBookingLineDto>>

    // Proxy verso user-auth-service (già pronto lato booking-service): mostra i
    // metodi/documenti salvati in Impostazioni Profilo senza uscire dal checkout.
    @GET("api/v1/checkout/payment-methods")
    suspend fun getSavedPaymentMethods(): Response<List<PaymentMethodDto>>

    @GET("api/v1/checkout/travel-documents")
    suspend fun getSavedTravelDocuments(): Response<List<TravelDocumentDto>>

    // Associa un passeggero (con documento già scelto/inserito) a una riga di prenotazione.
    @POST("api/v1/bookings/lines/{bookingLineId}/passengers")
    suspend fun addPassenger(
        @Path("bookingLineId") bookingLineId: Long,
        @Body request: PassengerRequestDTO
    ): Response<Unit>

    @GET("api/v1/bookings/catalog/{catalogItemId}/has-booked")
    suspend fun hasUserBookedItem(@Path("catalogItemId") catalogItemId: Long): Response<Boolean>
}
