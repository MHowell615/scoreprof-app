package cloud.scoreprof.app

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val version: Int = UIDevice.currentDevice.systemVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0
    override val deviceModel: String = UIDevice.currentDevice.model
    override val language: String = "en" // Simplified for now
    override fun shareText(text: String) {
        // Implementation for sharing text on iOS
    }
}

actual fun getPlatform(): Platform = IOSPlatform()