package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppVersionInfo(
    val minVersionCode: Int,
    val latestVersionName: String,
    val forceUpdate: Boolean,
    val updateUrl: String
)