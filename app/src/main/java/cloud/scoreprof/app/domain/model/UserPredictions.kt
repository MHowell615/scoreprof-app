package cloud.scoreprof.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import cloud.scoreprof.app.ui.utils.UUIDSerializer
import cloud.scoreprof.app.ui.utils.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.util.UUID

@Serializable
@Entity(tableName = "user_predictions")
data class UserPredictions(
    @PrimaryKey val id: Int,
    @Serializable(with = UUIDSerializer::class)
    val userid: UUID,
    val matchid: Int,
    val competitor1selected: Boolean,
    val competitor2selected: Boolean,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val lastmodified: ZonedDateTime
)