package com.tripify.tripify_android.profile.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.BuildConfig
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.data.model.UpdatePecRequest
import com.tripify.tripify_android.data.model.UpdateProfileRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel(private val tokenManager: TokenManager) : ViewModel() {

    var name by mutableStateOf("")
    var surname by mutableStateOf("")
    var email by mutableStateOf("")
    var profilePictureUrl by mutableStateOf<String?>(null)

    var phone by mutableStateOf("")
    var address by mutableStateOf("")
    var companyName by mutableStateOf("")
    var pec by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var isUploadingImage by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isLoggedOut by mutableStateOf(false)

    private val api = RetrofitClient.createApi(tokenManager)
    private val profileApi = RetrofitClient.createProfileApi(tokenManager)

    private var authService: AuthorizationService? = null

    var organizersList by mutableStateOf<List<com.tripify.tripify_android.data.UserResponse>>(emptyList())
    var isLoadingOrganizers by mutableStateOf(false)

    fun loadUserProfile() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.getCurrentUser()

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    name = user.name ?: ""
                    surname = user.surname ?: ""
                    email = user.email ?: ""
                    profilePictureUrl = user.profilePictureUrl
                    phone = user.phone ?: ""
                    address = user.address ?: ""
                    companyName = user.companyName ?: ""
                    pec = user.pec ?: ""
                } else {
                    errorMessage = "Impossibile recuperare i dati. Errore: ${response.code()}"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "loadUserProfile fallita", e)
                errorMessage = "Impossibile caricare il profilo. Controlla la connessione e riprova."
            } finally {
                isLoading = false
            }
        }
    }

    fun uploadProfilePicture(context: Context, uri: Uri) {
        viewModelScope.launch {
            isUploadingImage = true
            errorMessage = null
            var tempFile: File? = null
            try {
                tempFile = withContext(Dispatchers.IO) { copyUriToCache(context, uri) }
                if (tempFile == null) {
                    errorMessage = "Errore durante la lettura dell'immagine."
                    return@launch
                }

                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

                val response = profileApi.uploadProfilePicture(body)

                if (response.isSuccessful && response.body() != null) {
                    profilePictureUrl = response.body()?.get("imageUrl")
                } else {
                    errorMessage = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "Errore durante il caricamento dell'immagine (${response.code()})."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "uploadProfilePicture fallita", e)
                errorMessage = "Errore di rete durante il caricamento. Riprova."
            } finally {
                tempFile?.let { runCatching { it.delete() } }
                isUploadingImage = false
            }
        }
    }

    private fun copyUriToCache(context: Context, uri: Uri): File? {
        val tempFile = File.createTempFile("profile_img", ".jpg", context.cacheDir)
        return try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream == null) {
                tempFile.delete()
                return null
            }
            stream.use { input -> FileOutputStream(tempFile).use { output -> input.copyTo(output) } }
            tempFile
        } catch (e: Exception) {
            android.util.Log.e("ProfileViewModel", "copyUriToCache fallita", e)
            tempFile.delete()
            null
        }
    }

    fun updateProfile(
        newName: String,
        newSurname: String,
        newPhone: String,
        newAddress: String,
        newEmail: String,
        newPassword: String?,

        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val request = UpdateProfileRequest(
                    name = newName.ifBlank { null },
                    surname = newSurname.ifBlank { null },
                    phone = newPhone.ifBlank { null },
                    address = newAddress.ifBlank { null },
                    email = newEmail.ifBlank { null },
                    newPassword = if (newPassword.isNullOrBlank()) null else newPassword
                )

                val response = profileApi.updateProfile(request)

                if (response.isSuccessful) {
                    if (newName.isNotBlank()) name = newName
                    if (newSurname.isNotBlank()) surname = newSurname
                    if (newEmail.isNotBlank()) email = newEmail
                    if (newPhone.isNotBlank()) phone = newPhone
                    if (newAddress.isNotBlank()) address = newAddress
                    onSuccess()
                } else {
                    errorMessage = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "Errore durante il salvataggio: ${response.code()}"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Salvataggio profilo fallito", e)
                errorMessage = "Errore di rete durante il salvataggio. Riprova."
            } finally {
                isLoading = false
            }
        }
    }

    suspend fun getIdToken(): String? {
        return tokenManager.getIdToken()
    }

    fun getEndSessionIntent(context: Context, idToken: String): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("${BuildConfig.KEYCLOAK_BASE_URL}/realms/tripify/protocol/openid-connect/auth"),
            Uri.parse("${BuildConfig.KEYCLOAK_BASE_URL}/realms/tripify/protocol/openid-connect/token")
        )

        val endSessionEndpoint = Uri.parse("${BuildConfig.KEYCLOAK_BASE_URL}/realms/tripify/protocol/openid-connect/logout")

        val endSessionConfig = AuthorizationServiceConfiguration(
            serviceConfig.authorizationEndpoint,
            serviceConfig.tokenEndpoint,
            null,
            endSessionEndpoint
        )

        val endSessionRequest = EndSessionRequest.Builder(endSessionConfig)
            .setIdTokenHint(idToken)
            .setPostLogoutRedirectUri(Uri.parse("com.tripify.app://oauth"))
            .build()

        val service = authService ?: AuthorizationService(context.applicationContext).also { authService = it }
        return service.getEndSessionRequestIntent(endSessionRequest)
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearTokens()
            name = ""
            surname = ""
            email = ""
            profilePictureUrl = null
            isLoggedOut = true
        }
    }

    var organizersError by mutableStateOf<String?>(null)

    fun loadOrganizers() {
        viewModelScope.launch {
            isLoadingOrganizers = true
            organizersError = null
            try {
                organizersList = profileApi.getOrganizers()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "loadOrganizers fallita", e)
                organizersError = "Impossibile caricare gli organizzatori. Controlla la connessione e riprova."
            } finally {
                isLoadingOrganizers = false
            }
        }
    }

    fun updatePec(newPec: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = profileApi.updatePec(UpdatePecRequest(newPec))
                if (response.isSuccessful) {
                    pec = newPec
                    onSuccess()
                } else {
                    errorMessage = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "Errore durante il salvataggio: ${response.code()}"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Salvataggio profilo fallito", e)
                errorMessage = "Errore di rete durante il salvataggio. Riprova."
            } finally {
                isLoading = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        authService?.dispose()
        authService = null
    }
}
