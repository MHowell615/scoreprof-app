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
}
