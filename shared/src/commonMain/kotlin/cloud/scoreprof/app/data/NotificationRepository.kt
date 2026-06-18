package cloud.scoreprof.app.data

import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.AppNotification
import cloud.scoreprof.app.domain.model.NotificationResponse
import cloud.scoreprof.app.domain.model.SendNotification
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface NotificationRepository {
    suspend fun sendNotification(notification: SendNotification)
    suspend fun fetchNotifications(token: String, email: String): List<AppNotification>
    suspend fun markAsReadNotification(email: String, id: Int)
    suspend fun acceptJoinRequest(token: String, leagueId: String, joinerId: String): Boolean
}

class NotificationRepositoryImpl(
    private val dao: ScoreProfDao,
    private val tokenManager: TokenManager,
    private val httpClient: HttpClient
) : NotificationRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _notifications = MutableStateFlow(
        NotificationResponse(notifications = emptyList())
    )
    val notifications: StateFlow<NotificationResponse> = _notifications

    override suspend fun fetchNotifications(token: String, email: String): List<AppNotification> {
        return withContext(Dispatchers.IO) {
            val url = "https://www.scoreprof.cloud/rpc/getnotifications"
            val body = buildJsonObject {
                put("user_token", token)
                put("_email", email)
            }

            val result: String = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()

            val response = json.decodeFromString<NotificationResponse>(result)
            dao.updateAllNotifications(response.notifications)
            _notifications.value = response
            response.notifications
        }
    }

    override suspend fun acceptJoinRequest(token: String, leagueId: String, joinerId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val url = "https://www.scoreprof.cloud/rpc/accept_join_request"
            val body = buildJsonObject {
                put("user_token", token)
                put("_leagueid", leagueId)
                put("_joinerid", joinerId)
            }

            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            true
        }
    }

    override suspend fun markAsReadNotification(email: String, id: Int) {
        withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/mark_as_read_notification"
            val body = buildJsonObject {
                put("user_token", token)
                put("_id", id)
                put("_email", email)
            }

            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    override suspend fun sendNotification(notification: SendNotification) {
        withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/send_invite"
            val body = buildJsonObject {
                put("user_token", token)
                put("_email", notification.email)
                put("_leagueid", notification.leagueid)
                put("_isread", notification.isRead)
                put("_type", notification.type.toString())
            }

            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }
}
