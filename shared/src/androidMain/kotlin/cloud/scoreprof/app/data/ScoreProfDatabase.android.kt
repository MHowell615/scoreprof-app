package cloud.scoreprof.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

actual fun getDatabaseBuilder(): RoomDatabase.Builder<ScoreProfDatabase> {
    val appContext = getAppContext()
    val dbFile = appContext.getDatabasePath("scoreprof.db")
    return Room.databaseBuilder<ScoreProfDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

// We need a way to get the context in androidMain
private var androidContext: Context? = null
fun setAppContext(context: Context) {
    androidContext = context
}
fun getAppContext(): Context = androidContext ?: throw Exception("Context not set")
