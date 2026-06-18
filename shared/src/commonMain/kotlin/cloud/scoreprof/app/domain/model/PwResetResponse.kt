package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PwResetResponse(
    val status: String,
    val http_code: String
)
