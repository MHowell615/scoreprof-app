package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Language(
    var languageCode: String,
    var languageName: String,
    val isSelected: Boolean = false
)

@Serializable
data class LanguageResponse(
    val languages: List<Language>
)

@Serializable
data class LanguageDirection(
    val language_code: String,
    val direction: String
)