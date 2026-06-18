package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.MatchRepository


class UpdatePredictionInDbUseCase constructor(
    private val repository: MatchRepository
) {
    suspend operator fun invoke(matchid: Int, competitor1selected: Boolean, competitor2selected: Boolean) {
        repository.updateLocalPrediction(matchid, competitor1selected, competitor2selected)
    }
}