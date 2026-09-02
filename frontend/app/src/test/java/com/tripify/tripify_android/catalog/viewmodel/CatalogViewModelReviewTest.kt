package com.tripify.tripify_android.catalog.viewmodel

import com.tripify.tripify_android.data.BookingApi
import com.tripify.tripify_android.data.CatalogApi
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.ReviewApi
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.model.ReviewDto
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class CatalogViewModelReviewTest {

    private val mainDispatcher = StandardTestDispatcher()
    private lateinit var reviewApi: ReviewApi
    private lateinit var bookingApi: BookingApi

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        reviewApi = mockk()
        bookingApi = mockk()
        coEvery { reviewApi.getReviewsForItem(any()) } returns Response.success(emptyList())
        coEvery { bookingApi.hasUserBookedItem(any()) } returns Response.success(false)
        mockkObject(RetrofitClient)
        every { RetrofitClient.createReviewApi(any()) } returns reviewApi
        every { RetrofitClient.createBookingApi(any()) } returns bookingApi
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun viewModel(): CatalogViewModel =
        CatalogViewModel(mockk(relaxed = true), mockk<TokenManager>(relaxed = true))

    private fun errorBody(message: String) =
        message.toResponseBody("text/plain".toMediaTypeOrNull())

    private fun review() = ReviewDto(
        id = 1L, rating = 5, comment = "tutto perfetto", travelerId = "sub-1", catalogItemId = 42L
    )

    @Test
    fun submitReviewInvokesOnSuccessWhenTheServerAccepts() = runTest(mainDispatcher) {
        coEvery { reviewApi.addReview(any()) } returns Response.success(review())
        val vm = viewModel()
        advanceUntilIdle()

        var succeeded = false
        var error: String? = null
        vm.submitReview(42L, 5, "tutto perfetto", onSuccess = { succeeded = true }, onError = { error = it })
        advanceUntilIdle()

        assertTrue(succeeded)
        assertNull(error)
        coVerify { reviewApi.addReview(any()) }
    }

    @Test
    fun submitReviewSurfacesTheServerMessageOnFailure() = runTest(mainDispatcher) {
        coEvery { reviewApi.addReview(any()) } returns Response.error(400, errorBody("Devi aver prenotato"))
        val vm = viewModel()
        advanceUntilIdle()

        var error: String? = null
        vm.submitReview(42L, 5, "x", onSuccess = { }, onError = { error = it })
        advanceUntilIdle()

        assertEquals("Devi aver prenotato", error)
    }

    @Test
    fun submitReviewUnwrapsJsonErrorBody() = runTest(mainDispatcher) {
        coEvery { reviewApi.addReview(any()) } returns
            Response.error(409, errorBody("{\"error\":\"Hai gia recensito questa esperienza\"}"))
        val vm = viewModel()
        advanceUntilIdle()

        var error: String? = null
        vm.submitReview(42L, 5, "x", onSuccess = { }, onError = { error = it })
        advanceUntilIdle()

        assertEquals("Hai gia recensito questa esperienza", error)
    }

    @Test
    fun submitReviewForwardsTheShowNameChoice() = runTest(mainDispatcher) {
        coEvery { reviewApi.addReview(any()) } returns Response.success(review())
        val vm = viewModel()
        advanceUntilIdle()

        vm.submitReview(42L, 5, "tutto perfetto", showName = true, onSuccess = { }, onError = { })
        advanceUntilIdle()

        coVerify { reviewApi.addReview(match { it.showName && it.catalogItemId == 42L }) }
    }

    @Test
    fun submitReviewDefaultsShowNameToFalse() = runTest(mainDispatcher) {
        coEvery { reviewApi.addReview(any()) } returns Response.success(review())
        val vm = viewModel()
        advanceUntilIdle()

        vm.submitReview(42L, 5, "tutto perfetto", onSuccess = { }, onError = { })
        advanceUntilIdle()

        coVerify { reviewApi.addReview(match { !it.showName }) }
    }

    @Test
    fun updateReviewForwardsTheShowNameChoice() = runTest(mainDispatcher) {
        coEvery { reviewApi.updateReview(any(), any()) } returns Response.success(review())
        val vm = viewModel()
        advanceUntilIdle()

        vm.updateReview(1L, 42L, 4, "ok", showName = true, onSuccess = { }, onError = { })
        advanceUntilIdle()

        coVerify { reviewApi.updateReview(1L, match { it.showName }) }
    }

    @Test
    fun replyToReviewInvokesOnSuccessWhenTheServerAccepts() = runTest(mainDispatcher) {
        coEvery { reviewApi.replyToReview(any(), any()) } returns Response.success(review())
        val vm = viewModel()
        advanceUntilIdle()

        var succeeded = false
        vm.replyToReview(1L, 42L, "grazie del feedback", onSuccess = { succeeded = true }, onError = { })
        advanceUntilIdle()

        assertTrue(succeeded)
        coVerify { reviewApi.replyToReview(1L, any()) }
    }

    @Test
    fun replyToReviewSurfacesTheServerMessageOnFailure() = runTest(mainDispatcher) {
        coEvery { reviewApi.replyToReview(any(), any()) } returns Response.error(403, errorBody("Non sei l'organizzatore"))
        val vm = viewModel()
        advanceUntilIdle()

        var error: String? = null
        vm.replyToReview(1L, 42L, "grazie", onSuccess = { }, onError = { error = it })
        advanceUntilIdle()

        assertEquals("Non sei l'organizzatore", error)
    }

    @Test
    fun updateReviewFallsBackToADefaultMessageWhenTheServerBodyIsEmpty() = runTest(mainDispatcher) {
        coEvery { reviewApi.updateReview(any(), any()) } returns Response.error(500, errorBody(""))
        val vm = viewModel()
        advanceUntilIdle()

        var error: String? = null
        vm.updateReview(1L, 42L, 4, "meno bene", onSuccess = { }, onError = { error = it })
        advanceUntilIdle()

        assertEquals("Impossibile modificare la recensione.", error)
    }

    @Test
    fun deleteReviewInvokesOnSuccessWhenTheServerAccepts() = runTest(mainDispatcher) {
        coEvery { reviewApi.deleteReview(any()) } returns Response.success(Unit)
        val vm = viewModel()
        advanceUntilIdle()

        var succeeded = false
        vm.deleteReview(1L, 42L, onSuccess = { succeeded = true }, onError = { })
        advanceUntilIdle()

        assertTrue(succeeded)
        coVerify { reviewApi.deleteReview(1L) }
    }

    @Test
    fun toggleReviewHelpfulPatchesOnlyTheHelpfulFieldsOfTheTargetReview() = runTest(mainDispatcher) {
        val a = ReviewDto(id = 1L, rating = 5, comment = "a", travelerId = null, catalogItemId = 42L, helpfulCount = 0, helpfulByMe = false)
        val b = ReviewDto(id = 2L, rating = 4, comment = "b", travelerId = null, catalogItemId = 42L, helpfulCount = 7, helpfulByMe = false)
        coEvery { reviewApi.getReviewsForItem(42L) } returns Response.success(listOf(a, b))
        coEvery { reviewApi.toggleHelpful(1L) } returns Response.success(
            a.copy(travelerId = "leak-should-be-ignored", helpfulCount = 1, helpfulByMe = true)
        )
        val vm = viewModel()
        vm.loadReviewsAndBookingStatus(42L)
        advanceUntilIdle()

        vm.toggleReviewHelpful(1L, onError = { })
        advanceUntilIdle()

        val updated = vm.itemReviews.value
        assertEquals(1, updated.first { it.id == 1L }.helpfulCount)
        assertTrue(updated.first { it.id == 1L }.helpfulByMe)
        assertNull(updated.first { it.id == 1L }.travelerId)
        assertEquals(7, updated.first { it.id == 2L }.helpfulCount)
    }

    @Test
    fun toggleReviewHelpfulSurfacesTheServerMessageOnFailure() = runTest(mainDispatcher) {
        coEvery { reviewApi.toggleHelpful(any()) } returns Response.error(409, errorBody("{\"error\":\"Non puoi votare la tua recensione\"}"))
        val vm = viewModel()
        advanceUntilIdle()

        var error: String? = null
        vm.toggleReviewHelpful(9L, onError = { error = it })
        advanceUntilIdle()

        assertEquals("Non puoi votare la tua recensione", error)
    }
}
