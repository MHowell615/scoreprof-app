package cloud.scoreprof.app.di

import cloud.scoreprof.app.Platform
import cloud.scoreprof.app.data.BillingManager
import cloud.scoreprof.app.data.BillingManagerImpl
import cloud.scoreprof.app.data.setAppContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import cloud.scoreprof.app.AndroidPlatform
import cloud.scoreprof.app.worker.NotificationWorker
import org.koin.androidx.workmanager.dsl.workerOf

actual val platformModule: Module = module {
    single<BillingManager> { BillingManagerImpl(androidContext()) }
    single<Platform> {
        val context = androidContext()
        setAppContext(context)
        AndroidPlatform()
    }
    workerOf(::NotificationWorker)
}
