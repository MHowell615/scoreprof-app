package cloud.scoreprof.app.data

import android.content.Context
import android.util.Log
import cloud.scoreprof.app.BuildConfig
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.Setup
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.json.JSONObject

interface SetupRepository {
    suspend fun getSetup(userid: UUID) : Flow<Setup?>
    suspend fun refreshSetupFromServer(userid: UUID)
    suspend fun upsertSetup(setup: Setup)
    //suspend fun updateAllSetupOnServer(payload: SetupPayload)
    suspend fun updateUserCompetition(competitionid: String, isSelected: Boolean)
    suspend fun updateUserProfile(name: String, email: String, language: String)
    suspend fun activateAccount(email: String, preferredLanguage: String)
    suspend fun sendInviteEmail(inviteeEmail: String, inviterName: String, leagueid: String, owneruserid: String)
    suspend fun logout()
    suspend fun updateUserLeague(leagueid: String, owneruserid: UUID, isSelected: Boolean)
    suspend fun sendSupportEmail(userEmail: String, category: String, subject: String, description: String): Boolean
}

@Singleton
class SetupRepositoryImpl @Inject constructor(
    private val database: ScoreProfDatabase,
    private val dao: ScoreProfDao,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : SetupRepository {

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

    override suspend fun getSetup(userid: UUID): Flow<Setup?> {
        return dao.getSetup()
    }

    override suspend fun refreshSetupFromServer(userid: UUID) {
        // Here you could also add logic to fetch from the server first if needed
        try {
            val token = tokenManager.getToken() ?: ""
            if (token.isBlank()) throw IllegalStateException("SESSION_EXPIRED")

            val url = "https://www.scoreprof.cloud/rpc/getsetupdata?user_token=$token"
//println("SetupRepo: refreshSetupFromServer url: $url")
            println("SetupRepo: Fetching with custom token validation")
            val responseString = fetchPublicFromServer(url)
//println("SetupRepo: refreshSetupFromServer response: $responseString")

            if (!responseString.isNullOrBlank() && responseString != "null") {
                val setupData = json.decodeFromString<Setup>(responseString)
                // Log to verify the new competition IS actually in the JSON
                Log.d("SetupRepo", "Competitions from server: ${setupData.competitions.size}")
                //println("SetupRepo setupData: $setupData")
                // Perform the update in a transaction-like way
                withContext(Dispatchers.IO) {
                    // 1. Update the parent Setup record
                    dao.upsertSetup(setupData)

                    // 2. FORCE update the specific child tables
                    // This ensures new competitions are inserted into the 'Competitions' table
                    dao.upsertSetupCompetitions(setupData.competitions)

                    // 3. Update the leagues table (handles the 'Deleted' state sync too)
                    dao.upsertSetupLeagues(setupData.leagues)
                }
            } else {
               Log.w("SetupRepository", "Received null or empty response from server.")
            }
        } catch (e: Exception) {
            Log.e("SetupRepository", "Failed to fetch and save setup data.", e)
            // Re-throw the exception so the ViewModel knows the call failed.
            throw e
        }
    }

    override suspend fun sendInviteEmail(inviteeEmail: String, inviterName: String, leagueid: String, owneruserid: String) {
        // This will be the IP Hetzner gives you
        val url = "https://api.scoreprof.cloud:3000/send-invite"

        // Get the inviter's name from your existing 'setup' state
        //val inviterName = _setup.value?.name ?: "A friend"
        val authKey = BuildConfig.SPROF_AUTH_KEY
        val subject = "${inviterName} invited you to join league '$leagueid'"
        val message = "Hi!\n\n${inviterName} has invited you to join the league '$leagueid' on the ScoreProf app.\n\nTo acccept this invitation and start playing, open the ScoreProf app select the league via Setup > Leagues.\n\nIf you don't have the app yet, you can download it here: [YOUR_GOOGLE_PLAY_STORE_LINK]\n\nHappy playing!\nThe ScoreProf Team"

        val jsonBody = JSONObject().apply {
            put("friendEmail", inviteeEmail) // Matches req.body.friendEmail in server.js
            put("inviterName", inviterName)   // Matches req.body.senderName in server.js
            put("subject", subject)
            put("message", message)
            put("authKey", authKey) // Security handshake
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                println("Invite sent successfully to $inviteeEmail")
            },
            { error ->
                println("Failed to send invite: ${error.message}")
            }
        )

        // Ensure you have a RequestQueue initialized in your ViewModel
        // If you don't have one, you can use Volley.newRequestQueue(getApplication())
        requestQueue.add(request)
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            // This clears all 13 tables we defined in the Database class
            database.clearAllTables()
        }
        // This clears the encrypted shared preferences
        tokenManager.deleteToken()
    }

    override suspend fun updateUserLeague(
        leagueid: String,
        owneruserid: UUID,
        isSelected: Boolean
    ) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_user_league"
            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_leagueid", leagueid)
                    put("_owneruserid", owneruserid)
                    put("_isselected", isSelected)
                }
            } catch (e: Exception) {
                println("TEST: [SetupRepository] updateUserLeague JSON Construction Failed: ${e.message}")
                throw e
            }
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
                        if (error is AuthFailureError) {
                            tokenManager.deleteToken()
                            if (continuation.isActive) continuation.resumeWithException(
                                IllegalStateException("SESSION_EXPIRED")
                            )
                            //return@StringRequest
                        }

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
                Log.d("SetupRepository", "updateUserLeague request sent")
            }
            dao.updateUserLeagueSelection(leagueid, owneruserid, isSelected)
        }
    }


    /*override suspend fun updateAllSetupOnServer(payload: SetupPayload) {
        return suspendCancellableCoroutine { continuation ->
            // 1. Use the base URL without query parameters.
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_user_setup_all"

            println("Sending consolidated setup update to server: $payload")

            val jsonBody = JSONObject().apply {
                put("user_token", token)
                put("_name", payload.name)
                put("_membersince", payload.memberSince)
                put("_preferred_language", payload.preferredLanguage)
                put("_competitions", JSONArray(json.encodeToString(payload.competitions)))
                put("_userleagues", JSONArray(json.encodeToString(payload.leagues)))
                put("_leagues", JSONArray(json.encodeToString(payload.leagues)))
            }

            // 2. Serialize the SetupPayload object into a JSON string.
            val requestBody = jsonBody.toString()

            val stringRequest =
                object : Request<String>(Method.POST, url, { error ->
                    if (error is AuthFailureError) {
                        tokenManager.deleteToken()
                        if (continuation.isActive) continuation.resumeWithException(IllegalStateException("SESSION_EXPIRED"))
                        //return@Request
                    }

                    val responseBody = error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                    println("Server returned error: ${error.message}, Body: $responseBody")
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }) {
                    override fun getHeaders(): MutableMap<String, String> = HashMap()

                    override fun deliverResponse(response: String?) {
                        println("Update successful: $response")
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }

                    override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
                        // PostgREST returns 204 No Content for successful void functions.
                        // Volley treats 204 as an error by default, so we handle it as a success.
                        return try {
                            if (response?.statusCode == 204 || response?.statusCode == 200) {
                                Response.success(
                                    "Success",
                                    HttpHeaderParser.parseCacheHeaders(
                                        response
                                    )
                                )
                            } else {
                                val responseData =
                                    response?.data?.let { String(it, Charsets.UTF_8) }
                                        ?: "No response body"
                                Response.error(
                                    ParseError(Exception("Server error with status code: ${response?.statusCode}, body: $responseData"))
                                )
                            }
                        } catch (e: Exception) {
                            Response.error(ParseError(e))
                        }
                    }

                    override fun getBodyContentType(): String = "application/json; charset=utf-8"

                    override fun getBody(): ByteArray = requestBody.toByteArray(Charsets.UTF_8)
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
    }*/

    override suspend fun sendSupportEmail(userEmail: String, category: String, subject: String, description: String): Boolean {
        val url = "https://api.scoreprof.cloud:3000/send-support"
        val authKey = BuildConfig.SPROF_AUTH_KEY

        val jsonBody = JSONObject().apply {
            put("userEmail", userEmail)
            put("category", category)
            put("subject", subject)
            put("description", description)
            put("authKey", BuildConfig.SPROF_AUTH_KEY)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                println("Message sent successfully to support from $userEmail")
            },
            { error ->
                println("Failed to send message to support: ${error.message}")
            }
        )

        requestQueue.add(request)

        return true
    }

    override suspend fun upsertSetup(setup: Setup) {
        withContext(Dispatchers.IO) {
            dao.upsertSetup(setup)
        }
    }

    override suspend fun activateAccount(email: String, preferredLanguage: String) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/activate_pending_user"
            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_email", email)
                    put("_language", preferredLanguage)
                }
            } catch (e: Exception) {
                println("TEST: [SetupRepository] activateAccount JSON Construction Failed: ${e.message}")
                throw e
            }
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
                        if (error is AuthFailureError) {
                            tokenManager.deleteToken()
                            if (continuation.isActive) continuation.resumeWithException(
                                IllegalStateException("SESSION_EXPIRED")
                            )
                            //return@StringRequest
                        }

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
                Log.d("SetupRepository", "activateAccount request sent")
            }
        }
    }

    override suspend fun updateUserProfile(name: String, email: String, language: String) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_user_profile"
            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_username", name)
                    put("_email", email)
                    put("_language", language)
                }
            } catch (e: Exception) {
                println("TEST: [SetupRepository] updateUserProfile JSON Construction Failed: ${e.message}")
                throw e
            }
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
                        if (error is AuthFailureError) {
                            tokenManager.deleteToken()
                            if (continuation.isActive) continuation.resumeWithException(
                                IllegalStateException("SESSION_EXPIRED")
                            )
                            //return@StringRequest
                        }

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
                Log.d("SetupRepository", "updateUserProfile request sent")
            }
        }
    }

    override suspend fun updateUserCompetition(competitionid: String, isSelected: Boolean) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_user_competition"
            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_competitionid", competitionid)
                    put("_isselected", isSelected)
                }
            } catch (e: Exception) {
                println("TEST: [SetupRepository] updateUserCompetition JSON Construction Failed: ${e.message}")
                throw e
            }
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
                        if (error is AuthFailureError) {
                            tokenManager.deleteToken()
                            if (continuation.isActive) continuation.resumeWithException(IllegalStateException("SESSION_EXPIRED"))
                            //return@StringRequest
                        }

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
                Log.d("SetupRepository", "updateUserCompetition request sent")
            }
        }
    }

    private suspend fun fetchPublicFromServer(url: String): String {
        return suspendCancellableCoroutine { continuation ->
            val stringRequest = StringRequest(
                Request.Method.GET,
                url,
                { response ->
                    if (continuation.isActive) continuation.resume(response)
                },
                { error ->
                    if (error is AuthFailureError) {
                        // 1. Log the event clearly
                        Log.e("SetupRepository", "401 Unauthorized: Session invalidated by server change.")

                        // 2. Clear the invalid token from local storage
                        tokenManager.deleteToken()

                        // 3. Signal the failure to the calling function
                        if (continuation.isActive) {
                            continuation.resumeWithException(IllegalStateException("SESSION_EXPIRED"))
                        }
                        return@StringRequest // Exit the listener
                    }
                    val responseBody =
                        error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                    println("Setup Public Fetch Error: $responseBody")
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            )
            // No getHeaders() override here - this bypasses the PostgREST JWT check

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