package cloud.scoreprof.app

import cloud.scoreprof.app.di.*
import org.koin.core.context.startKoin

fun doInitKoin() {
    startKoin {
        modules(
            appModule,
            networkModule,
            databaseModule,
            platformModule,
            settingsModule
        )
    }
}
