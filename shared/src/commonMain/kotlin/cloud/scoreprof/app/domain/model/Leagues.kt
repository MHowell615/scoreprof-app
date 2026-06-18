package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable


@Serializable
@Entity(
    tableName = "Leagues",
    indices = [androidx.room.Index(value = ["leagueid", "owneruserid"], unique = true)]
)
data class Leagues(
    @PrimaryKey val id: Int,
    val leagueid: String,
    val competitionid: String?,
    val leaguecode: String?,
    
    val owneruserid: String,
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