package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val u_id: String,
    val token: String
)
