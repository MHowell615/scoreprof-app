package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserLeagueUpdateRequest(
    val user_token: String,
    val _leagueid: String,
    val _owneruserid: String,
    val _isselected: Boolean
)
