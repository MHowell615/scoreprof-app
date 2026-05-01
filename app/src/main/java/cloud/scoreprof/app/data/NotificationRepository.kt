package cloud.scoreprof.app.data

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.AppNotification
import cloud.scoreprof.app.domain.model.NotificationResponse
import cloud.scoreprof.app.domain.model.SendNotification
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject
import kotlin.collections.emptyList
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


interface NotificationRepository {
    suspend fun sendNotification(notification: SendNotification)
    suspend fun fetchNotifications(token: String, email: String): List<AppNotification>
    suspend fun markAsReadNotification(email: String, id: Int)
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ScoreProfDao,
    private val tokenManager: TokenManager,
    private val json: Json
) : NotificationRepository {
    private val requestQueue = Volley.newRequestQueue(context)

    private var _notifications = MutableStateFlow(
        NotificationResponse(notifications = emptyList())
    )
    val notifications: StateFlow<NotificationResponse> = _notifications

    // Companion object for constants
    companion object {
        private const val MAX_RETRIES = 2 // This will result in 1 initial attempt + 2 retries = 3 total attempts
        private const val INITIAL_TIMEOUT_MS = 5000 // 5 seconds
    }

    // In NotificationRepository.kt
    override suspend fun fetchNotifications(token: String, email: String): List<AppNotification> {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/getnotifications"

            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_email", email)
                }
            } catch (e: Exception) {
                println("TEST: [NotificationRepository] JSON Construction failed on fetching notifications: ${e.message}")
                throw e
            }

            // Serialize the body
            val requestBody = jsonBody.toString()

            val result = suspendCancellableCoroutine<String> { continuation ->
                val stringRequest = object : StringRequest(
                    Method.POST,
                    url,
                    { response ->
                        if (continuation.isActive) {
                            continuation.resume(response)
                        }
                    },
                    { error ->
                        val responseBody =
                            error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                        println("Server error: ${error.message}, Body: $responseBody")
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        return headers
                    }

                    override fun getBodyContentType(): String {
                        return "application/json; charset=utf-8"
                    }

                    override fun getBody(): ByteArray {
                        return requestBody.toByteArray(Charsets.UTF_8)
                    }
                }

                stringRequest.retryPolicy = DefaultRetryPolicy(
                    INITIAL_TIMEOUT_MS,
                    MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )

                continuation.invokeOnCancellation {
                    stringRequest.cancel()
                }

                requestQueue.add(stringRequest)
            }
            println("TEST: [NotificationRepository] Result: $result")
            val response = json.decodeFromString<NotificationResponse>(result)

            dao.updateAllNotifications(response.notifications)
            _notifications = MutableStateFlow(response)
            //_notifications.value = response
            response.notifications
        }
    }

    override suspend fun markAsReadNotification(email: String, id: Int) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/mark_as_read_notification"

            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_id", id)
                    put("_email", email)
                }
            } catch (e: Exception) {
                println("TEST: [NotificationRepository] JSON Construction failed on marking notification as read")
                throw e
            }

            // Serialize the body
            val requestBody = jsonBody.toString()

            suspendCancellableCoroutine<String> { continuation ->
                val stringRequest = object : StringRequest(
                    Method.POST,
                    url,
                    { response ->
                        if (continuation.isActive) {
                            continuation.resume(response)
                        }
                    },
                    { error ->
                        val responseBody =
                            error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                        println("Server error: ${error.message}, Body: $responseBody")
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        return headers
                    }

                    override fun getBodyContentType(): String {
                        return "application/json; charset=utf-8"
                    }

                    override fun getBody(): ByteArray {
                        return requestBody.toByteArray(Charsets.UTF_8)
                    }
                }

                stringRequest.retryPolicy = DefaultRetryPolicy(
                    INITIAL_TIMEOUT_MS,
                    MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )

                continuation.invokeOnCancellation {
                    stringRequest.cancel()
                }

                requestQueue.add(stringRequest)
            }
        }
    }

    override suspend fun sendNotification(
        notification: SendNotification
    ) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/send_invite"

            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_email", notification.email)
                    put("_leagueid", notification.leagueid)
                    put("_isread", notification.isRead)
                    put("_type", notification.type.toString())
                }
            } catch (e: Exception) {
                println("TEST: [NotificationRepository] JSON Construction failed on sending notifications: ${e.message}")
                throw e
            }

            // Serialize the body
            val requestBody = jsonBody.toString()

            suspendCancellableCoroutine<String> { continuation ->
                val stringRequest = object : StringRequest(
                    Method.POST,
                    url,
                    { response ->
                        if (continuation.isActive) {
                            continuation.resume(response)
                        }
                    },
                    { error ->
                        val responseBody =
                            error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                        println("Server error: ${error.message}, Body: $responseBody")
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        return headers
                    }

                    override fun getBodyContentType(): String {
                        return "application/json; charset=utf-8"
                    }

                    override fun getBody(): ByteArray {
                        return requestBody.toByteArray(Charsets.UTF_8)
                    }
                }

                stringRequest.retryPolicy = DefaultRetryPolicy(
                    INITIAL_TIMEOUT_MS,
                    MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )

                continuation.invokeOnCancellation {
                    stringRequest.cancel()
                }

                requestQueue.add(stringRequest)
            }
        }
    }
}