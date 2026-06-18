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

actual val platformModule: Module = module {
    single<BillingManager> { BillingManagerImpl(androidContext()) }
    single<Platform> {
        val context = androidContext()
        setAppContext(context)
        
        object : Platform {
            override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
            override val version: Int = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode
                }
            } catch (e: Exception) { 0 }
            override val deviceModel: String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            override val language: String = Locale.getDefault().language
            
            override fun shareText(text: String) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(intent, null).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }
}
