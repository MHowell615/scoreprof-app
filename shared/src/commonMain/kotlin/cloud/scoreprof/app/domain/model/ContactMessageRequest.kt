package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ContactMessageRequest(
    val user_token: String,
    val _category: String,
    val _subject: String,
    val _details: String
)
