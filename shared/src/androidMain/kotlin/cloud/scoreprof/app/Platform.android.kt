package cloud.scoreprof.app

import android.os.Build
import android.content.Intent
import cloud.scoreprof.app.data.getAppContext

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val version: Int = 14 // Will be set in module
    override val deviceModel: String = ""
    override val language: String = ""
    
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