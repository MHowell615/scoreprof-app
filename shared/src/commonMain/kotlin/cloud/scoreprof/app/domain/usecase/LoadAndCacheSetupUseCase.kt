package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.SetupRepository


class LoadAndCacheSetupUseCase(private val repository: SetupRepository) {
    suspend operator fun invoke(userid: String) {
        //repository.loadAndCacheSetupFromJson(userid)
    }
}