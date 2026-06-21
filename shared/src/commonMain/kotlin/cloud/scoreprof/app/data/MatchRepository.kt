package cloud.scoreprof.app.data

import cloud.scoreprof.app.data.local.TokenManager
import cloud.scoreprof.app.domain.model.Match
import cloud.scoreprof.app.domain.model.MatchHeader
import cloud.scoreprof.app.Platform
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface MatchRepository {
    fun getMatchesByCompetition(competitionId: String): Flow<List<Match>>
    suspend fun upsertMatches(matches: List<Match>)
    suspend fun loadAndCacheMatchesFromJson(competitionId: String, userid: String, lang: String? = null)
    suspend fun upsertMatch(match: Match)
    suspend fun updateLocalPrediction(
        matchid: Int,
        competitor1selected: Boolean,
        competitor2selected: Boolean
    )
    suspend fun hasMatches(competitionId: String): Boolean
}

class MatchRepositoryImpl(
    private val dao: ScoreProfDao,
    private val tokenManager: TokenManager,
    private val httpClient: HttpClient,
    private val platform: Platform
) : MatchRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
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
        return dao.getMatchCountForCompetition(competitionId) > 0
    }

    override suspend fun loadAndCacheMatchesFromJson(competitionId: String, userid: String, lang: String?) {
        try {
            val token = tokenManager.getToken() ?: ""
            val language = lang ?: platform.language

            val url = "https://www.scoreprof.cloud/rpc/getmatchesbycomp?competition_id=$competitionId&lang=$language&user_token=$token"
            val responseString: String = httpClient.get(url).body()
            println("Matches response received. Sample: ${responseString.take(100)}")

            if (responseString.isNotBlank() && responseString != "null") {
                val matchHeader = json.decodeFromString<MatchHeader>(responseString)
                withContext(Dispatchers.IO) {
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
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
