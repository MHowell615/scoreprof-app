package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.MatchRepository
import javax.inject.Inject

class HasMatchesUseCase @Inject constructor(
    private val repository: MatchRepository
) {
    suspend operator fun invoke(competitionId: String): Boolean {
        return repository.hasMatches(competitionId)
    }
}