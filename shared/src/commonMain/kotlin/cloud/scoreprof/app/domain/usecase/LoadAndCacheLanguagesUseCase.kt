package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.LanguageRepository

class LoadAndCacheLanguagesUseCase(private val repository: LanguageRepository) {
    suspend operator fun invoke(competitionId: String) {
        //repository.loadAndCacheLanguagesFromJson(preferredLanguage)
    }
}