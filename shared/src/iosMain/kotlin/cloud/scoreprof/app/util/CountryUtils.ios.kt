package cloud.scoreprof.app.util

actual fun getFlagEmoji(countryCode: String?): String {
    if (countryCode == null || countryCode.length != 2) return ""
    val code = countryCode.uppercase()
    
    // High surrogate for all regional indicators is always 0xD83C
    val high = 0xD83C.toChar()
    
    // Low surrogate starts at 0xDDE6 for 'A' (127462)
    val low1 = (0xDDE6 + (code[0].code - 65)).toChar()
    val low2 = (0xDDE6 + (code[1].code - 65)).toChar()
    
    return "${high}${low1}${high}${low2}"
}
