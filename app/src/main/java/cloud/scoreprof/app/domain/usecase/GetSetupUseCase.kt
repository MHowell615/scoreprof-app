package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.SetupRepository
import cloud.scoreprof.app.domain.model.Setup
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class GetSetupUseCase(private val repository: SetupRepository) {
    suspend operator fun invoke(userid: UUID): Flow<Setup?> {
        return repository.getSetup(userid)
    }
}