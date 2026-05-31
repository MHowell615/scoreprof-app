package cloud.scoreprof.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import cloud.scoreprof.app.domain.model.Competition
import cloud.scoreprof.app.domain.model.LanguageEntity
import cloud.scoreprof.app.domain.model.League
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.LeagueInvitation
import cloud.scoreprof.app.domain.model.LeagueTable
import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.domain.model.Match
import cloud.scoreprof.app.domain.model.Setup
import cloud.scoreprof.app.domain.model.UserCompetitionSelection
import cloud.scoreprof.app.domain.model.UserLeagueSelection
import cloud.scoreprof.app.domain.model.UserPredictions
import cloud.scoreprof.app.domain.model.AppNotification
import cloud.scoreprof.app.domain.model.UserLeagueUsers
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ScoreProfDao {
    //@Insert(onConflict = OnConflictStrategy.REPLACE)
    //suspend fun insertAll(matches: List<Match>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLeague(league: List<League>)

    @Query("SELECT COUNT(id) FROM Matches")
    suspend fun count(): Int

    @Query("SELECT * FROM matches ORDER BY kickOff ASC")
    fun getAllMatches(): Flow<List<Match>>

    @Query("SELECT * FROM matches WHERE competitionid = :competitionid ORDER BY kickOff ASC")
    fun getMatchesByCompetition(competitionid: String): Flow<List<Match>>

    @Query("SELECT * FROM matches WHERE competitionId = :competitionId ORDER BY kickOff ASC")
    suspend fun getMatchesByCompetitionDebug(competitionId: String): List<Match>

    @Upsert
    suspend fun upsertMatch(match: Match)

    @Upsert
    suspend fun upsertPredictions(predictions: List<UserPredictions>)

    @Query("UPDATE matches SET competitor1selected = :competitor1selected, competitor2selected = :competitor2selected WHERE id = :matchid")
    suspend fun updatePrediction(matchid: Int, competitor1selected: Boolean, competitor2selected: Boolean)

    @Query("SELECT * FROM competitions ORDER BY id ASC")
    fun getSetupCompetitions(): Flow<List<Competition>>

    @Query(
            "SELECT * FROM League_Table " +
                    "WHERE leagueid = :leagueid AND owneruserid = :owneruserid " +
                    "ORDER BY points DESC"
    )
    fun getLeagueTable(leagueid: String, owneruserid: String): Flow<List<LeagueTable>>

    @Query("SELECT leaguecode FROM leagues WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    fun getLeagueCode(leagueid: String, owneruserid: UUID): String

    @Query("SELECT * FROM leagues WHERE UPPER(state) = 'ACTIVE' ORDER BY id ASC")
    suspend fun getSetupLeagues(): List<Leagues>

    @Query("SELECT * FROM setup WHERE userid = :userid")
    suspend fun getLatestSetup(userid: UUID): Setup?

    @Upsert
    suspend fun upsertSetupCompetitions(competitions: List<UserCompetitionSelection>)

    @Upsert
    suspend fun upsertSetupLeagues(leagues: List<UserLeagueSelection>)

    @Upsert
    suspend fun upsertLeagues(leagues: List<Leagues>)

    @Upsert
    fun upsertLeagueTable(leagueTable: List<LeagueTable>)

    @Query("SELECT * FROM leagues WHERE UPPER(state) = 'ACTIVE' AND selected = TRUE ORDER BY id ASC")
    suspend fun getLeagues(): List<Leagues>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeague(league: League)

    @Upsert
    suspend fun upsertLeagueInDb(league: League)

    @Upsert
    suspend fun upsertLeaguesInDb(leagues: Leagues)

    @Query("UPDATE leagues SET state = :state, selected = :isSelected WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun softDeleteLeague(leagueid: String, owneruserid: UUID, state: String = "Deleted", isSelected: Boolean = false)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeagues(leagues: Leagues)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitations(invitations: List<LeagueInvitation>)

    @Query("DELETE FROM league_invitations WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun deleteInvitations(leagueid: String, owneruserid: UUID)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetup(setup: Setup)

    @Query("SELECT * FROM League_Header WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun getLeagueHeader(leagueid: String, owneruserid: UUID): LeagueHeader

    @Query("DELETE FROM League_Header WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun deleteLeagueHeader(leagueid: String, owneruserid: UUID)

    @Query("DELETE FROM Leagues WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun deleteLeagues(leagueid: String, owneruserid: UUID)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeagueHeader(header: LeagueHeader)

    @Query("SELECT COUNT(id) FROM matches WHERE competitionId = :competitionId")
    suspend fun getMatchCountForCompetition(competitionId: String): Int

    @Upsert
    suspend fun upsertMatches(matches: List<Match>)

    @Query("SELECT * FROM language ORDER BY languageName ASC")
    fun getLanguages(): Flow<List<LanguageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLanguages(languages: List<LanguageEntity>)

    @Upsert
    suspend fun upsertLanguages(languages: List<LanguageEntity>)

    @Query("SELECT * FROM setup LIMIT 1")
    fun getSetup(): Flow<Setup?>

    @Upsert
    suspend fun upsertSetup(setup: Setup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchesIgnore(matches: List<Match>)

    @Query("""
    UPDATE matches 
    SET score1 = :score1, 
        score2 = :score2, 
        winner = :winner, 
        supplement = :supplement 
    WHERE id = :matchid
""")
    suspend fun updateMatchResults(
        matchid: Int,
        score1: Int?,
        score2: Int?,
        winner: Int?,
        supplement: String?
    )

    @Query("SELECT competitionid FROM league_header WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun getCompetitionIdForLeague(leagueid: String, owneruserid: UUID): String

    @Upsert
    suspend fun updateUserCompetition(competition: UserCompetitionSelection)

    @Upsert
    suspend fun updateAllNotifications(notifications: List<AppNotification>)

    @Query("UPDATE appnotification SET isread = 1 WHERE notificationid = :id")
    suspend fun markNotificationAsReadLocal(id: Int)

    @Query("SELECT * FROM appnotification WHERE notificationid = :id")
    suspend fun getAppNotification(id: Int): AppNotification?

    @Query("DELETE FROM setup")
    suspend fun deleteSetup()

    @Query("SELECT * FROM User_League_Users WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun getLeagueUserStatuses(leagueid: String, owneruserid: String): List<UserLeagueUsers>

    @Upsert
    suspend fun upsertUserLeagueUsersInDb(userLeagueUsers: UserLeagueUsers)

    @Transaction
    suspend fun deleteLeague(leagueid: String, owneruserid: UUID) {
        // Delete from all tables associated with this specific league name/owner combo
        deleteFromLeagues(leagueid, owneruserid)
        deleteFromLeague(leagueid, owneruserid)
        deleteFromLeagueHeader(leagueid, owneruserid)
        deleteFromLeagueInvitation(leagueid, owneruserid)
        deleteFromLeagueTable(leagueid, owneruserid)
        deleteFromUserLeagueSelection(leagueid, owneruserid)
        deleteFromUserLeagueUsers(leagueid, owneruserid)
    }

    @Query("DELETE FROM leagues WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun deleteFromLeagues(leagueid: String, owneruserid: UUID)

    @Query("DELETE FROM league WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun deleteFromLeague(leagueid: String, owneruserid: UUID)

    @Query("DELETE FROM league_header WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun deleteFromLeagueHeader(leagueid: String, owneruserid: UUID)

    @Query("DELETE FROM league_invitations WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun deleteFromLeagueInvitation(leagueid: String, owneruserid: UUID)

    @Query("DELETE FROM league_table WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun deleteFromLeagueTable(leagueid: String, owneruserid: UUID)

    @Query("DELETE FROM UserLeagueSelection WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun deleteFromUserLeagueSelection(leagueid: String, owneruserid: UUID)

    @Query("DELETE FROM user_league_users WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun deleteFromUserLeagueUsers(leagueid: String, owneruserid: UUID)

    @Query("UPDATE userleagueselection SET selected = :selected WHERE leagueid = :leagueid AND owneruserid = :owneruserid")
    suspend fun updateUserLeagueSelection(leagueid: String, owneruserid: UUID, selected: Boolean)

    @Query("SELECT * FROM league_table WHERE leagueid = :leagueid AND owneruserid = :owneruserid ORDER BY points DESC")
    fun getLeagueTableList(leagueid: String, owneruserid: String): List<LeagueTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeagueTable(table: List<LeagueTable>)

    @Transaction
    suspend fun updateLeagueTableCache(leagueid: String, owneruserid: UUID, table: List<LeagueTable>) {
        deleteFromLeagueTable(leagueid, owneruserid)
        insertLeagueTable(table)
    }

    @Update
    suspend fun updateAllCompetitions(selections: List<UserCompetitionSelection>)
}