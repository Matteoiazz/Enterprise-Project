package com.tripify.tripify_android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenManagerTest {

    private lateinit var tokenManager: TokenManager

    private class FakeDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> get() = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    @Before
    fun setUp() {
        val context = mockk<Context>()
        mockkStatic("com.tripify.tripify_android.data.TokenManagerKt")
        every { context.dataStore } returns FakeDataStore()
        tokenManager = TokenManager(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun savesAndReadsBackTheAccessToken() = runTest {
        tokenManager.saveToken("access-1")

        assertEquals("access-1", tokenManager.tokenFlow.first())
    }

    @Test
    fun savesAndReadsBackIdAndRefreshTokens() = runTest {
        tokenManager.saveIdToken("id-1")
        tokenManager.saveRefreshToken("refresh-1")

        assertEquals("id-1", tokenManager.getIdToken())
        assertEquals("refresh-1", tokenManager.getRefreshToken())
    }

    @Test
    fun clearTokensRemovesEveryStoredToken() = runTest {
        tokenManager.saveToken("access-1")
        tokenManager.saveIdToken("id-1")
        tokenManager.saveRefreshToken("refresh-1")

        tokenManager.clearTokens()

        assertNull(tokenManager.tokenFlow.first())
        assertNull(tokenManager.getIdToken())
        assertNull(tokenManager.getRefreshToken())
    }

    @Test
    fun refreshWithoutAStoredRefreshTokenReportsInvalidGrant() = runTest {
        val result = tokenManager.refreshAccessToken("previous-access")

        assertTrue(result is TokenManager.RefreshResult.InvalidGrant)
    }

    @Test
    fun refreshShortCircuitsWhenAnotherCallAlreadyRotatedTheToken() = runTest {
        tokenManager.saveToken("rotated-by-another-call")

        val result = tokenManager.refreshAccessToken("previous-access")

        assertTrue(result is TokenManager.RefreshResult.Success)
        assertEquals("rotated-by-another-call", (result as TokenManager.RefreshResult.Success).accessToken)
    }
}
