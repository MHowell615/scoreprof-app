package cloud.scoreprof.app.ui.view_models

import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.domain.model.LeaguesHeader
import java.util.UUID

data class LeaguesVM(
    val id: Int,
    val leagueid: String,
    val competitionid: String?,
    val leaguecode: String?,
    val owneruserid: UUID,
    val userid: UUID,
    val name: String,
    val state: String
) {
    companion object {
        fun fromEntity(entity: Leagues): LeaguesVM {
            return LeaguesVM(
                id = entity.id,
                leagueid = entity.leagueid,
                competitionid = entity.competitionid,
                leaguecode = entity.leaguecode,
                owneruserid = entity.owneruserid,
                userid = entity.owneruserid,
                name = entity.name,
                state = entity.state
            )
        }
    }
}

data class LeaguesHeaderVM(
    val leagues: List<LeaguesVM>
) {
    companion object {
        fun fromEntityHeader(entity: LeaguesHeader): LeaguesHeaderVM {
            return LeaguesHeaderVM(
                leagues = entity.leagues.map { LeaguesVM.fromEntity(it) }
            )
        }
    }
}

fun LeaguesVM.toEntity(): Leagues {
    val id = this.id
    return Leagues(
        id = id,
        leagueid = this.leagueid,
        competitionid = this.competitionid,
        leaguecode = this.leaguecode,
        owneruserid = this.owneruserid,
        name = this.name,
        state = this.state,
        invited = false,
        selected = false
    )
}

fun LeaguesHeaderVM.toEntity(): LeaguesHeader {
    return LeaguesHeader(
        leagues = this.leagues.map { it.toEntity() }
    )
}