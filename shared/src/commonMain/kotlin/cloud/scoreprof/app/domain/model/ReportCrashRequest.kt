package cloud.scoreprof.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReportCrashRequest(
    val userid: String,
    val error_message: String,
    val stack_trace: String,
    val app_version: String,
    val device_model: String
)
