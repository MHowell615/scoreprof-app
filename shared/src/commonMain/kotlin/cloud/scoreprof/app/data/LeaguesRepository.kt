package cloud.scoreprof.app.data

import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.League
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.LeagueTable
import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.domain.model.LeaguesResponse
import cloud.scoreprof.app.domain.model.UserLeague
import cloud.scoreprof.app.domain.model.UserLeagueUsers
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class LeagueCreationResult(
    val newId: String,
    val leagueCode: String
)

interface LeaguesRepository {
    suspend fun getLeagues(userid: String): List<Leagues>
    fun getLeagueTable(
        leagueid: String,
        owneruserid: String,
        sortBy: String,
        jumpToTop: Boolean = false
    ): Flow<List<LeagueTable>>
    suspend fun upsertLeagueInDb(league: League, leagues: Leagues)
    suspend fun insertLeagues(leagues: Leagues)
    suspend fun createNewLeague(
        leagueHeader: LeagueHeader,
        userLeague: UserLeague,
        userEmail: String
    ): LeagueCreationResult
    suspend fun inviteUserToLeague(leagueid: String, userEmail: String)
    suspend fun getEditLeague(leagueid: String, owneruserid: String): LeagueHeader?
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
    suspend fun softDeleteLeague(leagueid: String, owneruserid: String)
    suspend fun softDeleteLeagueInvitee(leagueid: String, owneruserid: String, email: String)
    fun generateLeagueCode(length: Int = 7): String
}

class LeaguesRepositoryImpl(
    private val dao: ScoreProfDao,
    private val tokenManager: TokenManager,
    private val setupRepository: SetupRepository,
    private val httpClient: HttpClient,
    private val platform: cloud.scoreprof.app.Platform
) : LeaguesRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getLeagues(userid: String): List<Leagues> {
        return withContext(Dispatchers.IO) {
            var localLeagues = dao.getLeagues()
            try {
                val token = tokenManager.getToken() ?: ""
                val language = platform.language
                val url = "https://www.scoreprof.cloud/rpc/getleagues?user_token=$token"
                val responseString: String = httpClient.get(url).body()

                if (responseString.isNotBlank() && responseString != "null") {
                    val serverResponse = json.decodeFromString<LeaguesResponse>(responseString)
                    val serverLeagues = serverResponse.leagues
                    if (serverLeagues.isNotEmpty()) {
                        dao.upsertLeagues(serverLeagues)
                        localLeagues = serverLeagues
                    }
                }
            } catch (e: Exception) {
                // Return local data on error
            }
            localLeagues
        }
    }

    override fun generateLeagueCode(length: Int): String {
        val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override fun getLeagueTable(
        leagueid: String,
        owneruserid: String,
        sortBy: String,
        jumpToTop: Boolean
    ): Flow<List<LeagueTable>> {
        return flow {
            try {
                val token = tokenManager.getToken() ?: ""
                val url = "https://www.scoreprof.cloud/rpc/getleaguetable_v2"
                val body = buildJsonObject {
                    put("_leagueid", leagueid)
                    put("_owneruserid", owneruserid)
                    put("user_token", token)
                    put("_sort_by", sortBy)
                    put("_jump_to_top", jumpToTop)
                }

                val responseString: String = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.body()

                if (responseString.isNotBlank() && responseString != "null") {
                    val serverTable = json.decodeFromString<List<LeagueTable>>(responseString)
                    dao.upsertLeagueTable(serverTable)
                    emit(serverTable)
                }
            } catch (e: Exception) {
                // Emit cached data on error
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
        // TODO
    }

    override suspend fun getLeagueUserStatuses(leagueid: String, owneruserid: String): List<UserLeagueUsers> {
        return withContext(Dispatchers.IO) {
            val localLeagueUserStatuses = dao.getLeagueUserStatuses(leagueid, owneruserid)
            try {
                val token = tokenManager.getToken() ?: ""
                val url = "https://www.scoreprof.cloud/rpc/getleagueuserstats"
                val body = buildJsonObject {
                    put("l_id", leagueid)
                    put("o_id", owneruserid)
                    put("user_token", token)
                }

                val response: String = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.body()

                if (response.isNotBlank() && response != "null") {
                    val serverStatuses = json.decodeFromString<List<UserLeagueUsers>>(response)
                    serverStatuses.forEach { dao.upsertUserLeagueUsersInDb(it) }
                    return@withContext serverStatuses
                }
            } catch (e: Exception) {
                // Fallback to local
            }
            localLeagueUserStatuses
        }
    }

    override suspend fun createNewLeague(
        leagueHeader: LeagueHeader, userLeague: UserLeague, userEmail: String
    ): LeagueCreationResult {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/create_new_league"
            val body = buildJsonObject {
                put("user_token", token)
                put("_leagueid", leagueHeader.leagueid)
                put("_competitionid", leagueHeader.competitionid)
            }

            val result: String = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()

            val jsonResponse = json.parseToJsonElement(result)
            val leagueCode = (jsonResponse as? kotlinx.serialization.json.JsonObject)?.get("league_code")?.toString()?.trim('"') ?: ""
            val newId = (jsonResponse as? kotlinx.serialization.json.JsonObject)?.get("new_id")?.toString()?.trim('"') ?: ""

            dao.insertLeagueHeader(leagueHeader)
            leagueHeader.leagueusers.forEach { dao.upsertLeagueInDb(it) }
            setupRepository.refreshSetupFromServer(leagueHeader.owneruserid)

            LeagueCreationResult(newId, leagueCode)
        }
    }

    override suspend fun updateLeagueInvitee(
        leagueid: String, email: String, status: String, invited: Boolean, selected: Boolean
    ) {
        withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_league_invitee"
            val body = buildJsonObject {
                put("user_token", token)
                put("l_id", leagueid)
                put("_email", email)
                put("_status", status)
                put("is_invited", invited)
                put("is_selected", selected)
            }
            try {
                httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    override suspend fun getEditLeague(leagueid: String, owneruserid: String): LeagueHeader? {
        return withContext(Dispatchers.IO) {
            val local = dao.getLeagueHeader(leagueid, owneruserid)
            if (local != null) return@withContext local

            try {
                val url = "https://www.scoreprof.cloud/rpc/getleagueuserstats"
                val token = tokenManager.getToken() ?: ""
                val body = buildJsonObject {
                    put("l_id", leagueid)
                    put("o_id", owneruserid)
                    put("user_token", token)
                }

                val response: String = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.body()

                val userLeagueUsers = json.decodeFromString<List<UserLeagueUsers>>(response)
                val firstUser = userLeagueUsers.firstOrNull() ?: throw Exception("Empty list")

                val leagueUsers = userLeagueUsers.map { status ->
                    League(
                        leagueid = leagueid,
                        owneruserid = owneruserid,
                        userid = status.userid,
                        username = status.email.substringBefore("@")
                    )
                }

                val remoteHeader = LeagueHeader(
                    leagueid = leagueid,
                    owneruserid = owneruserid,
                    competitionid = firstUser.competitionid,
                    leagueusers = leagueUsers
                )

                dao.insertLeagueHeader(remoteHeader)
                remoteHeader
            } catch (e: Exception) {
                dao.getLeagueHeader(leagueid, owneruserid)
            }
        }
    }

    override suspend fun saveEditLeague(
        originalLeagueId: String, leagueHeader: LeagueHeader, userLeague: UserLeague, userEmail: String
    ) {
        withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_league"
            val body = buildJsonObject {
                put("user_token", token)
                put("original_leagueid", originalLeagueId)
                put("new_leagueid", leagueHeader.leagueid)
                put("_competitionid", leagueHeader.competitionid)
            }

            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            val localUsers = dao.getLeagueUserStatuses(originalLeagueId, leagueHeader.owneruserid)
            val localEmailMap = localUsers.associateBy { it.email.lowercase().trim() }

            userLeague.userleagueusers.forEach { uiUser ->
                val email = uiUser.email.lowercase().trim()
                if (email == userEmail.lowercase().trim()) return@forEach

                val localRecord = localEmailMap[email]
                val isReinvite = localRecord?.state?.lowercase() == "deleted"
                val isNew = localRecord == null

                if (isNew || isReinvite) {
                    updateLeagueInvitee(leagueHeader.leagueid, uiUser.email, "Pending", true, true)
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
                dao.upsertLeagueInDb(user.copy(leagueid = leagueHeader.leagueid))
            }

            if (originalLeagueId != leagueHeader.leagueid) {
                dao.deleteLeague(originalLeagueId, leagueHeader.owneruserid)
            }

            setupRepository.refreshSetupFromServer(leagueHeader.owneruserid)
        }
    }

    override suspend fun softDeleteLeagueInvitee(leagueid: String, owneruserid: String, email: String) {
        updateLeagueInvitee(leagueid, email, "Deleted", false, false)
        withContext(Dispatchers.IO) {
            val localStatuses = dao.getLeagueUserStatuses(leagueid, owneruserid)
            localStatuses.find { it.email.lowercase().trim() == email.lowercase().trim() }?.let {
                dao.upsertUserLeagueUsersInDb(it.copy(state = "Deleted"))
            }

            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/soft_delete_league_invitee"
            val body = buildJsonObject {
                put("user_token", token)
                put("_leagueid", leagueid)
                put("_owneruserid", owneruserid)
                put("_email", email)
            }
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    override suspend fun softDeleteLeague(leagueid: String, owneruserid: String) {
        withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/soft_delete_league"
            val body = buildJsonObject {
                put("user_token", token)
                put("_leagueid", leagueid)
                put("_owneruserid", owneruserid)
            }
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    override suspend fun insertLeagues(leagues: Leagues) {
        dao.insertLeagues(leagues)
    }
}
