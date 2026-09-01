package com.tripify.tripify_android.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenManager: TokenManager

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenManager)).build()

    private fun request(): Request =
        Request.Builder().url(server.url("/api/v1/catalog/items")).build()

    @Test
    fun attachesBearerHeaderWhenTokenIsPresent() {
        every { tokenManager.tokenFlow } returns flowOf("access-1")
        server.enqueue(MockResponse().setResponseCode(200))

        client().newCall(request()).execute().close()

        assertEquals("Bearer access-1", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun sendsNoAuthorizationHeaderWhenTokenIsMissing() {
        every { tokenManager.tokenFlow } returns flowOf(null)
        server.enqueue(MockResponse().setResponseCode(200))

        client().newCall(request()).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun onUnauthorizedRefreshesAndRetriesWithTheNewToken() {
        every { tokenManager.tokenFlow } returns flowOf("stale")
        coEvery { tokenManager.refreshAccessToken("stale") } returns TokenManager.RefreshResult.Success("fresh")
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))

        val response = client().newCall(request()).execute()
        response.close()

        assertEquals(200, response.code)
        assertEquals(2, server.requestCount)
        assertEquals("Bearer stale", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer fresh", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun onInvalidGrantWithAnActiveSessionClearsTokensAndRetriesAnonymously() {
        every { tokenManager.tokenFlow } returns flowOf("stale")
        coEvery { tokenManager.refreshAccessToken("stale") } returns TokenManager.RefreshResult.InvalidGrant
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))

        val response = client().newCall(request()).execute()
        response.close()

        assertEquals(200, response.code)
        coVerify(exactly = 1) { tokenManager.clearTokens() }
        server.takeRequest()
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun onInvalidGrantWithoutASessionReturnsTheResponseUntouched() {
        every { tokenManager.tokenFlow } returns flowOf(null)
        coEvery { tokenManager.refreshAccessToken(null) } returns TokenManager.RefreshResult.InvalidGrant
        server.enqueue(MockResponse().setResponseCode(401))

        val response = client().newCall(request()).execute()
        response.close()

        assertEquals(401, response.code)
        assertEquals(1, server.requestCount)
        coVerify(exactly = 0) { tokenManager.clearTokens() }
    }

    @Test
    fun onTransientRefreshErrorKeepsTheSessionAndDoesNotRetry() {
        every { tokenManager.tokenFlow } returns flowOf("stale")
        coEvery { tokenManager.refreshAccessToken("stale") } returns TokenManager.RefreshResult.TransientError
        server.enqueue(MockResponse().setResponseCode(401))

        val response = client().newCall(request()).execute()
        response.close()

        assertEquals(401, response.code)
        assertEquals(1, server.requestCount)
        coVerify(exactly = 0) { tokenManager.clearTokens() }
    }
}
