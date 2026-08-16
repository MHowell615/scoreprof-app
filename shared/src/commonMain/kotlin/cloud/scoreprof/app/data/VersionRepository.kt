package cloud.scoreprof.app.data

import cloud.scoreprof.app.Platform
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface VersionRepository {
    val isUpdateRequired: StateFlow<Boolean>
    val updateUrl: StateFlow<String>
    suspend fun checkAppVersion()
}

@Serializable
data class VersionResponse(
    val android_min_version: Int? = null,
    val ios_min_version: Int? = null,
    val android_update_url: String? = null,
    val ios_update_url: String? = null,
    // Keep old fields for backward compatibility if needed, or remove them
    val min_version: Int? = null,
    val update_url: String? = null
)

class VersionRepositoryImpl(
    private val httpClient: HttpClient,
    private val platform: Platform
) : VersionRepository {

    private val _isUpdateRequired = MutableStateFlow(false)
    override val isUpdateRequired: StateFlow<Boolean> = _isUpdateRequired

    private val _updateUrl = MutableStateFlow("")
    override val updateUrl: StateFlow<String> = _updateUrl

    override suspend fun checkAppVersion() {
        val url = "https://www.scoreprof.cloud/rpc/check_version"
        val currentVersion = platform.version

        try {
            val response: VersionResponse = httpClient.post(url) {
                header("Accept", "application/vnd.pgrst.object+json")
            }.body()

            val minVersion = if (platform.isAndroid) {
                response.android_min_version ?: response.min_version ?: 0
            } else {
                response.ios_min_version ?: response.min_version ?: 0
            }

            val updateUrl = if (platform.isAndroid) {
                response.android_update_url ?: response.update_url ?: ""
            } else {
                response.ios_update_url ?: response.update_url ?: ""
            }

            if (updateUrl.isNotBlank()) {
                _updateUrl.value = updateUrl
                _isUpdateRequired.value = currentVersion < minVersion
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}
