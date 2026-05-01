package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import cloud.scoreprof.app.ui.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(
    tableName = "Leagues",
    indices = [androidx.room.Index(value = ["leagueid", "owneruserid"], unique = true)]
)
data class Leagues(
    @PrimaryKey val id: Int,
    val leagueid: String,
    val competitionid: String?,
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID,
    val name: String,
    val state: String,
    val invited: Boolean,
    val selected: Boolean
)

@Serializable
@Entity(tableName = "Leagues_Header")
data class LeaguesHeader(
    @PrimaryKey
    val leagues: List<Leagues>
)

@Serializable
data class LeaguesResponse(
    val leagues: List<Leagues>
)

