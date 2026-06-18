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

    fun hasToken(): Boolean = getToken() != null

    fun saveUserId(userid: String) {
        settings.putString("userid", userid)
    }

    fun getUserId(): String? {
        return settings.getStringOrNull("userid")
    }
}
