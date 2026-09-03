package com.tripify.tripify_android.itinerary.viewmodel

import android.content.Context
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.itinerary.data.BookAllResultDto
import com.tripify.tripify_android.itinerary.data.CreateListRequest
import com.tripify.tripify_android.itinerary.data.FavoriteListDto
import com.tripify.tripify_android.itinerary.data.FavoriteListItemDto
import com.tripify.tripify_android.itinerary.data.GenerateItineraryRequest
import com.tripify.tripify_android.itinerary.data.ItineraryApi
import com.tripify.tripify_android.itinerary.data.ItineraryRetrofit
import com.tripify.tripify_android.itinerary.data.LikeResponse
import com.tripify.tripify_android.itinerary.data.RemoveItemResultDto
import com.tripify.tripify_android.itinerary.data.UpdateVisibilityRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ItineraryViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()
    private lateinit var api: ItineraryApi

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        api = mockk()
        mockkObject(ItineraryRetrofit)
        every { ItineraryRetrofit.create(any()) } returns api
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun viewModel(): ItineraryViewModel = ItineraryViewModel(mockk<TokenManager>(relaxed = true))

    private fun errorBody(message: String) = message.toResponseBody("text/plain".toMediaTypeOrNull())

    /** Corpo d'errore realistico: il backend risponde sempre con un ApiError JSON (vedi GlobalExceptionHandler), mai testo semplice. */
    private fun jsonErrorBody(message: String) =
        """{"status":400,"error":"Bad Request","message":"$message"}""".toResponseBody("application/json".toMediaTypeOrNull())

    private fun list(id: Long = 1L, name: String = "Viaggio", ownerId: String = "owner-1") = FavoriteListDto(
        id = id, name = name, ownerId = ownerId, visibility = "PRIVATE", publicToken = null, collabToken = null, city = null
    )

    @Test
    fun loadDetailPopulatesSuccessStateWhenTheServerRespondsWithTheList() = runTest(mainDispatcher) {
        coEvery { api.getById(1L) } returns Response.success(list())
        val vm = viewModel()

        vm.loadDetail(1L)
        advanceUntilIdle()

        val state = vm.detailState.value
        assertTrue(state is ItineraryDetailState.Success)
        assertEquals(1L, (state as ItineraryDetailState.Success).list.id)
    }

    @Test
    fun loadDetailPopulatesErrorStateWhenTheListIsNotFound() = runTest(mainDispatcher) {
        coEvery { api.getById(99L) } returns Response.error(404, errorBody("non trovato"))
        val vm = viewModel()

        vm.loadDetail(99L)
        advanceUntilIdle()

        assertTrue(vm.detailState.value is ItineraryDetailState.Error)
    }

    @Test
    fun loadDetailPopulatesErrorStateWhenTheCallThrows() = runTest(mainDispatcher) {
        coEvery { api.getById(any()) } throws IOException()
        val vm = viewModel()

        vm.loadDetail(1L)
        advanceUntilIdle()

        assertEquals("Nessuna connessione al server", (vm.detailState.value as ItineraryDetailState.Error).message)
    }

    @Test
    fun loadFeedPopulatesSuccessStateWithThePublicLists() = runTest(mainDispatcher) {
        coEvery { api.getPublicFeed(any(), any()) } returns Response.success(listOf(list()))
        val vm = viewModel()

        vm.loadFeed()
        advanceUntilIdle()

        val state = vm.feedState.value
        assertTrue(state is ItineraryFeedState.Success)
        assertEquals(1, (state as ItineraryFeedState.Success).lists.size)
    }

    @Test
    fun loadFeedPopulatesErrorStateWhenTheServerFails() = runTest(mainDispatcher) {
        coEvery { api.getPublicFeed(any(), any()) } returns Response.error(500, errorBody("errore"))
        val vm = viewModel()

        vm.loadFeed()
        advanceUntilIdle()

        assertTrue(vm.feedState.value is ItineraryFeedState.Error)
    }

    @Test
    fun loadMinePopulatesSuccessStateWithOwnedAndSharedLists() = runTest(mainDispatcher) {
        coEvery { api.getMyLists() } returns Response.success(listOf(list(), list(id = 2L)))
        val vm = viewModel()

        vm.loadMine()
        advanceUntilIdle()

        assertEquals(2, (vm.feedState.value as ItineraryFeedState.Success).lists.size)
    }

    @Test
    fun loadDetailByPublicTokenPopulatesSuccessState() = runTest(mainDispatcher) {
        coEvery { api.getByPublicToken("tok-1") } returns Response.success(list())
        val vm = viewModel()

        vm.loadDetailByPublicToken("tok-1")
        advanceUntilIdle()

        assertTrue(vm.detailState.value is ItineraryDetailState.Success)
    }

    @Test
    fun loadDetailByPublicTokenPopulatesErrorStateForAnInvalidToken() = runTest(mainDispatcher) {
        coEvery { api.getByPublicToken(any()) } returns Response.error(404, errorBody("non trovato"))
        val vm = viewModel()

        vm.loadDetailByPublicToken("token-inesistente")
        advanceUntilIdle()

        assertEquals(
            "Link non valido o itinerario non più pubblico",
            (vm.detailState.value as ItineraryDetailState.Error).message
        )
    }

    @Test
    fun createListReturnsTheCreatedListOnSuccess() = runTest(mainDispatcher) {
        coEvery { api.createList(CreateListRequest("Nuovo viaggio")) } returns Response.success(list(name = "Nuovo viaggio"))
        val vm = viewModel()

        var result: FavoriteListDto? = null
        vm.createList("Nuovo viaggio") { result = it }
        advanceUntilIdle()

        assertEquals("Nuovo viaggio", result?.name)
    }

    @Test
    fun createListReturnsNullWhenTheServerRejectsTheRequest() = runTest(mainDispatcher) {
        coEvery { api.createList(any()) } returns Response.error(400, errorBody("nome non valido"))
        val vm = viewModel()

        var result: FavoriteListDto? = list()
        vm.createList("") { result = it }
        advanceUntilIdle()

        assertNull(result)
    }

    @Test
    fun deleteListReportsSuccessWhenTheServerAccepts() = runTest(mainDispatcher) {
        coEvery { api.deleteList(1L) } returns Response.success(Unit)
        val vm = viewModel()

        var success = false
        vm.deleteList(1L) { success = it }
        advanceUntilIdle()

        assertTrue(success)
    }

    @Test
    fun deleteListReportsFailureWhenTheCallThrows() = runTest(mainDispatcher) {
        coEvery { api.deleteList(any()) } throws IOException()
        val vm = viewModel()

        var success = true
        vm.deleteList(1L) { success = it }
        advanceUntilIdle()

        assertFalse(success)
    }

    @Test
    fun removeItemReloadsTheDetailAndReportsAlsoRemovedItemsOnSuccess() = runTest(mainDispatcher) {
        coEvery { api.removeItem(1L, 0) } returns Response.success(RemoveItemResultDto(listOf("Hotel a Roma")))
        coEvery { api.getById(1L) } returns Response.success(list())
        val vm = viewModel()

        var success = false
        var alsoRemoved: List<String> = emptyList()
        vm.removeItem(1L, 0) { s, removed -> success = s; alsoRemoved = removed }
        advanceUntilIdle()

        assertTrue(success)
        assertEquals(listOf("Hotel a Roma"), alsoRemoved)
        coVerify { api.getById(1L) }
    }

    @Test
    fun removeItemDoesNotReloadTheDetailWhenTheServerRejects() = runTest(mainDispatcher) {
        coEvery { api.removeItem(any(), any()) } returns Response.error(409, errorBody("conflitto"))
        val vm = viewModel()

        var success = true
        vm.removeItem(1L, 0) { s, _ -> success = s }
        advanceUntilIdle()

        assertFalse(success)
        coVerify(exactly = 0) { api.getById(any()) }
    }

    @Test
    fun toggleLikeReloadsTheDetailWhenTheServerAccepts() = runTest(mainDispatcher) {
        coEvery { api.toggleLike(1L) } returns Response.success(LikeResponse(true))
        coEvery { api.getById(1L) } returns Response.success(list())
        val vm = viewModel()

        vm.toggleLike(1L)
        advanceUntilIdle()

        coVerify { api.getById(1L) }
    }

    @Test
    fun updateVisibilityReloadsTheDetailAndReportsSuccess() = runTest(mainDispatcher) {
        coEvery { api.updateVisibility(1L, UpdateVisibilityRequest("PUBLIC", "Roma")) } returns Response.success(list())
        coEvery { api.getById(1L) } returns Response.success(list())
        val vm = viewModel()

        var success = false
        var error: String? = "x"
        vm.updateVisibility(1L, "PUBLIC", "Roma") { s, e -> success = s; error = e }
        advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
    }

    @Test
    fun updateVisibilitySurfacesTheServerMessageWhenRequirementsAreNotMet() = runTest(mainDispatcher) {
        coEvery { api.updateVisibility(any(), any()) } returns Response.error(400, jsonErrorBody("Servono almeno 2 componenti"))
        val vm = viewModel()

        var error: String? = null
        vm.updateVisibility(1L, "PUBLIC", null) { _, e -> error = e }
        advanceUntilIdle()

        assertEquals("Servono almeno 2 componenti", error)
    }

    @Test
    fun enableLinkSharingReloadsTheDetailOnSuccess() = runTest(mainDispatcher) {
        coEvery { api.enableLinkSharing(1L) } returns Response.success(list())
        coEvery { api.getById(1L) } returns Response.success(list())
        val vm = viewModel()

        var success = false
        vm.enableLinkSharing(1L) { success = it }
        advanceUntilIdle()

        assertTrue(success)
        coVerify { api.getById(1L) }
    }

    @Test
    fun disableLinkSharingReportsFailureWhenTheCallThrows() = runTest(mainDispatcher) {
        coEvery { api.disableLinkSharing(any()) } throws IOException()
        val vm = viewModel()

        var success = true
        vm.disableLinkSharing(1L) { success = it }
        advanceUntilIdle()

        assertFalse(success)
    }

    @Test
    fun enableCollabInviteReloadsTheDetailOnSuccess() = runTest(mainDispatcher) {
        coEvery { api.enableCollabInvite(1L) } returns Response.success(list())
        coEvery { api.getById(1L) } returns Response.success(list())
        val vm = viewModel()

        var success = false
        vm.enableCollabInvite(1L) { success = it }
        advanceUntilIdle()

        assertTrue(success)
        coVerify { api.getById(1L) }
    }

    @Test
    fun disableCollabInviteReportsFailureWhenTheServerRejects() = runTest(mainDispatcher) {
        coEvery { api.disableCollabInvite(any()) } returns Response.error(403, errorBody("vietato"))
        val vm = viewModel()

        var success = true
        vm.disableCollabInvite(1L) { success = it }
        advanceUntilIdle()

        assertFalse(success)
    }

    @Test
    fun joinAsCollaboratorReturnsTheListIdOnSuccess() = runTest(mainDispatcher) {
        coEvery { api.joinAsCollaborator("tok-123") } returns Response.success(list(id = 7L))
        val vm = viewModel()

        var listId: Long? = null
        var error: String? = "x"
        vm.joinAsCollaborator("tok-123") { id, e -> listId = id; error = e }
        advanceUntilIdle()

        assertEquals(7L, listId)
        assertNull(error)
    }

    @Test
    fun joinAsCollaboratorReportsAnErrorForAnInvalidToken() = runTest(mainDispatcher) {
        coEvery { api.joinAsCollaborator(any()) } returns Response.error(404, errorBody("non valido"))
        val vm = viewModel()

        var error: String? = null
        vm.joinAsCollaborator("token-inesistente") { _, e -> error = e }
        advanceUntilIdle()

        assertEquals("Link di invito non valido o non più attivo", error)
    }

    @Test
    fun renameListReloadsTheDetailOnSuccess() = runTest(mainDispatcher) {
        coEvery { api.renameList(1L, CreateListRequest("Nuovo nome")) } returns Response.success(list(name = "Nuovo nome"))
        coEvery { api.getById(1L) } returns Response.success(list(name = "Nuovo nome"))
        val vm = viewModel()

        var success = false
        vm.renameList(1L, "Nuovo nome") { success = it }
        advanceUntilIdle()

        assertTrue(success)
        coVerify { api.getById(1L) }
    }

    @Test
    fun bookAllReturnsTheServerCountsOnSuccess() = runTest(mainDispatcher) {
        val toBook = list(id = 3L).copy(items = listOf(FavoriteListItemDto(catalogItemId = 1L)))
        coEvery { api.bookAll(3L) } returns Response.success(BookAllResultDto(successCount = 1, total = 1, errors = emptyList()))
        val vm = viewModel()

        var successCount = -1
        var total = -1
        vm.bookAll(toBook) { s, t, _ -> successCount = s; total = t }
        advanceUntilIdle()

        assertEquals(1, successCount)
        assertEquals(1, total)
    }

    @Test
    fun bookAllFallsBackToZeroSuccessesWhenTheCallThrows() = runTest(mainDispatcher) {
        val toBook = list(id = 3L).copy(items = listOf(FavoriteListItemDto(catalogItemId = 1L)))
        coEvery { api.bookAll(any()) } throws IOException()
        val vm = viewModel()

        var successCount = -1
        var total = -1
        vm.bookAll(toBook) { s, t, _ -> successCount = s; total = t }
        advanceUntilIdle()

        assertEquals(0, successCount)
        assertEquals(1, total)
    }

    @Test
    fun exportCalendarReportsAnErrorWhenTheServerRespondsWithFailure() = runTest(mainDispatcher) {
        coEvery { api.exportCalendar(1L) } returns Response.error(404, errorBody("non trovato"))
        val vm = viewModel()

        var error: String? = null
        vm.exportCalendar(mockk(relaxed = true), 1L, "Viaggio") { error = it }
        advanceUntilIdle()

        assertEquals("Impossibile scaricare il calendario", error)
    }

    @Test
    fun exportCalendarReportsAConnectionErrorWhenTheCallThrows() = runTest(mainDispatcher) {
        coEvery { api.exportCalendar(any()) } throws IOException()
        val vm = viewModel()

        var error: String? = null
        vm.exportCalendar(mockk(relaxed = true), 1L, "Viaggio") { error = it }
        advanceUntilIdle()

        assertEquals("Nessuna connessione al server", error)
    }

    @Test
    fun cloneListReturnsTheNewListIdOnSuccess() = runTest(mainDispatcher) {
        coEvery { api.cloneList(1L) } returns Response.success(list(id = 2L, name = "Copia di Viaggio"))
        val vm = viewModel()

        var newId: Long? = null
        var error: String? = "non toccato"
        vm.cloneList(1L) { id, e -> newId = id; error = e }
        advanceUntilIdle()

        assertEquals(2L, newId)
        assertNull(error)
    }

    @Test
    fun cloneListSurfacesTheServerMessageOnFailure() = runTest(mainDispatcher) {
        coEvery { api.cloneList(any()) } returns Response.error(403, jsonErrorBody("Non hai i permessi per modificare questa lista"))
        val vm = viewModel()

        var newId: Long? = 1L
        var error: String? = null
        vm.cloneList(1L) { id, e -> newId = id; error = e }
        advanceUntilIdle()

        assertNull(newId)
        assertEquals("Non hai i permessi per modificare questa lista", error)
    }

    @Test
    fun cloneListReportsConnectionErrorOnException() = runTest(mainDispatcher) {
        coEvery { api.cloneList(any()) } throws IOException()
        val vm = viewModel()

        var error: String? = null
        vm.cloneList(1L) { _, e -> error = e }
        advanceUntilIdle()

        assertEquals("Nessuna connessione al server", error)
    }

    @Test
    fun generateItineraryReturnsTheNewListIdOnSuccess() = runTest(mainDispatcher) {
        coEvery { api.generateItinerary(GenerateItineraryRequest("Milano", "Roma", 3, 2, true, null)) } returns
            Response.success(list(id = 5L, name = "Viaggio a Roma"))
        val vm = viewModel()

        var newId: Long? = null
        vm.generateItinerary("Milano", "Roma", 3, 2, true, null) { id, _ -> newId = id }
        advanceUntilIdle()

        assertEquals(5L, newId)
    }

    @Test
    fun generateItinerarySurfacesTheServerMessageOnFailure() = runTest(mainDispatcher) {
        coEvery { api.generateItinerary(any()) } returns
            Response.error(400, jsonErrorBody("Nessun volo con hotel disponibile per Atlantide: prova un'altra destinazione"))
        val vm = viewModel()

        var newId: Long? = 1L
        var error: String? = null
        vm.generateItinerary("Milano", "Atlantide", 3, 1, false, null) { id, e -> newId = id; error = e }
        advanceUntilIdle()

        assertNull(newId)
        assertEquals("Nessun volo con hotel disponibile per Atlantide: prova un'altra destinazione", error)
    }

    @Test
    fun generateItineraryReportsConnectionErrorOnException() = runTest(mainDispatcher) {
        coEvery { api.generateItinerary(any()) } throws IOException()
        val vm = viewModel()

        var error: String? = null
        vm.generateItinerary("Milano", "Roma", 3, 1, false, null) { _, e -> error = e }
        advanceUntilIdle()

        assertEquals("Nessuna connessione al server", error)
    }
}
