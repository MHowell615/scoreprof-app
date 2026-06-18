package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserCompetitionRequest(
    val user_token: String,
    val _competitionid: String,
    val _isselected: Boolean
)
