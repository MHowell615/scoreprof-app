package cloud.scoreprof.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPredictionUpdate(
    @SerialName("_userid")
    val userid: String,
    @SerialName("_matchid")
    val matchid: Int,
    @SerialName("_competitor1selected")
    val competitor1selected: Boolean,
    @SerialName("_competitor2selected")
    val competitor2selected: Boolean
)
