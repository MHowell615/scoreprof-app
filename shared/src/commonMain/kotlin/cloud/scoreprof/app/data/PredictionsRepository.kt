package cloud.scoreprof.app.data

import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.UserPredictionUpdate
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface PredictionUpdateRepository {
    suspend fun updatePredictionOnServer(predictionUpdate: UserPredictionUpdate)
}

class PredictionUpdateRepositoryImpl(
    private val tokenManager: TokenManager,
    private val httpClient: HttpClient
) : PredictionUpdateRepository {

    override suspend fun updatePredictionOnServer(predictionUpdate: UserPredictionUpdate) {
        withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: ""
            val url = "https://www.scoreprof.cloud/rpc/update_user_prediction"

            val body = buildJsonObject {
                put("user_token", token)
                put("_competitor1selected", predictionUpdate.competitor1selected)
                put("_competitor2selected", predictionUpdate.competitor2selected)
                put("_matchid", predictionUpdate.matchid)
            }

            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }
}
