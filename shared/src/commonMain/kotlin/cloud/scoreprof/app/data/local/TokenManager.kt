package cloud.scoreprof.app.data.local

import com.russhwolf.settings.Settings

class TokenManager(private val settings: Settings = Settings()) {

    fun saveToken(token: String) {
        settings.putString("auth_token", token)
    }

    fun getToken(): String? {
        return settings.getStringOrNull("auth_token")
    }

    fun deleteToken() {
        settings.remove("auth_token")
    }

    fun clear() {
        settings.remove("auth_token")
        settings.remove("userid")
        settings.remove("email")
    }

    fun hasToken(): Boolean = getToken() != null

    fun saveUserId(userid: String) {
        settings.putString("userid", userid)
    }

    fun getUserId(): String? {
        return settings.getStringOrNull("userid")
    }

    fun saveEmail(email: String) {
        settings.putString("email", email)
    }

    fun getEmail(): String? {
        return settings.getStringOrNull("email")
    }

    fun getLastNotifiedId(): Int {
        return settings.getInt("last_notified_id", -1)
    }

    fun saveLastNotifiedId(id: Int) {
        settings.putInt("last_notified_id", id)
    }

    fun checkCacheVersion(currentVersion: Int, onClear: () -> Unit) {
        val lastVersion = settings.getInt("last_app_version", -1)
        // If coming from a version before the package rename (e.g., version 18 or lower)
        if (lastVersion != -1 && lastVersion < currentVersion && lastVersion <= 18) {
            clear()
            onClear()
        }
        settings.putInt("last_app_version", currentVersion)
    }
}
