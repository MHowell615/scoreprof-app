package cloud.scoreprof.app.domain.usecase

import cloud.scoreprof.app.data.NotificationRepository
import cloud.scoreprof.app.domain.model.SendNotification

class SendNotificationsUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(notification: SendNotification) {
        repository.sendNotification(notification)
    }
}