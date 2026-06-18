package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PwResetRequest (
    val email_input: String,
    val language_input: String,
    val auth_key_input: String
)

