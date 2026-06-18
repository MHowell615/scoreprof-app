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
    val min_version: Int,
    val update_url: String
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
            if (response.update_url.isNotBlank()) {
                _updateUrl.value = response.update_url
                _isUpdateRequired.value = currentVersion < response.min_version
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}
