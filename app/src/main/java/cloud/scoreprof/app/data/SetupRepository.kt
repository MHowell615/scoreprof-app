package cloud.scoreprof.app.data

import android.content.Context
import android.util.Log
import cloud.scoreprof.app.BuildConfig
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.Setup
import cloud.scoreprof.app.R
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
    suspend fun updateUserPrivacy(receiveEmail: Boolean)
    suspend fun requestJoinLeague(joinCode: String)
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
        val url = "https://api.scoreprof.cloud/send-invite"

        val authKey = BuildConfig.SPROF_AUTH_KEY
        val subject = context.getString(
            R.string.invite_subject,
            inviterName,
            leagueid
        )
        val message = context.getString(
            R.string.invite_message,
            inviterName,
            leagueid
        )

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

        // Set timeout to 15s to allow SES to process
        request.retryPolicy = DefaultRetryPolicy(
            15000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

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

    override suspend fun sendSupportEmail(userEmail: String, category: String, subject: String, description: String): Boolean {
        val url = "https://api.scoreprof.cloud/send-support"
        val authKey = BuildConfig.SPROF_AUTH_KEY

        val jsonBody = JSONObject().apply {
            put("userEmail", userEmail)
            put("category", category)
            put("subject", "Support Request: $category - $subject")
            put("description", description)
            put("authKey", BuildConfig.SPROF_AUTH_KEY)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                println("Support email request accepted by server")
            },
            { error ->
                val responseBody = error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                Log.e("SetupRepository", "Failed to send support email: $responseBody")
            }
        )

        // Set timeout to 15s to allow SES to process
        request.retryPolicy = DefaultRetryPolicy(
            15000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)
        return true
    }

    override suspend fun upsertSetup(setup: Setup) {
        withContext(Dispatchers.IO) {
            dao.upsertSetup(setup)
        }
    }

    override suspend fun requestJoinLeague(joinCode: String) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/request_join_league"
            val jsonBody = try {
                JSONObject().apply {
                    put("_league_code", joinCode)
                    put("user_token", token)
                }
            } catch (e: Exception) {
                println("TEST: [SetupRepository] requestJoinLeague JSON Construction Failed: ${e.message}")
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
                Log.d("SetupRepository", "requestJoinLeague request sent")
            }
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

    override suspend fun updateUserPrivacy(receiveEmail: Boolean) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_user_privacy"
            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_receive_email", receiveEmail)
                }
            } catch (e: Exception) {
                Log.e("SetupRepository", "updateUserPrivacy JSON failed: ${e.message}")
                throw e
            }
            val requestBody = jsonBody.toString()

            suspendCancellableCoroutine<String> { continuation ->
                val stringRequest = object : StringRequest(
                    Method.POST,
                    url,
                    { response -> if (continuation.isActive) continuation.resume(response) },
                    { error ->
                        if (error is AuthFailureError) {
                            tokenManager.deleteToken()
                            if (continuation.isActive) continuation.resumeWithException(IllegalStateException("SESSION_EXPIRED"))
                        }
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                ) {
                    override fun getBodyContentType(): String = "application/json; charset=utf-8"
                    override fun getBody(): ByteArray = requestBody.toByteArray(Charsets.UTF_8)
                }

                stringRequest.retryPolicy = DefaultRetryPolicy(INITIAL_TIMEOUT_MS, MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
                continuation.invokeOnCancellation { stringRequest.cancel() }
                requestQueue.add(stringRequest)
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