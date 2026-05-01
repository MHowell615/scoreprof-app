package cloud.scoreprof.app.ui.navigation

import android.os.Bundle
import androidx.navigation.NavType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cloud.scoreprof.app.domain.model.Competition

class CompetitionListNavType : NavType<List<Competition>>(isNullableAllowed = false) {
    private val gson = Gson()

    override fun get(bundle: Bundle, key: String): List<Competition>? {
        return bundle.getParcelableArrayList(key) // Use Parcelable for simplicity
    }

    override fun parseValue(value: String): List<Competition> {
        // This is not used when passing Parcelables directly
        val type = object : TypeToken<List<Competition>>() {}.type
        return gson.fromJson(value, type)
    }

    override fun put(bundle: Bundle, key: String, value: List<Competition>) {
        bundle.putParcelableArrayList(key, ArrayList(value))
    }
}
