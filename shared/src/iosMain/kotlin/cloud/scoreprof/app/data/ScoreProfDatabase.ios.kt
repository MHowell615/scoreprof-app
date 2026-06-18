package cloud.scoreprof.app.data

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

actual fun getDatabaseBuilder(): RoomDatabase.Builder<ScoreProfDatabase> {
    val dbFile = NSHomeDirectory() + "/scoreprof.db"
    return Room.databaseBuilder<ScoreProfDatabase>(
        name = dbFile,
        factory = { ScoreProfDatabaseConstructor.initialize() }
    )
}
