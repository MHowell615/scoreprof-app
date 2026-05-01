package cloud.scoreprof.app.domain.model

import cloud.scoreprof.app.ui.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class LoginResponse(val token: String,
                         @Serializable(with = UUIDSerializer::class)
                         val userId: UUID,
                         val username: String,
                         val email: String
)
