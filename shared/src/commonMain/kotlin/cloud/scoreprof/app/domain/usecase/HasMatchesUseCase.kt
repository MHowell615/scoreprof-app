package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.MatchRepository


class HasMatchesUseCase constructor(
    private val repository: MatchRepository
) {
    suspend operator fun invoke(competitionId: String): Boolean {
        return repository.hasMatches(competitionId)
    }
}