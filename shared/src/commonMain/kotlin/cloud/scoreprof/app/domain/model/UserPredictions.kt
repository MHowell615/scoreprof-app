package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import cloud.scoreprof.app.ui.utils.InstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_predictions")
data class UserPredictions(
    @PrimaryKey val id: Int,
    val userid: String,
    val matchid: Int,
    val competitor1selected: Boolean,
    val competitor2selected: Boolean,
    @Serializable(with = InstantSerializer::class)
    val lastmodified: Instant
)
