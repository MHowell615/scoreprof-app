package cloud.scoreprof.app.data

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import cloud.scoreprof.app.BuildConfig
import cloud.scoreprof.app.data.local.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface VersionRepository {
    val isUpdateRequired: StateFlow<Boolean>
    val updateUrl: StateFlow<String>
    suspend fun checkAppVersion()
}

@Singleton
class VersionRepositoryImpl @Inject constructor(
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : VersionRepository {

    private val requestQueue = Volley.newRequestQueue(context)

    private val _isUpdateRequired = MutableStateFlow(false)
    override val isUpdateRequired: StateFlow<Boolean> = _isUpdateRequired

    private val _updateUrl = MutableStateFlow("")
    override val updateUrl: StateFlow<String> = _updateUrl

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val MAX_RETRIES = 2 // 1 initial attempt + 2 retries = 3 total
        private const val INITIAL_TIMEOUT_MS = 5000 // 5 seconds
    }

    override suspend fun checkAppVersion() {
        val url = "https://www.scoreprof.cloud/rpc/check_version"
        val currentVersion = BuildConfig.VERSION_CODE

        val request = object: JsonObjectRequest(
            Request.Method.POST, url, org.json.JSONObject(),
            { response ->
                try {
                    // Match the keys you inserted into your Postgres settings table
                    val minVersion = response.getInt("min_version")
                    val serverUrl = response.getString("update_url")
                    if (serverUrl.isNotBlank()) {
                        _updateUrl.value = serverUrl
                        println("[VersionRepo] Update URL: $serverUrl, min_version: $minVersion, currentVersion = $currentVersion")
                        // Trigger the lockout only if we have a valid URL to send them to
                        _isUpdateRequired.value = currentVersion < minVersion
                    }
                    Log.d(
                        "VersionCheck",
                        "Success! Server Min: $minVersion, Local: $currentVersion"
                    )
                } catch (e: Exception) {
                    Log.e("VersionCheck", "Parsing Error: ${e.message}")
                }
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode
                Log.e("VersionCheck", "Failed to check version. Status: $statusCode, Msg: ${error.message}")

            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cache-Control"] = "no-cache, no-store, must-revalidate"
                headers["Pragma"] = "no-cache"
                return headers
            }
        }

        // 3. Set a shorter timeout for the version check to keep the UI snappy
        request.retryPolicy = com.android.volley.DefaultRetryPolicy(
            INITIAL_TIMEOUT_MS,
            MAX_RETRIES,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)
    }
}