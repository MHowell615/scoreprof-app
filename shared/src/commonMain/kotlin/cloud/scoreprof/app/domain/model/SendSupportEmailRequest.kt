package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SendSupportEmailRequest(
    val userEmail: String,
    val category: String,
    val subject: String,
    val description: String,
    val authKey: String
)
