package cloud.scoreprof.app.domain.model

import cloud.scoreprof.app.ui.utils.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A simple Data Transfer Object (DTO) representing the data needed
 * to update a single user prediction on the server. This is NOT a database entity.
 */
@Serializable
data class UserPredictionUpdate(
    // The SerialNames are used in the JSON output to the API
    @SerialName("_userid")
    @Serializable(with = UUIDSerializer::class)
    val userid: UUID,

    @SerialName("_matchid")
    val matchid: Int,

    @SerialName("_competitor1selected")
    val competitor1selected: Boolean,

    @SerialName("_competitor2selected")
    val competitor2selected: Boolean

)