package cloud.scoreprof.app.data

import androidx.room.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import cloud.scoreprof.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

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
    version = 15,
    exportSchema = true
)
@TypeConverters(Converters::class)
@ConstructedBy(ScoreProfDatabaseConstructor::class)
abstract class ScoreProfDatabase : RoomDatabase() {
    abstract fun scoreProfDao(): ScoreProfDao
}

// The constructor needed for Room KMP
@Suppress("KotlinNoActualForExpect")
expect object ScoreProfDatabaseConstructor : RoomDatabaseConstructor<ScoreProfDatabase> {
    override fun initialize(): ScoreProfDatabase
}

// Room KMP uses expect/actual for the builder
expect fun getDatabaseBuilder(): RoomDatabase.Builder<ScoreProfDatabase>

fun getDatabase(builder: RoomDatabase.Builder<ScoreProfDatabase>): ScoreProfDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
}
