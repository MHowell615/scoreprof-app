package cloud.scoreprof.app

import android.app.Application
import cloud.scoreprof.app.di.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ScoreProfApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@ScoreProfApplication)
            modules(
                appModule,
                networkModule,
                databaseModule,
                platformModule,
                settingsModule
            )
        }
    }
}
