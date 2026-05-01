package cloud.scoreprof.app.domain.model

@kotlinx.serialization.Serializable
data class InviteEmailRequest(
    val to: String,
    val leagueid: String,        // The PK string
    val owneruserid: String,    // The UUID as string
    val senderName: String,
    val authKey: String
)