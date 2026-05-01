package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "Country")
data class Country(
    @PrimaryKey // The country code is the unique primary key
    val countryCode: String,
    val en: String,
    val translations: Map<String, String>
)

@Serializable
data class CountryHeader(
    val version: String,
    val countries: List<Map<String, Map<String, String>>>
)