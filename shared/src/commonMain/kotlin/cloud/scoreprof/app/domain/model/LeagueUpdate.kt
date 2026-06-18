package cloud.scoreprof.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeagueUpdate(
    @SerialName("_leagueid")
    val leagueid: String,
    @SerialName("_owneruserid")
    val owneruserid: String,
    @SerialName("_competitionid")
    val competitionid: String,
    @SerialName("_userid")
    val userid: String
)
