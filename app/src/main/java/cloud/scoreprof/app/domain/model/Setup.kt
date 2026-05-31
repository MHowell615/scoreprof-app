package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import cloud.scoreprof.app.ui.utils.UUIDSerializer
import cloud.scoreprof.app.ui.utils.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.util.UUID

@Serializable
@Entity(tableName = "Setup")
data class Setup(
    @PrimaryKey val id: Int,
    val version: Int,
    val email: String,
    val name: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val userid: UUID,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val memberSince: ZonedDateTime? = null,
    val preferred_language: String,
    val competitions: List<UserCompetitionSelection>,
    val leagues: List<UserLeagueSelection>,
    val receive_email: Boolean = true,
    val is_ads_removed: Boolean = false
)

@Serializable
@Entity(tableName = "UserCompetitionSelection")
data class UserCompetitionSelection(
    @PrimaryKey val id: Int,
    val competitionid: String,
    val sport_type: String? = null,
    val region: String? = null,
    val country_ranking: Int? = null,
    val name: String, // We can ignore this name, as we'll use the one from competitions.json
    val selected: Boolean? = false,
    val has_upcoming: Boolean? = false
)

@Serializable
@Entity(tableName = "UserLeagueSelection")
data class UserLeagueSelection(
    @PrimaryKey val id: Int,
    val leagueid: String,
    val competitionid: String?,
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID,
    val leaguecode: String?,
    val name: String,
    val state: String? = "Active",
    val invited: Boolean? = false,
    val selected: Boolean? = false
)
