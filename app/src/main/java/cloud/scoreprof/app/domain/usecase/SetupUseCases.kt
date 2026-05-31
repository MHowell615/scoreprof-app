package cloud.scoreprof.app.domain.usecase

data class SetupUseCases(
    val getSetup: GetSetupUseCase,
    val loadAndCacheSetup: LoadAndCacheSetupUseCase,
    val updateUserCompetition: UpdateUserCompetitionUseCase,
    val sendSupportEmail: SendSupportEmailUseCase,
    val updateAllUserCompetitions: UpdateAllUserCompetitionsCase
)