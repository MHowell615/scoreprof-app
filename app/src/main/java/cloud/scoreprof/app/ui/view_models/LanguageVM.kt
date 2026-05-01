package cloud.scoreprof.app.ui.view_models

import cloud.scoreprof.app.domain.model.Language

data class LanguageVM(
    val languageCode: String,
    val languageName: String,
    val isSelected: Boolean
) {
    companion object {
        fun fromModel(entity: Language, isSelected: Boolean): LanguageVM {
            return LanguageVM(
                languageCode = entity.languageCode,
                languageName = entity.languageName,
                isSelected = isSelected
            )
        }
    }
}