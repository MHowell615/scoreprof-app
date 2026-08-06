package cloud.scoreprof.app.util

actual fun getFlagEmoji(countryCode: String?): String {
    if (countryCode == null || countryCode.length != 2) return ""
    val code = countryCode.uppercase()
    val firstChar = Character.toChars(code[0].code - 65 + 127462)
    val secondChar = Character.toChars(code[1].code - 65 + 127462)
    return String(firstChar) + String(secondChar)
}
