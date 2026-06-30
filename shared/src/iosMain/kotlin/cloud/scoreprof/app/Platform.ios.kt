package cloud.scoreprof.app

import platform.UIKit.UIDevice
import platform.UIKit.UIApplication
import platform.UIKit.UIActivityViewController
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.NSString
import platform.Foundation.NSDiacriticInsensitiveSearch
import platform.Foundation.NSCaseInsensitiveSearch
import platform.Foundation.NSBundle
import platform.Foundation.compare

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName + " " + UIDevice.currentDevice.systemVersion
    override val version: Int = (NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String)?.toIntOrNull() ?: 0
    override val deviceModel: String = UIDevice.currentDevice.model
    override val language: String get() = NSLocale.currentLocale.languageCode
    
    override fun setLanguage(languageCode: String) {
        // iOS per-app language usually requires app restart or complex bundle swizzling.
        // For now, we rely on system settings.
    }

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

    override fun showSystemNotification(title: String, message: String) {
        // To be implemented using UserNotifications framework on a real Mac
    }
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getStringComparator(): Comparator<String> = Comparator { a, b ->
    val nsA = a as Any as NSString
    val options = NSDiacriticInsensitiveSearch or NSCaseInsensitiveSearch
    nsA.compare(b, options).toInt()
}
