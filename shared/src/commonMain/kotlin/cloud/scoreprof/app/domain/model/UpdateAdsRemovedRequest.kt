package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateAdsRemovedRequest(
    val user_token: String,
    val _is_ads_removed: Boolean
)
