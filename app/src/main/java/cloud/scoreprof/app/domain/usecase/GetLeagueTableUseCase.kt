package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.LeaguesRepository
import cloud.scoreprof.app.domain.model.LeagueTable
import kotlinx.coroutines.flow.Flow

class GetLeagueTableUseCase(private val repository: LeaguesRepository) {
    suspend operator fun invoke(leagueid: String, owneruserid: String, sortBy: String): Flow<List<LeagueTable>> {
        return repository.getLeagueTable(leagueid, owneruserid, sortBy)
    }
}