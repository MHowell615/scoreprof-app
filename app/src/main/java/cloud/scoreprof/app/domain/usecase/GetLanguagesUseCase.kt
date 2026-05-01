package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.LanguageRepository
import cloud.scoreprof.app.domain.model.Language

class GetLanguagesUseCase(private val repository: LanguageRepository) {
    suspend operator fun invoke(preferredLanguage: String): List<Language> {
        return repository.getLanguages(preferredLanguage)
    }
}