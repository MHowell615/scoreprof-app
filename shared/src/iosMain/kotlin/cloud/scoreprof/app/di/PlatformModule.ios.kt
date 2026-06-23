package cloud.scoreprof.app.di

import cloud.scoreprof.app.Platform
import cloud.scoreprof.app.IOSPlatform
import cloud.scoreprof.app.data.BillingManager
import cloud.scoreprof.app.data.IOSBillingManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<BillingManager> { IOSBillingManager() }
    single<Platform> { IOSPlatform() }
}
