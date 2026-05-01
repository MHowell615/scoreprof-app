package cloud.scoreprof.app.domain.model

import cloud.scoreprof.app.ui.utils.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SetupPayload(
    // PostgREST calls RPC functions using named arguments in the JSON body.
    // The JSON keys MUST exactly match the function's parameter names.
    // We will use @SerialName to send the underscore versions the server expects.

    @SerialName("_userid")
    @Serializable(with = UUIDSerializer::class)
    val userid: UUID,
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
    @Serializable(with = UUIDSerializer::class)
    @SerialName("userid")
    val userid: UUID,
    @SerialName("leagueid")
    val leagueid: String,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("owneruserid")
    val owneruserid: UUID,
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
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID,
    @SerialName("state")
    val state: String?,
    @SerialName("leaguename")
    val leaguename: String
)
