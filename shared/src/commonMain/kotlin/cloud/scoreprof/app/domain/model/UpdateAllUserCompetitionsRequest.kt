package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateAllUserCompetitionsRequest(
    val user_token: String,
    val _isselected: Boolean
)
