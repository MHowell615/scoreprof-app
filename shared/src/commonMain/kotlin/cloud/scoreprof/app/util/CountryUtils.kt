package cloud.scoreprof.app.util

/**
 * Converts a 2-letter Country Code (ISO 3166-1 alpha-2) to a Flag Emoji.
 * e.g., "FR" -> 🇫🇷, "NG" -> 🇳🇬
 */
expect fun getFlagEmoji(countryCode: String?): String
