package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.LeaguesRepository
import cloud.scoreprof.app.domain.model.Leagues

class GetLeaguesUseCase(private val repository: LeaguesRepository) {
    suspend operator fun invoke(userid: String): List<Leagues> {
        return repository.getLeagues(userid)
    }
}