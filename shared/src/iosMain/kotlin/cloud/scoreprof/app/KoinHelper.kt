package cloud.scoreprof.app

import cloud.scoreprof.app.data.FirebaseManager
import cloud.scoreprof.app.di.*
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

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

fun getFirebaseManager(): FirebaseManager {
    return object : KoinComponent {
        val manager: FirebaseManager = get()
    }.manager
}
