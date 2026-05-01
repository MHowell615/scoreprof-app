package cloud.scoreprof.app.data

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.UserPredictionUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.apply
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Interface defining the contract
interface PredictionUpdateRepository {
    // This function will now throw an exception on failure
    suspend fun updatePredictionOnServer(predictionUpdate: UserPredictionUpdate)
}

// Implementation class
@Singleton
class PredictionUpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager, // Inject TokenManager here
    private val json: Json
) : PredictionUpdateRepository {

    private val requestQueue = Volley.newRequestQueue(context)

    // Companion object for constants
    companion object {
        private const val MAX_RETRIES = 2 // This will result in 1 initial attempt + 2 retries = 3 total attempts
        private const val INITIAL_TIMEOUT_MS = 5000 // 5 seconds
    }

    override suspend fun updatePredictionOnServer(predictionUpdate: UserPredictionUpdate) {
        // Wrap the callback-based Volley request in a coroutine
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_user_prediction"

            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_competitor1selected", predictionUpdate.competitor1selected)
                    put("_competitor2selected", predictionUpdate.competitor2selected)
                    put("_matchid", predictionUpdate.matchid)
                }
            } catch (e: Exception) {
                println("TEST: [PredictionsRepository] JSON Construction Failed: ${e.message}")
                throw e
            }

            // Serialize the body
            val requestBody = jsonBody.toString()

            val result = suspendCancellableCoroutine<String> { continuation ->
                val stringRequest = object : StringRequest(
                    Method.POST,
                    url,
                    { response ->
                        if (continuation.isActive) {
                            continuation.resume(response)
                        }
                    },
                    { error ->
                        val responseBody =
                            error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                        println("Server error: ${error.message}, Body: $responseBody")
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        return headers
                    }

                    override fun getBodyContentType(): String {
                        return "application/json; charset=utf-8"
                    }

                    override fun getBody(): ByteArray {
                        return requestBody.toByteArray(Charsets.UTF_8)
                    }
                }

                stringRequest.retryPolicy = DefaultRetryPolicy(
                    INITIAL_TIMEOUT_MS,
                    MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )

                continuation.invokeOnCancellation {
                    stringRequest.cancel()
                }

                requestQueue.add(stringRequest)
            }
        }
    }
}
