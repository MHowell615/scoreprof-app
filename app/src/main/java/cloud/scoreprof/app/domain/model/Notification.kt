package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import cloud.scoreprof.app.ui.utils.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// Notifications received
@Serializable
@Entity(tableName = "appnotification")
data class AppNotification(
    @PrimaryKey
    @SerialName("id")
    val notificationid: Int,
    val email: String,
    val leagueid: String?,
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID?,
    val ownername: String?,
    val isread: Boolean = false,
    val type: NotificationType,
    val lastmodified: String,
    val title: String? = null,
    val message: String? = null,
    val joinerid: String?
)

@Serializable
data class NotificationResponse(
    val notifications: List<AppNotification>
)

// Mark as read
@Serializable
data class MarkAsReadNotification(
    val id: Int,
    val email: String
)

// Notifications Sent
data class SendNotification(
    val email: String,
    val leagueid: String?,
    val isRead: Boolean = false,
    val type: NotificationType
)

enum class NotificationType {
    LEAGUE_INVITE,
    JOIN_REQUEST,
    GENERAL;

    // Helper to check if it's an actionable request
    fun isActionable(): Boolean = this == JOIN_REQUEST || this == LEAGUE_INVITE
}