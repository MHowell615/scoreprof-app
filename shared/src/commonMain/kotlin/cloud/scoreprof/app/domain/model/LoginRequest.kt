package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email_input: String,
    val pass_input: String,
    val lang_input: String,
    val v_input: Int,
    val is_adult_input: Boolean
)
