package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "league_invitations")
data class LeagueInvitation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leagueid: String,
    val owneruserid: String,
    val invited_user_email: String,
    val status: String
)
