package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.SetupRepository


class UpdateUserCompetitionUseCase constructor(
    private val repository: SetupRepository
) {
    suspend operator fun invoke(competitionid: String, isSelected: Boolean) {
        repository.updateUserCompetition(competitionid, isSelected)
    }
}