package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.MatchRepository
import cloud.scoreprof.app.domain.model.Match
import kotlinx.coroutines.flow.Flow

class GetMatchesUseCase(private val repository: MatchRepository) {
    suspend operator fun invoke(competitionid: String): Flow<List<Match>> {
        return repository.getMatchesByCompetition(competitionid)
    }
}