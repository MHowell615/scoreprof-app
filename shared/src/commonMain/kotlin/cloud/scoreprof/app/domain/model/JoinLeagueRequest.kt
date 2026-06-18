package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class JoinLeagueRequest(
    val _league_code: String,
    val user_token: String
)
