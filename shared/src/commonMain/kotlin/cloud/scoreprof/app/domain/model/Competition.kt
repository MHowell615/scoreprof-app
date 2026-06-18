package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class CompetitionsData(
    val version: Int,
    val competitions: List<Competition>
)

@Serializable
@Entity(tableName = "Competitions")
data class Competition(
    @PrimaryKey val id: Int = 0,
    val competitionid: String,
    val sport_type: String?,
    val region: String?,
    val country_ranking: Int?,
    val name: String,
    val has_upcoming: Boolean?
)

fun groupAndSortCompetitions(
    competitions: List<Competition>): Map<String,List<Competition>>
{
    return competitions
        .groupBy { it.sport_type ?: "Other" } // Group by sport_type
        .toList()
        .sortedBy { it.first }
        .toMap()
        .mapValues { entry ->
            entry.value.sortedBy { it.name }  // Optional: Sort competitions within each group
        }
}

