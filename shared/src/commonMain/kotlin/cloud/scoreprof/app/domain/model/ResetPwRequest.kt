package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ResetPwRequest(
    val _email: String,
    val _code: String,
    val _new_password: String
)
