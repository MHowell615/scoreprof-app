package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ActivateAccountRequest(
    val user_token: String,
    val _email: String,
    val _language: String
)
