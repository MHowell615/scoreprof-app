package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "language")
data class LanguageEntity(
    @PrimaryKey
    val languageCode: String,
    val languageName: String
)