package cloud.scoreprof.app.util

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun getFlagEmoji(countryCode: String?): String {
    if (countryCode == null || countryCode.length != 2) return ""
    val code = countryCode.uppercase()
    val firstChar = code[0].code - 65 + 127462
    val secondChar = code[1].code - 65 + 127462
    
    // In iOS/KMP we can use UTF-32 code points via NSString
    return "${unicodeToString(firstChar)}${unicodeToString(secondChar)}"
}

private fun unicodeToString(code: Int): String {
    return NSString.stringWithFormat("%C%C", (0xd83c).toShort(), (code - 0x1f1a5).toShort())
}
