package cloud.scoreprof.app.data

import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.Language
import cloud.scoreprof.app.domain.model.LanguageDirection
import cloud.scoreprof.app.domain.model.LanguageEntity
import cloud.scoreprof.app.domain.model.LanguageResponse
import cloud.scoreprof.app.domain.model.Setup
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface LanguageRepository {
    suspend fun getLanguages(preferredLanguage: String): List<Language>
    suspend fun getLanguageDirection(languageCode: String): String
}

class LanguageRepositoryImpl(
    private val dao: ScoreProfDao,
    private val tokenManager: TokenManager,
    private val httpClient: HttpClient
) : LanguageRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getLanguageDirection(languageCode: String): String {
        val token = tokenManager.getToken() ?: ""
        if (token.isBlank()) throw IllegalStateException("SESSION_EXPIRED")

        try {
            val url = "https://www.scoreprof.cloud/rpc/getlangdirection?user_token=$token&_language=$languageCode"
            val response: String = httpClient.get(url).body()
            val languageDirectionData = json.decodeFromString<LanguageDirection>(response)
            return languageDirectionData.direction
        } catch (e: Exception) {
            println("update_language server call failed: ${e.message}")
            return "ltr"
        }
    }

    override suspend fun getLanguages(preferredLanguage: String): List<Language> {
        val localLanguages = dao.getLanguages().firstOrNull()

        if (localLanguages.isNullOrEmpty()) {
            val url = "https://www.scoreprof.cloud/rpc/getlanguages?preferredlanguage=$preferredLanguage"
            val responseString: String = httpClient.get(url).body()

            val languageResponse = json.decodeFromString<LanguageResponse>(responseString)
            val languagesFromApi = languageResponse.languages

            val languageEntities = languagesFromApi.map { lang ->
                LanguageEntity(languageCode = lang.languageCode, languageName = lang.languageName)
            }
            dao.upsertLanguages(languageEntities)
        }

        return dao.getLanguages().first().map { entity ->
            Language(languageCode = entity.languageCode, languageName = entity.languageName, isSelected = false)
        }
    }

    fun getLanguagesFromDb(): Flow<List<LanguageEntity>> {
        return dao.getLanguages()
    }

    suspend fun upsertLanguages(languages: List<LanguageEntity>) {
        withContext(Dispatchers.IO) {
            dao.upsertLanguages(languages)
        }
    }
}
