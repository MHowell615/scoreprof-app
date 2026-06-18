package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.MatchRepository

class LoadAndCacheMatchesUseCase(private val repository: MatchRepository) {
    suspend operator fun invoke(competitionId: String, userid: String) {
        repository.loadAndCacheMatchesFromJson(competitionId, userid)
    }
}
