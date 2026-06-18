package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "QuestionAnswer")
data class QuestionAnswer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val question: String,
    val answer: String
)