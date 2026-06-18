package cloud.scoreprof.app.data

import androidx.room.TypeConverter
import cloud.scoreprof.app.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }

    @TypeConverter
    fun fromNotificationType(type: NotificationType): String = type.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType = NotificationType.valueOf(value)

    @TypeConverter
    fun fromMatchList(list: List<Match>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun toMatchList(value: String?): List<Match>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromLeagueList(list: List<League>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun toLeagueList(value: String?): List<League>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromLeaguesList(list: List<Leagues>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun toLeaguesList(value: String?): List<Leagues>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromCompetitionSelectionList(list: List<UserCompetitionSelection>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun toCompetitionSelectionList(value: String?): List<UserCompetitionSelection>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromLeagueSelectionList(list: List<UserLeagueSelection>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun toLeagueSelectionList(value: String?): List<UserLeagueSelection>? = value?.let { json.decodeFromString(it) }
}
