package cloud.scoreprof.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cloud.scoreprof.app.Platform
import cloud.scoreprof.app.data.NotificationRepository
import cloud.scoreprof.app.data.ScoreProfDao
import cloud.scoreprof.app.data.SetupRepository
import cloud.scoreprof.app.data.local.TokenManager
import kotlinx.coroutines.flow.firstOrNull

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val setupRepository: SetupRepository,
    private val tokenManager: TokenManager,
    private val platform: Platform,
    private val dao: ScoreProfDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        println("NotificationWorker: Starting background work...")
        val userId = tokenManager.getUserId()
        val email = tokenManager.getEmail()
        val token = tokenManager.getToken()

        if (userId == null || email == null || token == null || userId == "guest") {
            return Result.success()
        }

        try {
            // 1. ALWAYS fetch from server to update the "In-App" local database
            val fetched = notificationRepository.fetchNotifications(token, email)

            // 2. Check privacy setting before showing a "Phone/System" notification
            val setup = setupRepository.getSetup(userId).firstOrNull()
            if (setup?.receive_notifications == true) {
                val lastNotifiedId = tokenManager.getLastNotifiedId()
                val newUnread = fetched.filter { !it.isread && it.notificationid > lastNotifiedId }

                if (newUnread.isNotEmpty()) {
                    newUnread.forEach { notification ->
                        platform.showSystemNotification(
                            notification.title ?: "ScoreProf",
                            notification.message ?: "You have a new message."
                        )
                    }
                    val maxId = newUnread.maxOf { it.notificationid }
                    tokenManager.saveLastNotifiedId(maxId)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            println("NotificationWorker failed: ${e.message}")
            return Result.retry()
        }
    }
}
