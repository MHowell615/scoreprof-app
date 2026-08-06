package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import kotlinx.serialization.Serializable


@Serializable
@Entity(
    tableName = "League",
    primaryKeys = ["leagueid", "owneruserid"]
)
data class League(
    //@PrimaryKey(autoGenerate = true) val id: Int,
    val leagueid: String,
    
    val owneruserid: String,
    
    val userid: String,
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
    
    val owneruserid: String,
    val competitionid: String?,
    val leagueusers: List<League>
)

@Serializable
@Entity(
    tableName = "League_Table",
    primaryKeys = ["leagueid", "owneruserid", "userid"]
)
data class LeagueTable(
    val leagueid: String,
    val owneruserid: String,
    val rank: Int,
    val userid: String,
    val username: String,
    val matches_played: Int,
    val points: Int,
    val win_percentage: Double,
    val country_code: String? = null
)

@Serializable
@Entity(
    tableName = "User_League_Users",
    primaryKeys = ["userid", "leagueid", "owneruserid"]
)
data class UserLeagueUsers(
    
    val userid: String,
    val leagueid: String,
    
    val owneruserid: String,
    val competitionid: String?,
    val leaguecode: String?,
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
    
    val original_owneruserid: String,
    val new_league_header: LeagueHeader,
    val new_user_league: UserLeague
)

@Serializable
data class UserStatusRequest(
    val leagueid: String,
    
    val owneruserid: String
)



