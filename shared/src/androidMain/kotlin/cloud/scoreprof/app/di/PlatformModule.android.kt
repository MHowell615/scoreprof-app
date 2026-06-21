package cloud.scoreprof.app.di

import cloud.scoreprof.app.Platform
import cloud.scoreprof.app.data.BillingManager
import cloud.scoreprof.app.data.BillingManagerImpl
import cloud.scoreprof.app.data.setAppContext
import android.content.Intent
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.Locale

import cloud.scoreprof.app.AndroidPlatform

actual val platformModule: Module = module {
    single<BillingManager> { BillingManagerImpl(androidContext()) }
    single<Platform> {
        val context = androidContext()
        setAppContext(context)
        AndroidPlatform()
    }
}
