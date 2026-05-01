package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.MatchRepository

class LoadAndCacheMatchesUseCase(private val repository: MatchRepository, private val userid: String) {
    suspend operator fun invoke(competitionId: String, userid: String) {
        repository.loadAndCacheMatchesFromJson(competitionId, userid)
    }
}
