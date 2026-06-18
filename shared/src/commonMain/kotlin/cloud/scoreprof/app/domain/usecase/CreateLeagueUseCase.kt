package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.LeaguesRepository
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.UserLeague

class CreateLeagueUseCase(private val repository: LeaguesRepository) {
    suspend operator fun invoke(leagueHeader: LeagueHeader, userLeague: UserLeague, userEmail: String) {
        repository.createNewLeague(leagueHeader, userLeague, userEmail)
    }
}
