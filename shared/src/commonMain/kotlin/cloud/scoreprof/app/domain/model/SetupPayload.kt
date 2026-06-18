package cloud.scoreprof.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SetupPayload(
    // PostgREST calls RPC functions using named arguments in the JSON body.
    // The JSON keys MUST exactly match the function's parameter names.
    // We will use @SerialName to send the underscore versions the server expects.

    @SerialName("_userid")
    
    val userid: String,
    @SerialName("_name")
    val name: String?,
    @SerialName("_membersince")
    val memberSince: String?,
    @SerialName("_preferred_language")
    val preferredLanguage: String?,
    @SerialName("_competitions")
    val competitions: List<UserCompetitionState>,
    @SerialName("_userleagues")
    val userleagues: List<UserLeagueState>,
    @SerialName("_leagues")
    val leagues: List<LeagueState>
)

@Serializable
data class UserCompetitionState(
    @SerialName("competitionid")
    val competitionid: String,
    @kotlinx.serialization.Transient
    @SerialName("sport_type")
    val sport_type: String? = null,
    @kotlinx.serialization.Transient
    @SerialName("region")
    val region: String? = null,
    @SerialName("selected")
    val selected: Boolean
)

@Serializable
data class UserLeagueState(
    
    @SerialName("userid")
    val userid: String,
    @SerialName("leagueid")
    val leagueid: String,
    
    @SerialName("owneruserid")
    val owneruserid: String,
    @SerialName("invited")
    val invited: Boolean,
    @SerialName("selected")
    val selected: Boolean
)

@Serializable
data class LeagueState(
    @SerialName("leagueid")
    val leagueid: String,
    @SerialName("owneruserid")
    
    val owneruserid: String,
    @SerialName("state")
    val state: String?,
    @SerialName("leaguename")
    val leaguename: String
)
