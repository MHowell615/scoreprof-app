package cloud.scoreprof.app.ui.view_models

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.data.SetupRepository
import cloud.scoreprof.app.domain.model.LoginRequest
import cloud.scoreprof.app.domain.model.LoginResponse
import cloud.scoreprof.app.domain.model.PwResetRequest
import cloud.scoreprof.app.domain.model.PwResetResponse
import cloud.scoreprof.app.domain.model.ResetPwRequest
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(
    private val repository: SetupRepository,
    private val tokenManager: TokenManager,
    private val httpClient: HttpClient,
    private val platform: cloud.scoreprof.app.Platform
) : ViewModel() {

    private val _email = mutableStateOf("")
    val email: State<String> = _email

    private val _password = mutableStateOf("")
    val password: State<String> = _password

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _isLoginMode = MutableStateFlow(true)
    val isLoginMode = _isLoginMode.asStateFlow()

    fun onEmailChange(value: String) { _email.value = value }
    fun onPasswordChange(value: String) { _password.value = value }

    fun setLoginMode(isLogin: Boolean) {
        _isLoginMode.value = isLogin
    }

    fun toggleMode() {
        _isLoginMode.value = !_isLoginMode.value
    }

    fun login() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()

        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.Error("Fields cannot be empty")) }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val httpResponse = httpClient.post("https://www.scoreprof.cloud/rpc/new_login") {
                    contentType(ContentType.Application.Json)
                    header("Accept", "application/vnd.pgrst.object+json")
                    setBody(LoginRequest(
                        email_input = currentEmail,
                        pass_input = currentPassword,
                        lang_input = platform.language,
                        v_input = 14, 
                        is_adult_input = true
                    ))
                }

                val responseBody = httpResponse.bodyAsText()
                println("Login response body: $responseBody")

                if (httpResponse.status.value !in 200..299) {
                    // Log the actual server error message (e.g., from Postgres)
                    println("Server Error 400 details: $responseBody")
                    
                    // Force log to database via existing repository method
                    repository.logError(
                        "Signup 400: $responseBody", 
                        "Status: ${httpResponse.status.value}", 
                        "LoginViewModel"
                    )

                    _eventFlow.emit(UiEvent.Error("Server error ${httpResponse.status.value}: $responseBody"))
                    return@launch
                }

                if (responseBody.trim() == "{}" || responseBody.isBlank() || responseBody == "null") {
                    _eventFlow.emit(UiEvent.Error("Account error: No data returned from server."))
                    return@launch
                }

                val loginResponse = Json { ignoreUnknownKeys = true }.decodeFromString<LoginResponse>(responseBody)

                tokenManager.saveToken(loginResponse.token)
                tokenManager.saveUserId(loginResponse.u_id)
                tokenManager.saveEmail(currentEmail)

                // Log Analytics Event
                val isLogin = _isLoginMode.value
                val eventName = if (isLogin) "login" else "sign_up_complete"
                platform.logEvent(eventName, mapOf("method" to "email"))

                // Update server-side profile language immediately after login
                val currentLang = platform.language
                try {
                    repository.updateLanguage(currentLang)
                } catch (e: Exception) {
                    println("Failed to sync language after login: ${e.message}")
                }

                _eventFlow.emit(UiEvent.LoginSuccess(loginResponse.u_id, currentEmail))
            } catch (e: Exception) {
                println("Login failed: ${e.message}")
                _eventFlow.emit(UiEvent.Error("Connection failed. Please try again."))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String) {
        val currentEmail = email.trim()
        if (currentEmail.isBlank()) return

        _isLoading.value = true // Start loader

        viewModelScope.launch {
            try {
                httpClient.post("https://www.scoreprof.cloud/api/reset_password") {
                    contentType(ContentType.Application.Json)
                    setBody(ResetPwRequest(
                            _email = currentEmail,
                            _code = code.trim(),
                            _new_password = newPassword.trim()
                        )
                    )
                }
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.Error("Reset password failed: ${e.message}"))
            } finally {
                _isLoading.value = false
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.ResetPasswordSuccess("Reset password successful"))
                }
            }
        }
    }

    fun requestReset(email: String) {
        val currentEmail = email.trim()
        if (currentEmail.isBlank()) return

        _isLoading.value = true // Start loader

        viewModelScope.launch {
            try {
                val response: PwResetResponse = httpClient.post("https://www.scoreprof.cloud/api/request_password_reset") {
                    contentType(ContentType.Application.Json)
                    setBody(PwResetRequest(
                        email_input = currentEmail,
                        language_input = platform.language,
                        auth_key_input = "" // TODO: Find secure way to store/get SPROF_AUTH_KEY with KMP
                    ))
                }.body()
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.Error("Login failed: ${e.message}"))
            } finally {
                _isLoading.value = false
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.RequestResetSuccess("Reset request sent"))
                }
            }
        }
    }

    sealed class UiEvent {
        data class LoginSuccess(val userId: String, val email: String): UiEvent()
        data class RequestResetSuccess(val message: String): UiEvent()
        data class ResetPasswordSuccess(val message: String): UiEvent()
        data class Error(val message: String): UiEvent()
    }
}
