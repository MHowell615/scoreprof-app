package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import cloud.scoreprof.app.ui.utils.InstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "matches")
data class Match(
    @PrimaryKey val id: Int,
    val competitionid: String,
    @Serializable(with = InstantSerializer::class)
    val kickoff: Instant,
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
