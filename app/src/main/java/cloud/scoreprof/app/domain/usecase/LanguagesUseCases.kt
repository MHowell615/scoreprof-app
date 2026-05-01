package cloud.scoreprof.app.domain.usecase

data class LanguagesUseCases(
    val getLanguages: GetLanguagesUseCase,
    val loadAndCacheLanguages: LoadAndCacheLanguagesUseCase
)

