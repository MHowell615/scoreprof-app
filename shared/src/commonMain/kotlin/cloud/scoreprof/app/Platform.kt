package cloud.scoreprof.app

interface Platform {
    val name: String
    val version: Int
    val deviceModel: String
    val language: String
    fun shareText(text: String)
    fun setLanguage(languageCode: String)
    fun showSystemNotification(title: String, message: String)
}

expect fun getPlatform(): Platform

/**
 * Returns a locale-aware string comparator for the current platform.
 */
expect fun getStringComparator(): Comparator<String>
