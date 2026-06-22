package cloud.scoreprof.app

import platform.UIKit.UIDevice
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.NSString
import platform.Foundation.NSDiacriticInsensitiveSearch
import platform.Foundation.NSCaseInsensitiveSearch

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName + " " + UIDevice.currentDevice.systemVersion
    override val version: Int = 14 // Simplified
    override val deviceModel: String = UIDevice.currentDevice.model
    override val language: String get() = NSLocale.currentLocale.languageCode ?: "en"
    
    override fun setLanguage(languageCode: String) {
        // iOS per-app language usually requires app restart or complex bundle swizzling.
        // For now, we rely on system settings.
    }

    override fun shareText(text: String) {
        // Implementation for sharing text on iOS
    }
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getStringComparator(): Comparator<String> {
    return object : Comparator<String> {
        override fun compare(a: String, b: String): Int {
            val nsA = a as NSString
            val options = NSDiacriticInsensitiveSearch or NSCaseInsensitiveSearch
            return nsA.compare(b, options).toInt()
        }
    }
}
