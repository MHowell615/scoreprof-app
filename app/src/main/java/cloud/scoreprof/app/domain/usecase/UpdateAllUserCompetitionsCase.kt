package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.SetupRepository
import javax.inject.Inject

class UpdateAllUserCompetitionsCase @Inject constructor(
    private val repository: SetupRepository
) {
    suspend operator fun invoke(isSelected: Boolean) {
        repository.updateAllUserCompetitions(isSelected)
    }
}
