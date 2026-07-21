package cloud.scoreprof.app.data

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(): RoomDatabase.Builder<ScoreProfDatabase> {
    val fileManager = NSFileManager.defaultManager
    val documentDirectory = fileManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true, // Ensure the directory is created if missing
        error = null
    )
    
    val path = documentDirectory?.path ?: throw IllegalStateException("Failed to locate Documents directory on iOS")
    val dbFile = "$path/scoreprof.db"

    return Room.databaseBuilder<ScoreProfDatabase>(
        name = dbFile
    ).fallbackToDestructiveMigration(true)
     .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // Use TRUNCATE mode for better stability on iOS
}
