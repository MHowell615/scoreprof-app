package cloud.scoreprof.app.ui.utils

import android.content.Context
import cloud.scoreprof.app.domain.model.Country
import cloud.scoreprof.app.domain.model.CountryHeader
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.Locale

class TranslationRepository(private val context: Context) {

    // Lazily load and cache the country data so we only read the file once.
    private val countries: List<Country> by lazy {
        loadCountriesFromJson()
    }

    private fun loadCountriesFromJson(): List<Country> {
        return try {
            val jsonString = context.assets.open("country.json").bufferedReader().use { it.readText() }
            val countryHeaderData = Json.decodeFromString<CountryHeader>(jsonString)
            val countryMap = countryHeaderData.countries.firstOrNull() ?: emptyMap()

            // Convert the complex map from JSON into a cleaner List<Country>
            countryMap.map { (code, translations) ->
                Country(
                    countryCode = code,
                    en = translations["en"] ?: "", // The English name
                    translations = translations
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Translates a country name if possible, otherwise returns the original name.
     */
    fun translateCountry(englishName: String): String {
        // Find the country object where the English name matches.
        val country = countries.find { it.en.equals(englishName, ignoreCase = true) }

        if (country == null) {
            return englishName // If not found, return the original English name.
        }

        // Get the device's current language (e.g., "de", "fr").
        val localeLanguage = Locale.getDefault().language

        // Return the translation for the current locale, or fall back to English if it doesn't exist.
        return country.translations[localeLanguage] ?: country.en
    }

    companion object {}
}
