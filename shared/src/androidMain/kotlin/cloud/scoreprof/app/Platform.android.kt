package cloud.scoreprof.app

import android.os.Build
import android.content.Intent
import cloud.scoreprof.app.data.getAppContext

import java.util.Locale
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class AndroidPlatform : Platform {
    private val context = getAppContext()
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val version: Int = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode
        }
    } catch (e: Exception) { 14 }
    override val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    override val language: String get() = Locale.getDefault().language
    
    override fun setLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    override fun shareText(text: String) {
        val context = getAppContext()
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

actual fun getPlatform(): Platform = AndroidPlatform()