package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import cloud.scoreprof.app.ui.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(
    tableName = "League",
    primaryKeys = ["leagueid", "owneruserid"]
)
data class League(
    //@PrimaryKey(autoGenerate = true) val id: Int,
    val leagueid: String,
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID,
    @Serializable(with = UUIDSerializer::class)
    val userid: UUID,
    val username: String//,
    //val position: Int,
    //val matches_played: Int,
    //val points: Int
)

@Serializable
@Entity(
    tableName = "League_Header",
    primaryKeys = ["leagueid", "owneruserid"]
)
data class LeagueHeader(
    //@ColumnInfo(name = "leagueid")
    val leagueid: String,
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID,
    val competitionid: String?,
    val leagueusers: List<League>
)

@Serializable
@Entity(
    tableName = "League_Table",
    primaryKeys = ["leagueid", "owneruserid"]
)
data class LeagueTable(
    val leagueid: String,
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID,
    val rank: Int,
    @Serializable(with = UUIDSerializer::class)
    val userid: UUID,
    val username: String,
    val matches_played: Int,
    val points: Int,
    val win_percentage: Double
)

@Serializable
@Entity(
    tableName = "User_League_Users",
    primaryKeys = ["userid", "leagueid", "owneruserid"]
)
data class UserLeagueUsers(
    @Serializable(with = UUIDSerializer::class)
    val userid: UUID,
    val leagueid: String,
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID,
    val competitionid: String?,
    val invited: Boolean? = false,
    val selected: Boolean? = false,
    val invitestatus: String?,
    val state: String?,
    val email: String
)

@Serializable
data class UserLeague(
    val userleagueusers: List<UserLeagueUsers>
)

@Serializable
data class UpdateLeagueRequest(
    val original_leagueid: String,
    @Serializable(with = UUIDSerializer::class)
    val original_owneruserid: UUID,
    val new_league_header: LeagueHeader,
    val new_user_league: UserLeague
)

@Serializable
data class UserStatusRequest(
    val leagueid: String,
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID
)


