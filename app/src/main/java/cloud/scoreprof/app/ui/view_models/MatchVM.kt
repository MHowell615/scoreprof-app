package cloud.scoreprof.app.ui.view_models

import cloud.scoreprof.app.domain.model.Match
import java.time.ZonedDateTime

data class MatchVM(
    val id: Int,
    val competitionid: String,
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
) {
    fun toEntity(): Match {
        return Match(
            id = id,
            competitionid = competitionid,
            kickoff = kickoff,
            stage = stage,
            venue = venue,
            competitor1 = competitor1,
            competitor2 = competitor2,
            score1 = score1,
            score2 = score2,
            winner = winner,
            supplement = supplement,
            competitor1selected = this.competitor1selected,
            competitor2selected = this.competitor2selected
        )
    }

    companion object {
        fun fromEntity(match: Match): MatchVM {
            return MatchVM(
                id = match.id,
                competitionid = match.competitionid,
                kickoff = match.kickoff,
                stage = match.stage,
                venue = match.venue,
                competitor1 = match.competitor1,
                competitor2 = match.competitor2,
                score1 = match.score1,
                score2 = match.score2,
                winner = match.winner,
                supplement = match.supplement,
                competitor1selected = match.competitor1selected,
                competitor2selected = match.competitor2selected
            )
        }
    }
}




