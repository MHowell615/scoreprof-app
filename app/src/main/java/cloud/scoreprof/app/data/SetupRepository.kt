package cloud.scoreprof.app.data

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
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
    suspend fun updateUserCompetition(competitionid: String, isSelected: Boolean)
    suspend fun updateUserProfile(name: String, email: String, language: String)
    suspend fun activateAccount(email: String, preferredLanguage: String)
    suspend fun sendInviteEmail(inviteeEmail: String, inviterName: String, leagueid: String, owneruserid: String)
    suspend fun logout()
    suspend fun updateUserLeague(leagueid: String, owneruserid: UUID, isSelected: Boolean)
    suspend fun sendSupportEmail(userEmail: String, category: String, subject: String, description: String): Boolean
    suspend fun updateUserPrivacy(receiveEmail: Boolean)
    suspend fun requestJoinLeague(joinCode: String)
    suspend fun updateAdsRemoved(isRemoved: Boolean)
    suspend fun reportCrash(errorMessage: String, stackTrace: String, appVersion: String)
    suspend fun updateAllUserCompetitions(isSelected: Boolean)
    suspend fun logError(errorMessage: String, stackTrace: String, appVersion: String)
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

    companion object {
        private const val MAX_RETRIES = 2
        private const val INITIAL_TIMEOUT_MS = 5000
    }

    override suspend fun getSetup(userid: UUID): Flow<Setup?> {
        return dao.getSetup()
    }

    override suspend fun refreshSetupFromServer(userid: UUID) {
        try {
            val token = tokenManager.getToken() ?: ""
            if (token.isBlank()) throw IllegalStateException("SESSION_EXPIRED")

            val appLocales = AppCompatDelegate.getApplicationLocales()
            val language = if (!appLocales.isEmpty) {
                appLocales[0]?.language?.take(2)?.lowercase() ?: "en"
            } else {
                java.util.Locale.getDefault().language.take(2).lowercase()
            }

            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val currentVersion = packageInfo.longVersionCode.toInt()

            var url = "https://www.scoreprof.cloud/rpc/getsetupdata?user_token=$token"
            if (currentVersion > 7) {
                url = "https://www.scoreprof.cloud/rpc/getsetupdata?user_token=$token&lang=$language"
            }

            println("TEST: sending request for url: $url")
            val responseString = fetchPublicFromServer(url)

            if (!responseString.isNullOrBlank() && responseString != "null") {
                val setupData = json.decodeFromString<Setup>(responseString)
                withContext(Dispatchers.IO) {
                    dao.upsertSetup(setupData)
                    dao.upsertSetupCompetitions(setupData.competitions)
                    dao.upsertSetupLeagues(setupData.leagues)
                }
            }
        } catch (e: Exception) {
            Log.e("SetupRepository", "Failed to fetch setup data.", e)
            throw e
        }
    }

    override suspend fun sendInviteEmail(inviteeEmail: String, inviterName: String, leagueid: String, owneruserid: String) {
        val url = "https://api.scoreprof.cloud/send-invite"
        val authKey = BuildConfig.SPROF_AUTH_KEY
        val subject = context.getString(R.string.invite_subject, inviterName, leagueid)
        val message = context.getString(R.string.invite_message, inviterName, leagueid)

        val jsonBody = JSONObject().apply {
            put("friendEmail", inviteeEmail)
            put("inviterName", inviterName)
            put("subject", subject)
            put("message", message)
            put("authKey", authKey)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody, null, null)
        request.retryPolicy = DefaultRetryPolicy(15000, 2, 1f)
        requestQueue.add(request)
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        tokenManager.deleteToken()
    }

    override suspend fun updateUserLeague(leagueid: String, owneruserid: UUID, isSelected: Boolean) {
        withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_user_league"
            val jsonBody = JSONObject().apply {
                put("user_token", token)
                put("_leagueid", leagueid)
                put("_owneruserid", owneruserid)
                put("_isselected", isSelected)
            }
            performPostRequest(url, jsonBody.toString())
            dao.updateUserLeagueSelection(leagueid, owneruserid, isSelected)
        }
    }

    override suspend fun sendSupportEmail(userEmail: String, category: String, subject: String, description: String): Boolean {

        val url = "https://api.scoreprof.cloud/send-support"
        val jsonBody = JSONObject().apply {
            put("userEmail", userEmail)
            put("category", category)
            put("subject", "Support Request: $category - $subject")
            put("description", description)
            put("authKey", BuildConfig.SPROF_AUTH_KEY)
        }

        return try {
            // Reuse your performPostRequest helper
            performPostRequest(url, jsonBody.toString())
            true
        } catch (e: Exception) {
            Log.e("SetupRepository", "Support email failed to send", e)
            false
        }
    }

    override suspend fun upsertSetup(setup: Setup) {
        withContext(Dispatchers.IO) { dao.upsertSetup(setup) }
    }

    override suspend fun reportCrash(errorMessage: String, stackTrace: String, appVersion: String) {
        val userId = tokenManager.getUserId()
        val token = tokenManager.getToken() ?: ""
        val url = "https://api.scoreprof.cloud/logs/error"
        val jsonBody = try {
            JSONObject().apply {
                put("userid", userId) // Can be null
                put("error_message", errorMessage)
                put("stack_trace", stackTrace)
                put("app_version", appVersion)
                put("device_model", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            }
        } catch (e: Exception) {
            println("[SetupRepository] reportCrash JSON Construction Failed: ${e.message}")
            throw e
        }

        performPostRequest(url, jsonBody.toString())
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

    override suspend fun logError(errorMessage: String, stackTrace: String, appVersion: String) {
        // This hits your Node.js endpoint: app.post('/logs/error', ...)
        val url = "https://api.scoreprof.cloud/logs/error"
        val userId = tokenManager.getUserId()?.toString() ?: "Unknown"
        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

        val jsonBody = JSONObject().apply {
            put("userid", userId)
            put("error_message", errorMessage)
            put("stack_trace", stackTrace)
            put("app_version", appVersion)
            put("device_model", deviceModel)
        }

        try {
            // Reuse your existing performPostRequest helper
            performPostRequest(url, jsonBody.toString())
        } catch (e: Exception) {
            // If logging fails, we just print to console to avoid infinite loops
            Log.e("SetupRepository", "Failed to send remote log", e)
        }
    }

    override suspend fun activateAccount(email: String, preferredLanguage: String) {
        val token = tokenManager.getToken() ?: ""
        val url = "https://www.scoreprof.cloud/rpc/activate_pending_user"
        val jsonBody = JSONObject().apply {
            put("user_token", token)
            put("_email", email)
            put("_language", preferredLanguage)
        }
        performPostRequest(url, jsonBody.toString())
    }

    override suspend fun updateUserProfile(name: String, email: String, language: String) {
        val token = tokenManager.getToken() ?: ""
        val url = "https://www.scoreprof.cloud/rpc/update_user_profile"
        val jsonBody = JSONObject().apply {
            put("user_token", token)
            put("_username", name)
            put("_email", email)
            put("_language", language)
        }
        performPostRequest(url, jsonBody.toString())
    }

    override suspend fun updateUserCompetition(competitionid: String, isSelected: Boolean) {
        val token = tokenManager.getToken() ?: ""
        val url = "https://www.scoreprof.cloud/rpc/update_user_competition"
        val jsonBody = JSONObject().apply {
            put("user_token", token)
            put("_competitionid", competitionid)
            put("_isselected", isSelected)
        }
        performPostRequest(url, jsonBody.toString())
    }

    override suspend fun updateAllUserCompetitions(isSelected: Boolean) {
        val token = tokenManager.getToken() ?: ""
        val url = "https://www.scoreprof.cloud/rpc/update_all_user_competitions"
        val jsonBody = JSONObject().apply {
            put("user_token", token)
            put("isselected", isSelected)
        }
        performPostRequest(url, jsonBody.toString())
    }

    override suspend fun updateUserPrivacy(receiveEmail: Boolean) {
        val token = tokenManager.getToken() ?: ""
        val url = "https://www.scoreprof.cloud/rpc/update_user_privacy"
        val jsonBody = JSONObject().apply {
            put("user_token", token)
            put("_receive_email", receiveEmail)
        }
        performPostRequest(url, jsonBody.toString())
    }

    override suspend fun updateAdsRemoved(isRemoved: Boolean) {
        val token = tokenManager.getToken() ?: ""
        val url = "https://www.scoreprof.cloud/rpc/update_ads_removed"
        val jsonBody = JSONObject().apply {
            put("user_token", token)
            put("_is_ads_removed", isRemoved)
        }
        performPostRequest(url, jsonBody.toString())
    }

    private suspend fun performPostRequest(url: String, requestBody: String) {
        suspendCancellableCoroutine<String> { continuation ->
            val stringRequest = object : StringRequest(Method.POST, url,
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
            stringRequest.retryPolicy = DefaultRetryPolicy(INITIAL_TIMEOUT_MS, MAX_RETRIES, 1f)
            continuation.invokeOnCancellation { stringRequest.cancel() }
            requestQueue.add(stringRequest)
        }
    }

    private suspend fun fetchPublicFromServer(url: String): String {
        return suspendCancellableCoroutine { continuation ->
            val stringRequest = StringRequest(Request.Method.GET, url,
                { response -> if (continuation.isActive) continuation.resume(response) },
                { error ->
                    if (error is AuthFailureError) {
                        tokenManager.deleteToken()
                        if (continuation.isActive) continuation.resumeWithException(IllegalStateException("SESSION_EXPIRED"))
                    }
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            )
            stringRequest.retryPolicy = DefaultRetryPolicy(INITIAL_TIMEOUT_MS, MAX_RETRIES, 1f)
            requestQueue.add(stringRequest)
            continuation.invokeOnCancellation { stringRequest.cancel() }
        }
    }
}
