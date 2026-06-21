package cloud.scoreprof.app

interface Platform {
    val name: String
    val version: Int
    val deviceModel: String
    val language: String
    fun shareText(text: String)
    fun setLanguage(languageCode: String)
}

expect fun getPlatform(): Platform
