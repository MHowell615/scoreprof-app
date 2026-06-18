package cloud.scoreprof.app.di

import cloud.scoreprof.app.Platform
import cloud.scoreprof.app.data.BillingManager
import cloud.scoreprof.app.data.IOSBillingManager
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.UIKit.*
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual val platformModule: Module = module {
    single<BillingManager> { IOSBillingManager() }
    single<Platform> {
        object : Platform {
            override val name: String = UIDevice.currentDevice.systemName + " " + UIDevice.currentDevice.systemVersion
            override val version: Int = (NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String)?.toIntOrNull() ?: 0
            override val deviceModel: String = UIDevice.currentDevice.model
            override val language: String = NSLocale.currentLocale.languageCode
            
            override fun shareText(text: String) {
                val window = UIApplication.sharedApplication.keyWindow
                val rootViewController = window?.rootViewController
                val activityViewController = UIActivityViewController(
                    activityItems = listOf(text),
                    applicationActivities = null
                )
                rootViewController?.presentViewController(
                    viewControllerToPresent = activityViewController,
                    animated = true,
                    completion = null
                )
            }
        }
    }
}
