package cloud.scoreprof.app.data

import androidx.room.TypeConverter
import cloud.scoreprof.app.domain.model.League
import cloud.scoreprof.app.domain.model.LeagueTable
import cloud.scoreprof.app.domain.model.Match
import cloud.scoreprof.app.domain.model.UserCompetitionSelection
import cloud.scoreprof.app.domain.model.UserLeagueSelection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime

class Converters {
    private val json = Json {
        ignoreUnknownKeys = true // Ignores extra fields
        coerceInputValues = true // Uses default values for missing/null fields
    }
    @TypeConverter
    fun fromZonedDateTime(value: ZonedDateTime?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toZonedDateTime(value: String?): ZonedDateTime? {
        return value?.let {
            ZonedDateTime.parse(value)
        }
    }

    @TypeConverter
    fun fromUUID(uuid: java.util.UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUUID(uuidString: String?): java.util.UUID? =
        uuidString?.let { java.util.UUID.fromString(it) }

    @TypeConverter
    fun fromMatchList(value: String): List<Match> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun toMatchList(list: List<Match>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromStringMap(value: String): Map<String, String> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun toStringMap(map: Map<String, String>): String {
        return json.encodeToString(map)
    }

    @TypeConverter
    fun fromUserCompetitionSelectionList(value: List<UserCompetitionSelection>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toUserCompetitionSelectionList(value: String): List<UserCompetitionSelection> {
        // Handle empty or invalid string case to prevent crashes
        return if (value.isEmpty()) emptyList() else json.decodeFromString(value)
    }

    // --- NEW: Converter for UserLeagueSelection List ---
    @TypeConverter
    fun fromUserLeagueSelectionList(value: List<UserLeagueSelection>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toUserLeagueSelectionList(value: String): List<UserLeagueSelection> {
        // Handle empty or invalid string case to prevent crashes
        return if (value.isEmpty()) emptyList() else json.decodeFromString(value)
    }

    @TypeConverter
    fun fromLeagueList(value: String): List<League> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun toLeagueList(list: List<League>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromLeagueTableList(value: String): List<LeagueTable> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun toLeagueTableList(list: List<LeagueTable>): String {
        return json.encodeToString(list)
    }
}