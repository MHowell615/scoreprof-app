package cloud.scoreprof.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cloud.scoreprof.app.domain.model.AppNotification
import cloud.scoreprof.app.domain.model.Competition
import cloud.scoreprof.app.domain.model.LanguageEntity
import cloud.scoreprof.app.domain.model.League
import cloud.scoreprof.app.domain.model.LeagueHeader
import cloud.scoreprof.app.domain.model.LeagueInvitation
import cloud.scoreprof.app.domain.model.LeagueTable
import cloud.scoreprof.app.domain.model.Leagues
import cloud.scoreprof.app.domain.model.Match
import cloud.scoreprof.app.domain.model.QuestionAnswer
import cloud.scoreprof.app.domain.model.Setup
import cloud.scoreprof.app.domain.model.UserCompetitionSelection
import cloud.scoreprof.app.domain.model.UserLeagueSelection
import cloud.scoreprof.app.domain.model.UserLeagueUsers
import cloud.scoreprof.app.domain.model.UserPredictions

@Database(
    entities = [
        AppNotification::class,
        Match::class,
        Competition::class,
        LanguageEntity::class,
        League::class,
        LeagueHeader::class,
        LeagueInvitation::class,
        Leagues::class,
        LeagueTable::class,
        QuestionAnswer::class,
        Setup::class,
        UserCompetitionSelection::class,
        UserLeagueSelection::class,
        UserPredictions::class,
        UserLeagueUsers::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ScoreProfDatabase : RoomDatabase() {
    abstract fun scoreProfDao(): ScoreProfDao

    companion object {
        const val DATABASE_NAME = "scoreprof.db"

        @Volatile
        private var Instance: ScoreProfDatabase? = null

        fun getDatabase(context: Context): ScoreProfDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, ScoreProfDatabase::class.java, "scoreprof_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}