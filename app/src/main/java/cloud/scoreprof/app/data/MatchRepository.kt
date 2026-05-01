package cloud.scoreprof.app.data

import android.content.Context
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.Match
import cloud.scoreprof.app.domain.model.MatchHeader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// The interface can remain in the same file or a separate one
interface MatchRepository {
    fun getMatchesByCompetition(competitionId: String): Flow<List<Match>>
    suspend fun upsertMatches(matches: List<Match>)
    suspend fun loadAndCacheMatchesFromJson(competitionId: String, userid: String)
    suspend fun upsertMatch(match: Match)
    suspend fun updateLocalPrediction(
        matchid: Int,
        competitor1selected: Boolean,
        competitor2selected: Boolean
    )
    suspend fun hasMatches(competitionId: String): Boolean
}

class MatchRepositoryImpl @Inject constructor(
    private val dao: ScoreProfDao,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : MatchRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val requestQueue = Volley.newRequestQueue(context)

    // Companion object for constants
    companion object {
        private const val MAX_RETRIES = 2 // 1 initial attempt + 2 retries = 3 total
        private const val INITIAL_TIMEOUT_MS = 5000 // 5 seconds
    }

    override fun getMatchesByCompetition(competitionId: String): Flow<List<Match>> {
        return dao.getMatchesByCompetition(competitionId)
    }

    override suspend fun upsertMatches(matches: List<Match>) {
        withContext(Dispatchers.IO) {
            dao.upsertMatches(matches)
        }
    }

    override suspend fun upsertMatch(match: Match) {
        withContext(Dispatchers.IO) {
            dao.upsertMatch(match)
        }
    }

    override suspend fun updateLocalPrediction(matchid: Int, competitor1selected: Boolean, competitor2selected: Boolean) {
        dao.updatePrediction(matchid, competitor1selected, competitor2selected)
    }

    override suspend fun hasMatches(competitionId: String): Boolean {
        // Ask the DAO for a count of matches. If it's more than 0, we have data.
        return dao.getMatchCountForCompetition(competitionId) > 0
    }

    override suspend fun loadAndCacheMatchesFromJson(competitionId: String, userid: String) {
        // Fetch the raw string from the server.
        try {
            val token = tokenManager.getToken() ?: ""
            val deviceLanguage = java.util.Locale.getDefault().language
            val url =
                "https://www.scoreprof.cloud/rpc/getmatchesbycomp?competition_id=$competitionId&lang=$deviceLanguage&user_token=$token"
            //println("TEST: Calling URL: $url")
            val responseString = fetchPublicFromServer(url)
            //println("TEST: Response: $responseString")

            // Decode the entire MatchHeader object, which includes the list of matches.
            if (!responseString.isNullOrBlank() && responseString != "null") {
                val matchHeader = json.decodeFromString(MatchHeader.serializer(), responseString)
                //println("TEST: MatchHeader: $matchHeader")

                // We use withContext to ensure this database operation runs on an IO thread.
                withContext(Dispatchers.IO) {
                    //upsertMatches(matchHeader.matches)
                    dao.insertMatchesIgnore(matchHeader.matches)
                    matchHeader.matches.forEach { match ->
                        dao.updateMatchResults(
                            matchid = match.id,
                            score1 = match.score1,
                            score2 = match.score2,
                            winner = match.winner,
                            supplement = match.supplement
                        )
                    }
                }
            } else {
                Log.w("MatchRepository", "Received null or empty response from server.")
            }
        } catch (e: Exception) {
            Log.e("MatchRepository", "Failed to fetch and save match data.", e)
            // Re-throw the exception so the ViewModel knows the call failed.
            throw e
        }
    }

    private suspend fun fetchPublicFromServer(url: String): String {
        return suspendCancellableCoroutine { continuation ->
            val stringRequest = object : StringRequest(
                Method.GET, // Use Method.GET directly (imported from Request.Method)
                url,
                { response ->
                    if (continuation.isActive) continuation.resume(response)
                },
                { error ->
                    val responseBody = error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                    Log.e("MatchRepository", "Public Fetch Error: $responseBody")
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            ) {
                override fun parseNetworkResponse(response: com.android.volley.NetworkResponse): com.android.volley.Response<String> {
                    val utf8String = String(response.data, Charsets.UTF_8)
                    return com.android.volley.Response.success(
                        utf8String,
                        com.android.volley.toolbox.HttpHeaderParser.parseCacheHeaders(response)
                    )
                }
            }

            stringRequest.retryPolicy = DefaultRetryPolicy(
                INITIAL_TIMEOUT_MS,
                MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            requestQueue.add(stringRequest)
            continuation.invokeOnCancellation { stringRequest.cancel() }
        }
    }

    private suspend fun fetchFromServer(url: String): String {
        return suspendCancellableCoroutine { continuation ->
            val stringRequest = object : StringRequest(
                Method.GET,
                url,
                { response ->
                    if (continuation.isActive) continuation.resume(response)
                },
                { error ->
                    Log.e("MatchRepository", "Private Fetch Error")
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            ) {
                override fun parseNetworkResponse(response: com.android.volley.NetworkResponse): com.android.volley.Response<String> {
                    val utf8String = String(response.data, Charsets.UTF_8)
                    return com.android.volley.Response.success(
                        utf8String,
                        com.android.volley.toolbox.HttpHeaderParser.parseCacheHeaders(response)
                    )
                }
            }

            stringRequest.retryPolicy = DefaultRetryPolicy(
                INITIAL_TIMEOUT_MS,
                MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            requestQueue.add(stringRequest)
            continuation.invokeOnCancellation { stringRequest.cancel() }
        }
    }

}
