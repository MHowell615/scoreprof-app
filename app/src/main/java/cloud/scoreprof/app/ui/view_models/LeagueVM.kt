package cloud.scoreprof.app.ui.view_models

import cloud.scoreprof.app.domain.model.League
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.LeagueTable
import java.util.UUID

data class LeagueVM(
    //val id: Int,
    val leagueid: String,
    val owneruserid: UUID,
    val userid: UUID,
    val username: String
) {
    companion object {
        fun fromEntity(entity: League): LeagueVM {
            return LeagueVM(
                //id = entity.id,
                leagueid = entity.leagueid,
                owneruserid = entity.owneruserid,
                userid = entity.userid,
                username = entity.username/*,
                position = entity.position,
                matches_played = entity.matches_played,
                points = entity.points*/
            )
        }
    }
}

data class LeagueHeaderVM(
    val leagueid: String,
    val owneruserid: UUID,
    val competitionid: String?,
    val leagueusers: List<LeagueVM>
) {
    companion object {
        fun fromEntityHeader(entity: LeagueHeader): LeagueHeaderVM {
            return LeagueHeaderVM(
                leagueid = entity.leagueid,
                owneruserid = entity.owneruserid,
                competitionid = entity.competitionid,
                leagueusers = entity.leagueusers.map { LeagueVM.fromEntity(it) }
            )
        }
    }
}

fun LeagueVM.toEntity(): League {
    //val id = this.id
    return League(
        //id = id,
        leagueid = this.leagueid,
        owneruserid = this.owneruserid,
        userid = this.userid,
        username = this.username/*,
        position = this.position,
        matches_played = this.matches_played,
        points = this.points*/
    )
}

fun LeagueHeaderVM.toEntity(): LeagueHeader {
    val leagueid = this.leagueid
    return LeagueHeader(
        leagueid = leagueid,
        owneruserid = this.owneruserid,
        competitionid = this.competitionid,
        leagueusers = this.leagueusers.map { it.toEntity() }
    )
}

data class LeagueTableVM(
    val leagueid: String,
    val owneruserid: UUID,
    val rank: Int,
    val userid: UUID,
    val username: String,
    val matches_played: Int,
    val points: Int,
    val win_percentage: Double
) {
    companion object {
        fun fromEntityHeader(entity: LeagueTable): LeagueTableVM {
            return LeagueTableVM(
                leagueid = entity.leagueid,
                owneruserid = entity.owneruserid,
                rank = entity.rank,
                userid = entity.userid,
                username = entity.username,
                matches_played = entity.matches_played,
                points = entity.points,
                win_percentage = entity.win_percentage
            )
        }
    }
}

fun LeagueTableVM.toEntity(): LeagueTable {
    return LeagueTable(
        leagueid = this.leagueid,
        owneruserid = this.owneruserid,
        rank = this.rank,
        userid = this.userid,
        username = this.username,
        matches_played = this.matches_played,
        points = this.points,
        win_percentage = this.win_percentage
    )
}