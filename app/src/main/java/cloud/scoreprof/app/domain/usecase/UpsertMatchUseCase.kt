package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.MatchRepository
import cloud.scoreprof.app.domain.model.Match

class UpsertMatchUseCase(private val repository: MatchRepository) {
    suspend operator fun invoke(match: Match) {
        repository.upsertMatch(match)
    }
}