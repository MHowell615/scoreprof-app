package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import cloud.scoreprof.app.ui.utils.ZonedDateTimeSerializer
import java.time.ZonedDateTime
import kotlinx.serialization.Serializable
import kotlin.String

@Serializable
@Entity(tableName = "matches")
data class Match(
    @PrimaryKey val id: Int,
    val competitionid: String,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val kickoff: ZonedDateTime,
    val stage: String ?= null,
    val venue: String?,
    val competitor1: String,
    val competitor2: String,
    val score1: Int? = null,
    val score2: Int? = null,
    val winner: Int? = null,
    val supplement: String? = null,
    val competitor1selected: Boolean = false,
    val competitor2selected: Boolean = false
)

@Serializable
data class MatchHeader(
    val version: Int,
    val competitionid: String,
    val competitortype: String ?= null,
    val competition: String,
    val matches: List<Match>
)

fun Match.toEntity(): Match {
    return Match(
        id = this.id,
        competitionid = this.competitionid,
        kickoff = this.kickoff,
        stage = this.stage,
        venue = this.venue,
        competitor1 = this.competitor1,
        competitor2 = this.competitor2,
        score1 = this.score1,
        score2 = this.score2,
        winner = this.winner,
        supplement = this.supplement,
        competitor1selected = this.competitor1selected,
        competitor2selected = this.competitor2selected
    )
}

