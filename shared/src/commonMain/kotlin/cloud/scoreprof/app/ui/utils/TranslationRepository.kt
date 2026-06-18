package cloud.scoreprof.app.ui.utils

import cloud.scoreprof.app.domain.model.Country
import cloud.scoreprof.app.domain.model.CountryHeader
import kotlinx.serialization.json.Json
import cloud.scoreprof.app.Platform

class TranslationRepository(
    private val platform: Platform
) {

    private var countries: List<Country> = emptyList()

    suspend fun loadCountries(jsonString: String) {
        try {
            val countryHeaderData = Json.decodeFromString<CountryHeader>(jsonString)
            val countryMap = countryHeaderData.countries.firstOrNull() ?: emptyMap()

            countries = countryMap.map { (code, translations) ->
                Country(
                    countryCode = code,
                    en = translations["en"] ?: "",
                    translations = translations
                )
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun translateCountry(englishName: String): String {
        val country = countries.find { it.en.equals(englishName, ignoreCase = true) }
            ?: return englishName

        val localeLanguage = platform.language
        return country.translations[localeLanguage] ?: country.en
    }
}
