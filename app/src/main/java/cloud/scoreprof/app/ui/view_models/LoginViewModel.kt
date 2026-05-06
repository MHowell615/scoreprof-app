package cloud.scoreprof.app.ui.view_models

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import cloud.scoreprof.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.text.intl.Locale
import cloud.scoreprof.app.BuildConfig
import org.json.JSONObject
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val context: Context

) : ViewModel() {

    private val _email = mutableStateOf("")
    val email: State<String> = _email

    private val _password = mutableStateOf("")
    val password: State<String> = _password

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    fun onEmailChange(value: String) { _email.value = value }
    fun onPasswordChange(value: String) { _password.value = value }

    fun login() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()

        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.Error("Fields cannot be empty")) }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            // 1. Prepare the JSON Body for PostgreSQL RPC
            val params = JSONObject().apply {
                put("email_input", currentEmail)
                put("pass_input", currentPassword)
            }

            val url = "https://www.scoreprof.cloud/rpc/login"

            // 2. The Volley Request
            val stringRequest = object: com.android.volley.toolbox.StringRequest(
                Method.POST, url,
                { response ->
                    // SUCCESS CALLBACK
                    try {
                        println("ScoreProfAuthLog: Raw Response: $response") // Add this to see what's happening

                        val jsonResponse = if (response.trim().startsWith("[")) {
                            val array = org.json.JSONArray(response)
                            if (array.length() > 0) array.getJSONObject(0) else null
                        } else {
                            JSONObject(response)
                        }

                        if (jsonResponse != null && jsonResponse.has("token")) {
                            val token = jsonResponse.getString("token")
                            val userIdString = jsonResponse.getString("u_id")
                            val userId = UUID.fromString(userIdString)

                            tokenManager.saveToken(token)
                            tokenManager.saveUserId(userIdString)

                            viewModelScope.launch {
                                _eventFlow.emit(UiEvent.LoginSuccess(userId, email.value))
                            }
                        } else {
                            throw Exception("Response missing token or userid")
                        }
                    } catch (e: Exception) {
                        println("ScoreProfAuthLog: Parsing error: ${e.message}. Response: $response")
                        viewModelScope.launch {
                            _eventFlow.emit(UiEvent.Error("Login failed: ${e.localizedMessage}"))
                        }
                    } finally {
                        _isLoading.value = false
                    }

                },
                { error ->
                    // ERROR CALLBACK
                    val statusCode = error.networkResponse?.statusCode
                    val serverData = error.networkResponse?.data?.let { String(it) }

                    val message = when (statusCode) {
                        404 -> "API Endpoint not found. Check server configuration."
                        401, 403 -> "Invalid email or password."
                        400 -> {
                            // Try to extract the Postgres error message from the response body
                            if (serverData != null) {
                                try {
                                    val json = JSONObject(serverData)
                                    val pgMessage = json.optString("message", "Bad Request")
                                    "Server Error: $pgMessage"
                                } catch (e: Exception) {
                                    "Bad Request (400): $serverData"
                                }
                            } else "Bad Request (400)"
                        }
                        else -> "Error $statusCode: ${serverData ?: "Unknown error"}"
                    }

                    viewModelScope.launch {
                        _eventFlow.emit(UiEvent.Error(message))
                        // Improved logging for debugging
                        val logCode = JSONObject(serverData ?: "{}").optString("code", "N/A")
                        println("ScoreProfAuthLog: [Status $statusCode] PG_Code: $logCode, serverData: $serverData")

                        if (error.networkResponse == null) {
                            println("ScoreProfAuthLog: No network response. Check internet/SSL.")
                        }

                        _isLoading.value = false
                    }
                }
            ) {
                override fun getBody(): ByteArray = params.toString().toByteArray(Charsets.UTF_8)
                override fun getBodyContentType(): String = "application/json; charset=utf-8"
            }

            //println("ScoreProfAuthLog: Sending login for $currentEmail. Password length: ${currentPassword.length}")

            stringRequest.retryPolicy = DefaultRetryPolicy(
                15000, // 10 seconds (Hashing 'bf' with 10 rounds takes time)
                0,     // No retries to avoid multiple hash attempts
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )

            requestQueue.add(stringRequest)
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Prepare the JSON Body for PostgreSQL RPC
                val params = JSONObject().apply {
                    put("email", email.trim())
                    put("code", code.trim())
                    put("new_password", newPassword.trim())
                }

                val url = "https://www.scoreprof.cloud/rpc/reset_password"

                val request = object : StringRequest(
                    Method.POST, url,
                    { response ->
                        _isLoading.value = false
                        println("ScoreProfAuthLog: Password reset successful for $email")
                    },
                    { error ->
                        _isLoading.value = false
                        val serverData = error.networkResponse?.data?.let { String(it) }
                        println("ScoreProfAuthLog: Error resetting password: $serverData")
                        val errorMessage = try {
                            val json = JSONObject(serverData ?: "{}") //.optString("error", "Reset failed")
                            json.optString("error", "Reset failed")
                        } catch (e: Exception) {
                            "Invalid reset code or expired."
                        }

                        //println("ScoreProfAuthLog: [400 ERROR DETAIL]: $serverData")

                        viewModelScope.launch {
                            _eventFlow.emit(UiEvent.Error(errorMessage))
                        }
                    }
                ) {
                    override fun getBody(): ByteArray =
                        params.toString().toByteArray(Charsets.UTF_8)

                    override fun getBodyContentType(): String = "application/json; charset=utf-8"
                }
                request.retryPolicy = DefaultRetryPolicy(
                    20000, // 20 seconds
                    0,     // No retries to avoid multiple hashing attempts
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )

                requestQueue.add(request)
            } catch (e: Exception) {
                println("TEST: [ResetPassword] Error: ${e.message}")
            }
        }
    }

    fun requestReset(email: String) {
        viewModelScope.launch {
            val params = JSONObject().apply {
                put("email", email)
            }

            val url = "https://www.scoreprof.cloud/rpc/request_password_reset"
            val authKey = BuildConfig.SPROF_AUTH_KEY
            val language = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"

            val jsonBody = JSONObject().apply {
                put("email", email)
                put("language", language)
                put("authKey", authKey)
            }

            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                { response ->
                    _isLoading.value = false
                    println("ScoreProfAuthLog: Reset code requested for $email")
                },
                { error ->
                    _isLoading.value = false
                    val serverData = error.networkResponse?.data?.let { String(it) }
                    val message = "Failed to send reset code. Please check your email and try again."
                    viewModelScope.launch {
                        _eventFlow.emit(UiEvent.Error(message))
                    }
                }
            )
            // Set timeout to 15s to allow SES to process
            request.retryPolicy = DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            requestQueue.add(request)
        }
    }

    sealed class UiEvent {
        data class LoginSuccess(val userId: UUID, val email: String): UiEvent()
        data class Error(val message: String): UiEvent()
    }
}