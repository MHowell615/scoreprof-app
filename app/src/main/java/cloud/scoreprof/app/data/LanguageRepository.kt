package cloud.scoreprof.app.data

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import cloud.scoreprof.app.domain.model.Language
import cloud.scoreprof.app.domain.model.LanguageEntity
import cloud.scoreprof.app.domain.model.LanguageResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface LanguageRepository {
    suspend fun getLanguages(preferredLanguage: String): List<Language>
}

class LanguageRepositoryImpl @Inject constructor(
    private val dao: ScoreProfDao,
    @ApplicationContext private val context: Context
) : LanguageRepository {

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

    override suspend fun getLanguages(preferredLanguage: String): List<Language> {
        val localLanguages = dao.getLanguages().firstOrNull()

        // If the database is empty, fetch from the network.
        if (localLanguages.isNullOrEmpty()) {
            val url = "https://www.scoreprof.cloud/rpc/getlanguages?preferredlanguage=$preferredLanguage"
            println("TEST: url = $url")
            val responseString = fetchFromServer(url)

            // Decode the JSON response into a list of Language objects.
            val languageResponse = json.decodeFromString<LanguageResponse>(responseString)
            val languagesFromApi = languageResponse.languages

            // Save the fresh data to the database.
            // We need a mapping function to convert Language -> LanguageEntity
            val languageEntities = languagesFromApi.map { lang ->
                LanguageEntity(languageCode = lang.languageCode, languageName = lang.languageName)
            }
            dao.upsertLanguages(languageEntities) // Assuming upsertLanguages now accepts LanguageEntity
        }

        // Return the data from the database (which is now guaranteed to be populated).
        // We map the Entity back to the domain model 'Language'.
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

    private suspend fun fetchFromServer(url: String): String {
        return suspendCancellableCoroutine { continuation ->
            val stringRequest = StringRequest(
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
            )

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