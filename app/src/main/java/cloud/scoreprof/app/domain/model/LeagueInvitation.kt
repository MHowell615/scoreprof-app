package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import cloud.scoreprof.app.ui.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(tableName = "league_invitations")
data class LeagueInvitation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leagueid: String,
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID,
    val invited_user_email: String,
    val status: String           // e.g., "pending", "accepted", "declined"
)