package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.SetupRepository
import java.util.UUID

class LoadAndCacheSetupUseCase(private val repository: SetupRepository) {
    suspend operator fun invoke(userid: UUID) {
        //repository.loadAndCacheSetupFromJson(userid)
    }
}