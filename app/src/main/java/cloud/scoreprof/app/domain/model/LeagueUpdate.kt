package cloud.scoreprof.app.domain.model

import cloud.scoreprof.app.ui.utils.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class LeagueUpdate(
    @SerialName("_leagueid")
    val leagueid: String,
    @SerialName("_owneruserid")
    @Serializable(with = UUIDSerializer::class)
    val owneruserid: UUID,
    @SerialName("_competitionid")
    val competitionid: String,
    @SerialName("_userid")
    @Serializable(with = UUIDSerializer::class)
    val userid: UUID
)