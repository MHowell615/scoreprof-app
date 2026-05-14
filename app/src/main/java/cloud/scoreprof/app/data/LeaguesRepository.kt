package cloud.scoreprof.app.data

import android.content.Context
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.League
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.LeagueTable
import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.domain.model.LeaguesResponse
import cloud.scoreprof.app.domain.model.UserLeague
import cloud.scoreprof.app.domain.model.UserLeagueUsers
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.json.JSONObject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

data class LeagueCreationResult(
    val newId: String,
    val leagueCode: String
)

interface LeaguesRepository {
    suspend fun getLeagues(userid: String): List<Leagues>
    fun getLeagueTable(leagueid: String, owneruserid: String, sortBy: String): Flow<List<LeagueTable>>
    suspend fun upsertLeagueInDb(league: League, leagues: Leagues)
    suspend fun insertLeagues(leagues: Leagues)
    suspend fun createNewLeague(
        leagueHeader: LeagueHeader,
        userLeague: UserLeague,
        userEmail: String
    ): LeagueCreationResult
    suspend fun inviteUserToLeague(leagueid: String, userEmail: String)
    suspend fun getEditLeague(leagueid: String, owneruserid: UUID): LeagueHeader?
    suspend fun saveEditLeague(
        originalLeagueId: String,
        leagueHeader: LeagueHeader,
        userLeague: UserLeague,
        userEmail: String
    )
    suspend fun getLeagueUserStatuses(leagueid: String, owneruserid: String): List<UserLeagueUsers>
    suspend fun updateLeagueInvitee(
        leagueid: String,
        email: String,
        status: String,
        invited: Boolean,
        selected: Boolean
    )
    suspend fun softDeleteLeague(leagueid: String, owneruserid: UUID)
    suspend fun softDeleteLeagueInvitee(leagueid: String, owneruserid: UUID, email: String)
    fun generateLeagueCode(length: Int = 7): String
    companion object

}

@Singleton
class LeaguesRepositoryImpl @Inject constructor(
    private val dao: ScoreProfDao,
    private val tokenManager: TokenManager,
    private val setupRepository: SetupRepository,
    @ApplicationContext private val context: Context
) : LeaguesRepository {
    private var json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var requestQueue = Volley.newRequestQueue(context)

    // Companion object for constants
    companion object {
        private const val MAX_RETRIES = 2 // 1 initial attempt + 2 retries = 3 total
        private const val INITIAL_TIMEOUT_MS = 5000 // 5 seconds
    }

    override suspend fun getLeagues(userid: String): List<Leagues> {
        return withContext(Dispatchers.IO) {
            // Step 1: Try to get leagues from the local database first.
            // As you correctly pointed out, no userid is needed for the local DAO call.
            var localLeagues = dao.getLeagues() // Corrected: No arguments passed

            // This check is important. If a userid is not provided, we should not make a network call.
            if (userid == null) {
                println("TEST: [Repo] Invalid userid (0). Returning empty dataset only.")
                return@withContext emptyList()
            }

            // Step 2: If the local database is empty, fetch from the server.
            //if (localLeagues.isEmpty()) {
                //println("TEST: [Repo] Local DB is empty. Fetching from server for user...")
                try {
                    println("(LeaguesRepository) Leagues list is empty, refreshing from server")
                    val token = tokenManager.getToken() ?: ""
                    val url = "https://www.scoreprof.cloud/rpc/getleagues?user_token=$token"
                    //println("TEST: [Repo] URL: $url")
                    val responseString = fetchPublicFromServer(url)
                    println("TEST: [Repo] Response: $responseString")

                    //println("TEST: LeaguesRepository 1")
                    if (!responseString.isNullOrBlank() && responseString != "null") {
                        //println("TEST: LeaguesRepository 2")
                        val serverResponse = json.decodeFromString<LeaguesResponse>(responseString)
//println("TEST: LeaguesRepository 3")
                        //println("TEST: [LeaguesRepository] serverResponse: $serverResponse")
                        val serverLeagues = serverResponse.leagues
                        //println("TEST: LeaguesRepository 4")
                        // Step 3: If the server returned data, save it to the database.
                        if (serverLeagues.isNotEmpty()) {
                            //println("TEST: LeaguesRepository 5")
                            println("TEST: [Repo] Fetched ${serverLeagues.size} leagues from server. Saving to DB.")
                            dao.upsertLeagues(serverLeagues)
                            //println("TEST: LeaguesRepository 6")

                            // After saving, update our local variable to return the new data.
                            localLeagues = serverLeagues
                        }
                    } else {
                        println("TEST: [Repo] Server returned no leagues for user.")
                    }
                } catch (e: Exception) {
                    println("TEST: [Repo] Error fetching from server: ${e.message}")
                    // In case of a network error, we'll return the (empty) list from the DB.
                }
            //}

            // Step 4: Return the data from the database.
            localLeagues
        }
    }

    override fun generateLeagueCode(length: Int): String {
        // We exclude 0, O, 1, and I to prevent user entry errors
        val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override fun getLeagueTable(leagueid: String, owneruserid: String, sortBy: String): Flow<List<LeagueTable>> {
        return flow {
            try {
                val token = tokenManager.getToken() ?: ""
                val url = "https://www.scoreprof.cloud/rpc/getleaguetable"

                val jsonBody = JSONObject().apply {
                    put("_leagueid", leagueid)
                    put("_owneruserid", owneruserid)
                    put("user_token", token)
                    put("_sort_by", sortBy)
                }

                val responseString = executePostRequest(url, jsonBody.toString())

                if (!responseString.isNullOrBlank() && responseString != "null") {
                    // 2. Decode the response as a List (since SQL now uses json_agg)
                    val serverTable = json.decodeFromString<List<LeagueTable>>(responseString)

                    // 3. Save ALL 8 people to the local database
                    dao.upsertLeagueTable(serverTable)

                    // 4. Emit the full list to the UI
                    emit(serverTable)
                }
            } catch (e: Exception) {
                println("TEST: [Repo] Error: ${e.message}")
                val cachedData = dao.getLeagueTable(leagueid, owneruserid).firstOrNull() ?: emptyList()
                emit(cachedData)
            }
        }
    }

    override suspend fun upsertLeagueInDb(league: League, leagues: Leagues) {
        withContext(Dispatchers.IO) {
            dao.upsertLeagueInDb(league)
            dao.upsertLeaguesInDb(leagues)
        }
    }

    override suspend fun inviteUserToLeague(leagueid: String, userEmail: String) {
        // TODO: Implement the Volley call for inviting a user.
        println("TODO: Implement inviteUserToLeague")
    }

    override suspend fun getLeagueUserStatuses(leagueid: String, owneruserid: String): List<UserLeagueUsers> {
        return withContext(Dispatchers.IO) {
            val localLeagueUserStatuses = dao.getLeagueUserStatuses(leagueid, owneruserid)
            //if (localLeagueUserStatuses.isEmpty()) {
            try {
                val token = tokenManager.getToken() ?: ""
                val url = "https://www.scoreprof.cloud/rpc/getleagueuserstats"

                val jsonBody = try {
                    JSONObject().apply {
                        put("l_id", leagueid)
                        put("o_id", owneruserid)
                        put("user_token", token)
                    }
                } catch (e: Exception) {
                    println("TEST: [LeaguesRepo] JSON Construction Failed: ${e.message}")
                    throw e
                }

                val requestBody = jsonBody.toString()

                val result = suspendCancellableCoroutine<String> { continuation ->
                    val stringRequest = object : StringRequest(
                        Method.POST, url,
                        { response -> continuation.resume(response) },
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
                        override fun getBody() = requestBody.toByteArray(Charsets.UTF_8)
                        override fun getHeaders(): MutableMap<String, String> {
                            val headers = HashMap<String, String>()
                            headers["Content-Type"] = "application/json; charset=utf-8"
                            return headers
                        }
                    }
                    stringRequest.retryPolicy = DefaultRetryPolicy(10000, 0, 1f)
                    requestQueue.add(stringRequest)
                    continuation.invokeOnCancellation { stringRequest.cancel() }
                }
                if (!result.isNullOrBlank() && result != "null") {
                    val serverStatuses = json.decodeFromString<List<UserLeagueUsers>>(result)
                    serverStatuses.forEach { dao.upsertUserLeagueUsersInDb(it) } // Assuming this DAO method exists
                    return@withContext serverStatuses
                }

                emptyList<UserLeagueUsers>() // Return empty if server result is null/blank
            } catch (e: Exception) {
                println("TEST: [LeaguesRepo] Error fetching league user statuses: ${e.message}")
                emptyList<UserLeagueUsers>()
            }
            /*} else {
                // Step 3: If local data was found, return it directly.
                println("TEST: [LeaguesRepo] Returning local league user statuses.")*/
            localLeagueUserStatuses
            //}
        }
    }

    override suspend fun createNewLeague(
        leagueHeader: LeagueHeader,userLeague: UserLeague, userEmail: String
    ): LeagueCreationResult {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/create_new_league"


            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_leagueid", leagueHeader.leagueid)
                    put("_competitionid", leagueHeader.competitionid)
                }
            } catch (e: Exception) {
                println("TEST: [CreateNewLeagueRepo] JSON Construction Failed: ${e.message}")
                throw e
            }

            val requestBody = jsonBody.toString()

            // 1. Network Call (Volley)
            val result = suspendCancellableCoroutine<String> { continuation ->
                val stringRequest = object : StringRequest(
                    Method.POST, url,
                    { response -> continuation.resume(response) },
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
                    override fun getBody() = requestBody.toByteArray(Charsets.UTF_8)
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Content-Type"] = "application/json; charset=utf-8"
                        return headers
                    }
                }
                stringRequest.retryPolicy = DefaultRetryPolicy(10000, 0, 1f)
                requestQueue.add(stringRequest)
                continuation.invokeOnCancellation { stringRequest.cancel() }
            }

            // now update the email invitees onto the server
            /*userLeague.userleagueusers.forEach {
                if (it.email != userEmail) {
                    updateLeagueInvitee(
                        leagueid = leagueHeader.leagueid,
                        email = it.email,
                        status = "Pending",
                        invited = true,
                        selected = false
                    )
                }
            }*/

            // the following id is not equivalent to the leagueid
            // id = INT sequence
            // leagueid = short set of chars as a unique code for the league
            //val _newid : String = result
            val jsonResponse = JSONObject(result)
            val leagueCode = jsonResponse.optString("league_code", "")
            val _newid = jsonResponse.optString("new_id", "")

            // DAO has Setup data for the user, which includes a subset called Leagues.
            // This subset contains id and leaguecode which will both need updating.

            // 2. Local DAO Sync
            dao.insertLeagueHeader(leagueHeader)

            // Insert the owner/users into the 'league' (stats) table
            leagueHeader.leagueusers.forEach { dao.upsertLeagueInDb(it) }

            setupRepository.refreshSetupFromServer(leagueHeader.owneruserid)

            LeagueCreationResult(
                newId = _newid,
                leagueCode = leagueCode
            )
        }
    }

    override suspend fun updateLeagueInvitee(
        leagueid: String,
        email: String,
        status: String,
        invited: Boolean,
        selected: Boolean
    ) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_league_invitee"

            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("l_id", leagueid)
                    put("_email", email)
                    put("_status", status)
                    put("is_invited", invited)
                    put("is_selected", selected)
                }
            } catch (e: Exception) {
                println("TEST: [CreateNewLeagueRepo] JSON Construction Failed: ${e.message}")
                throw e
            }

            val requestBody = jsonBody.toString()

            try {
                suspendCancellableCoroutine<String> { continuation ->
                    val stringRequest = object : StringRequest(
                        Method.POST, url,
                        { response ->
                            println("TEST: [UpdateInvitee] Server response: $response")
                            continuation.resume(response)
                        },
                        { error ->
                            println("TEST: [UpdateInvitee] Error: ${error.message}")
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
                        override fun getBody() = requestBody.toByteArray(Charsets.UTF_8)
                        override fun getHeaders(): MutableMap<String, String> {
                            val headers = HashMap<String, String>()
                            headers["Content-Type"] = "application/json; charset=utf-8"
                            return headers
                        }
                    }
                    stringRequest.retryPolicy = DefaultRetryPolicy(10000, 0, 1f)
                    requestQueue.add(stringRequest)
                    continuation.invokeOnCancellation { stringRequest.cancel() }
                }
            } catch (e: Exception) {
                Log.e("LeaguesRepo", "Failed to update invitee $email: ${e.message}")
            }
        }
    }

    override suspend fun getEditLeague(leagueid: String, owneruserid: UUID): LeagueHeader? {
        return withContext(Dispatchers.IO) {
            // 1. Check Local Cache first
            val local = dao.getLeagueHeader(leagueid, owneruserid)
            if (local != null) return@withContext local

            // 2. Fetch from Remote using a secure POST request
            return@withContext try {
                val url = "https://www.scoreprof.cloud/rpc/getleagueuserstats"
                //val owneruserid = leagueHeader.owneruserid
                //    ?: throw IllegalArgumentException("Owner UserID cannot be null when creating a league")

                val response = suspendCancellableCoroutine<String> { continuation ->
                    val stringRequest = object : StringRequest(
                        Method.POST, // Using POST to hide parameters
                        url,
                        { response -> continuation.resume(response) },
                        { error -> continuation.resumeWithException(error) }
                    ) {
                        override fun getHeaders(): MutableMap<String, String> {
                            return HashMap()
                        }

                        override fun getBody(): ByteArray {
                            // Pass IDs in the JSON body
                            val token = tokenManager.getToken() ?: ""
                            val map = mapOf(
                                "l_id" to leagueid,
                                "o_id" to owneruserid.toString(),
                                "user_token" to token // Pass the token here!
                            )
                            return json.encodeToString(map).toByteArray(Charsets.UTF_8)
                        }

                        override fun getBodyContentType(): String {
                            return "application/json; charset=utf-8"
                        }
                    }
                    // be nice to the server as HMAC and security checks are computationally expensive
                    stringRequest.retryPolicy = DefaultRetryPolicy(
                        10000,
                        0,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )

                    requestQueue.add(stringRequest)
                    continuation.invokeOnCancellation { stringRequest.cancel() }
                }

                // 3. Parse and Map the result
                val userLeagueUsers = json.decodeFromString<List<UserLeagueUsers>>(response)
                val firstUser = userLeagueUsers.firstOrNull() ?: throw Exception("Empty list")

                val leagueUsers = userLeagueUsers.map { status ->
                    League(
                        leagueid = leagueid,
                        owneruserid = owneruserid,
                        userid =
                            status.userid, // This is okay to see; it's public league info
                        username = status.email.substringBefore("@")
                    )
                }

                val remoteHeader = LeagueHeader(
                    leagueid = leagueid,
                    owneruserid = owneruserid,
                    competitionid = firstUser.competitionid,
                    leagueusers = leagueUsers
                )

                // Cache it locally
                dao.insertLeagueHeader(remoteHeader)
                remoteHeader
            } catch (e: Exception) {
                println("Error in getEditLeague: ${e.message}")
                dao.getLeagueHeader(leagueid, owneruserid)
            }
        }
    }

    override suspend fun saveEditLeague(
        originalLeagueId: String,
        leagueHeader: LeagueHeader,
        userLeague: UserLeague,
        userEmail: String
    ) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_league"

            val nameChanged = originalLeagueId != leagueHeader.leagueid

            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("original_leagueid", originalLeagueId)
                    put("new_leagueid", leagueHeader.leagueid)
                    put("_competitionid", leagueHeader.competitionid)
                }
            } catch (e: Exception) {
                println("TEST: [CreateNewLeagueRepo] JSON Construction Failed: ${e.message}")
                throw e
            }

            val requestBody = jsonBody.toString()

            // 1. Network Call (Volley)
            suspendCancellableCoroutine<String> { continuation ->
                val stringRequest = object : StringRequest(
                    Method.POST, url,
                    { response -> continuation.resume(response) },
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
                    override fun getBody() = requestBody.toByteArray(Charsets.UTF_8)
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Content-Type"] = "application/json; charset=utf-8"
                        return headers
                    }
                }
                stringRequest.retryPolicy = DefaultRetryPolicy(10000, 0, 1f)
                requestQueue.add(stringRequest)
                continuation.invokeOnCancellation { stringRequest.cancel() }
            }

            // now update the email invitees onto the server
            val localUsers = dao.getLeagueUserStatuses(
                originalLeagueId,
                leagueHeader.owneruserid.toString()
            )

            val localEmailMap = localUsers.associateBy { it.email.lowercase().trim() }

            // B. Handle NEW Invitees (In UI list but not in DB)
            // Or if name changed, everyone in the UI list needs a record for the NEW ID
            userLeague.userleagueusers.forEach { uiUser ->
                val email = uiUser.email.lowercase().trim()
                if (email == userEmail.lowercase().trim()) return@forEach

                val localRecord = localEmailMap[email]

                // Logic: Sync if brand new OR if they were previously 'Deleted' locally
                val isReinvite = localRecord?.state?.lowercase() == "deleted"
                val isNew = localRecord == null

                if (isNew || isReinvite) {
                    updateLeagueInvitee(
                        leagueid = leagueHeader.leagueid,
                        email = uiUser.email,
                        status = "Pending",
                        invited = true,
                        selected = true
                    )
                    // Update local DB immediately so they appear as 'Active' again
                    dao.upsertUserLeagueUsersInDb(uiUser.copy(state = "Active", invitestatus = "Pending"))
                }
            }

            localUsers.forEach { local ->
                val email = local.email.lowercase().trim()
                if (email == userEmail.lowercase().trim()) return@forEach

                val stillInUi = userLeague.userleagueusers.any { it.email.lowercase().trim() == email }
                if (!stillInUi && local.state?.lowercase() != "deleted") {
                    softDeleteLeagueInvitee(leagueHeader.leagueid, leagueHeader.owneruserid, local.email)
                }
            }

            dao.insertLeagueHeader(leagueHeader)
            val isSelectedInUI = userLeague.userleagueusers.find { it.email == userEmail }?.selected ?: true
            dao.updateUserLeagueSelection(leagueHeader.leagueid, leagueHeader.owneruserid, isSelectedInUI)

            leagueHeader.leagueusers.forEach { user ->
                val updatedUser = user.copy(leagueid = leagueHeader.leagueid)
                dao.upsertLeagueInDb(updatedUser)
            }

            // 3. Local DB Cleanup
            if (nameChanged) {
                dao.deleteLeague(originalLeagueId, leagueHeader.owneruserid)
            }

            setupRepository.refreshSetupFromServer(leagueHeader.owneruserid)
        }
    }

    override suspend fun softDeleteLeagueInvitee(
        leagueid: String,
        owneruserid: UUID,
        email: String
    ) {
        updateLeagueInvitee(leagueid, email, "Deleted", false, false)
        return withContext(Dispatchers.IO) {
            val localStatuses = dao.getLeagueUserStatuses(leagueid, owneruserid.toString())
            val record = localStatuses.find { it.email.lowercase().trim() == email.lowercase().trim() }
            record?.let {
                // Mark as Deleted locally so the UI filters it out immediately
                dao.upsertUserLeagueUsersInDb(it.copy(state = "Deleted"))
                println("TEST: [Repo] Soft deleted $email locally.")
            }

            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/soft_delete_league_invitee"

            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_leagueid", leagueid)
                    put("_owneruserid", owneruserid)
                    put("_email", email)
                }
            } catch (e: Exception) {
                println("TEST: [LeaguesRepository] softDeleteLeagueInvitee JSON Construction Failed: ${e.message}")
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
                Log.d("LeaguesRepository", "softDeleteLeagueInvitee request sent")
            }
        }
    }


    override suspend fun softDeleteLeague(leagueid: String, owneruserid: UUID) {
        println("TEST: [LeaguesRepo] Entering softDeleteLeague for ${leagueid} and ${owneruserid}")
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/soft_delete_league"

            val jsonBody = try {
                JSONObject().apply {
                    put("user_token", token)
                    put("_leagueid", leagueid)
                    put("_owneruserid", owneruserid)
                }
            } catch (e: Exception) {
                println("TEST: [LeaguesRepository] softDeleteLeague JSON Construction Failed: ${e.message}")
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
                Log.d("LeaguesRepository", "softDeleteLeague request sent")
            }
        }
    }

    override suspend fun insertLeagues(leagues: Leagues) {
        dao.insertLeagues(leagues)
    }

    private suspend fun executePostRequest(url: String, requestBody: String): String {
        return suspendCancellableCoroutine { continuation ->
            val stringRequest = object : StringRequest(
                Method.POST, url,
                { response -> if (continuation.isActive) continuation.resume(response) },
                { error -> if (continuation.isActive) continuation.resumeWithException(error) }
            ) {
                override fun getBody() = requestBody.toByteArray(Charsets.UTF_8)
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf("Content-Type" to "application/json; charset=utf-8")
                }
                override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                    val utf8String = String(response.data, Charsets.UTF_8)
                    return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response))
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(INITIAL_TIMEOUT_MS, MAX_RETRIES, 1f)
            requestQueue.add(stringRequest)
            continuation.invokeOnCancellation { stringRequest.cancel() }
        }
    }

    private suspend fun fetchPublicFromServer(url: String): String {
        return suspendCancellableCoroutine { continuation ->
            val stringRequest = object : StringRequest(
                Method.GET, url,
                { response -> if (continuation.isActive) continuation.resume(response) },
                { error -> if (continuation.isActive) continuation.resumeWithException(error) }
            ) {
                override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                    val utf8String = String(response.data, Charsets.UTF_8)
                    return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response))
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(INITIAL_TIMEOUT_MS, MAX_RETRIES, 1f)
            requestQueue.add(stringRequest)
            continuation.invokeOnCancellation { stringRequest.cancel() }
        }

    }

    private suspend fun fetchFromServer(url: String): String {
        return suspendCancellableCoroutine { continuation ->
            val stringRequest = object: StringRequest(
                Request.Method.GET,
                url,
                { response ->
                    // Success: Resume the coroutine with the JSON string response
                    if (continuation.isActive) {
                        continuation.resume(response)
                    }
                },
                { error ->
                    // Error: Resume the coroutine with an exception after all retries fail
                    println("Unable to contact server after multiple attempts.")
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    tokenManager.getToken()?.let { token ->
                        headers["Authorization"] = "Bearer $token"
                    }
                    return headers
                }
            }

            // *** ADDED: Set the retry policy for the request ***
            stringRequest.retryPolicy = DefaultRetryPolicy(
                INITIAL_TIMEOUT_MS,
                MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )

            // Add the request to the queue to execute it.
            requestQueue.add(stringRequest)

            // When the coroutine is cancelled, cancel the network request.
            continuation.invokeOnCancellation {
                stringRequest.cancel()
            }
        }
    }
}


