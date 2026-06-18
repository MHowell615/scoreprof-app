package cloud.scoreprof.app.data

import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.Setup
import cloud.scoreprof.app.Platform
import cloud.scoreprof.app.domain.model.ActivateAccountRequest
import cloud.scoreprof.app.domain.model.JoinLeagueRequest
import cloud.scoreprof.app.domain.model.LoginRequest
import cloud.scoreprof.app.domain.model.LoginResponse
import cloud.scoreprof.app.domain.model.ReportCrashRequest
import cloud.scoreprof.app.domain.model.SendSupportEmailRequest
import cloud.scoreprof.app.domain.model.UpdateAdsRemovedRequest
import cloud.scoreprof.app.domain.model.UpdateAllUserCompetitionsRequest
import cloud.scoreprof.app.domain.model.UpdateUserCompetitionRequest
import cloud.scoreprof.app.domain.model.UpdateUserPrivacyRequest
import cloud.scoreprof.app.domain.model.UpdateUserProfileRequest
import cloud.scoreprof.app.domain.model.UserLeagueUpdateRequest
import cloud.scoreprof.app.ui.view_models.LoginViewModel.UiEvent
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface SetupRepository {
    suspend fun getSetup(userid: String) : Flow<Setup?>
    suspend fun refreshSetupFromServer(userid: String)
    suspend fun upsertSetup(setup: Setup)
    suspend fun updateUserCompetition(competitionid: String, isSelected: Boolean)
    suspend fun updateUserProfile(name: String, email: String, language: String)
    suspend fun activateAccount(email: String, preferredLanguage: String)
    suspend fun logout()
    suspend fun updateUserLeague(leagueid: String, owneruserid: String, isSelected: Boolean)
    suspend fun sendSupportEmail(userEmail: String, category: String, subject: String, description: String): Boolean
    suspend fun updateUserPrivacy(receiveEmail: Boolean)
    suspend fun requestJoinLeague(joinCode: String)
    suspend fun updateAdsRemoved(isRemoved: Boolean)
    suspend fun updateAllUserCompetitions(isSelected: Boolean)
    suspend fun logError(errorMessage: String, stackTrace: String, appVersion: String)
}

class SetupRepositoryImpl(
    private val dao: ScoreProfDao,
    private val tokenManager: TokenManager,
    private val httpClient: HttpClient,
    private val platform: Platform,
    private val authKey: String
) : SetupRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getSetup(userid: String): Flow<Setup?> {
        return dao.getSetup()
    }

    override suspend fun refreshSetupFromServer(userid: String) {
        val token = tokenManager.getToken() ?: ""
        if (token.isBlank()) throw IllegalStateException("SESSION_EXPIRED")

        val language = platform.language
        val currentVersion = platform.version

        val url = if (currentVersion > 7) {
            "https://www.scoreprof.cloud/rpc/getsetupdata?user_token=$token&lang=$language"
        } else {
            "https://www.scoreprof.cloud/rpc/getsetupdata?user_token=$token"
        }

        val responseString: String = httpClient.get(url).body()

        if (responseString.isNotBlank() && responseString != "null") {
            val setupData = json.decodeFromString<Setup>(responseString)
            withContext(Dispatchers.IO) {
                dao.upsertSetup(setupData)
                dao.upsertSetupCompetitions(setupData.competitions)
                dao.upsertSetupLeagues(setupData.leagues)
            }
        }
    }

    override suspend fun logout() {
        tokenManager.deleteToken()
        dao.deleteSetup()
    }

    override suspend fun updateUserLeague(leagueid: String, owneruserid: String, isSelected: Boolean) {
        try {
            httpClient.post("https://www.scoreprof.cloud/rpc/update_user_league") {
                contentType(ContentType.Application.Json)
                setBody(UserLeagueUpdateRequest(
                    user_token = tokenManager.getToken() ?: "",
                    _leagueid = leagueid,
                    _owneruserid = owneruserid,
                    _isselected = isSelected
                ))
            }
        } catch (e: Exception) {
            logError(e.message.toString(), e.stackTraceToString(), platform.version.toString())
        }

        // dao.updateUserLeagueSelection(leagueid, owneruserid, isSelected) // owneruserid is UUID in DAO, needs fix
    }

    override suspend fun sendSupportEmail(userEmail: String, category: String, subject: String, description: String): Boolean {
        try {
            httpClient.post("https://api.scoreprof.cloud/send-support") {
                contentType(ContentType.Application.Json)
                setBody(SendSupportEmailRequest(
                    userEmail = userEmail,
                    category = category,
                    subject = "Support Request; $category - $subject",
                    description = description,
                    authKey = authKey
                ))
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    override suspend fun upsertSetup(setup: Setup) {
        dao.upsertSetup(setup)
    }

    override suspend fun logError(errorMessage: String, stackTrace: String, appVersion: String) {
        try {
            httpClient.post("https://api.scoreprof.cloud/logs/error") {
                contentType(ContentType.Application.Json)
                setBody(ReportCrashRequest(
                    userid = tokenManager.getUserId() ?: "",
                    error_message = errorMessage,
                    stack_trace = stackTrace,
                    app_version = appVersion,
                    device_model = platform.deviceModel
                ))
            }
        } catch (e: Exception) {
            // TODO: Log to console if remote log fails
        }
    }

    override suspend fun requestJoinLeague(joinCode: String) {
        try {
            httpClient.post("https://www.scoreprof.cloud/rpc/request_join_league") {
                contentType(ContentType.Application.Json)
                setBody(JoinLeagueRequest(
                    _league_code = joinCode,
                    user_token = tokenManager.getToken() ?: ""
                ))
            }
        } catch (e: Exception) {

        }
    }

    override suspend fun activateAccount(email: String, preferredLanguage: String) {
        try {
            httpClient.post("https://www.scoreprof.cloud/rpc/activate_pending_user") {
                contentType(ContentType.Application.Json)
                setBody(ActivateAccountRequest(
                    user_token = tokenManager.getToken() ?: "",
                    _email = email,
                    _language = preferredLanguage
                ))
            }
        } catch (e: Exception) {
            logError(e.message.toString(), e.stackTraceToString(), platform.version.toString())
        }
    }

    override suspend fun updateUserProfile(name: String, email: String, language: String) {
        try {
            httpClient.post("https://www.scoreprof.cloud/rpc/update_user_profile") {
                contentType(ContentType.Application.Json)
                setBody(UpdateUserProfileRequest(
                    user_token = tokenManager.getToken() ?: "",
                    _username = name,
                    _email = email,
                    _language = language
                ))
            }
        } catch (e: Exception) {
            logError(e.message.toString(), e.stackTraceToString(), platform.version.toString())
        }
    }

    override suspend fun updateUserCompetition(competitionid: String, isSelected: Boolean) {
        try {
            httpClient.post("https://www.scoreprof.cloud/rpc/update_user_competition") {
                contentType(ContentType.Application.Json)
                setBody(UpdateUserCompetitionRequest(
                    user_token = tokenManager.getToken() ?: "",
                    _competitionid = competitionid,
                    _isselected = isSelected
                ))
            }
        } catch (e: Exception) {
            logError(e.message.toString(), e.stackTraceToString(), platform.version.toString())
        }
    }

    override suspend fun updateAllUserCompetitions(isSelected: Boolean) {
        try {
            httpClient.post("https://www.scoreprof.cloud/rpc/update_all_user_competitions") {
                contentType(ContentType.Application.Json)
                setBody(UpdateAllUserCompetitionsRequest(
                    user_token = tokenManager.getToken() ?: "",
                    _isselected = isSelected
                ))
            }
        } catch (e: Exception) {
            logError(e.message.toString(), e.stackTraceToString(), platform.version.toString())
        }
    }

    override suspend fun updateUserPrivacy(receiveEmail: Boolean) {
        try {
            httpClient.post("https://www.scoreprof.cloud/rpc/update_user_privacy") {
                contentType(ContentType.Application.Json)
                setBody(UpdateUserPrivacyRequest(
                    user_token = tokenManager.getToken() ?: "",
                    _receive_email = receiveEmail
                ))
            }
        } catch (e: Exception) {
            logError(e.message.toString(), e.stackTraceToString(), platform.version.toString())
        }
    }

    override suspend fun updateAdsRemoved(isRemoved: Boolean) {
        try {
            httpClient.post("https://www.scoreprof.cloud/rpc/update_ads_removed") {
                contentType(ContentType.Application.Json)
                setBody(UpdateAdsRemovedRequest(
                    user_token = tokenManager.getToken() ?: "",
                    _is_ads_removed = isRemoved
                ))
            }
        } catch (e: Exception) {
            logError(e.message.toString(), e.stackTraceToString(), platform.version.toString())
        }
    }
}
