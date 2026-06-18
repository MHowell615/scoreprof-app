package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserProfileRequest(
    val user_token: String,
    val _username: String,
    val _email: String,
    val _language: String
)
