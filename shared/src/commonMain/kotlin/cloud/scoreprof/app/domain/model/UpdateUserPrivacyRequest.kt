package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserPrivacyRequest(
    val user_token: String,
    val _receive_email: Boolean
)
