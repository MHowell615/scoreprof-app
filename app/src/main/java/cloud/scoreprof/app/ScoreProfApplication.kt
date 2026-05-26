package cloud.scoreprof.app

import android.app.Application
import cloud.scoreprof.app.data.SetupRepository
import cloud.scoreprof.app.ui.utils.GlobalExceptionHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ScoreProfApplication : Application() {
    // We use @Inject to get the repository since this is a Hilt App
    @Inject
    lateinit var repository: SetupRepository

    override fun onCreate() {
        super.onCreate()

        // Setup the Global Exception Handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(applicationContext, repository, defaultHandler)
        )
    }
}